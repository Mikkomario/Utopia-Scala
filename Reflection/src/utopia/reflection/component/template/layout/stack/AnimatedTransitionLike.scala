package utopia.reflection.component.template.layout.stack

import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.image.Image
import utopia.genesis.util.Fps
import utopia.paradigm.animation.{Animation, AnimationLike}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reflection.component.template.ReflectionComponentWrapper
import utopia.reflection.event.TransitionState
import utopia.reflection.event.TransitionState.{Finished, NotStarted, Ongoing}

import java.time.Instant
import scala.concurrent.Promise
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for animating component transitions in a stack layout environment. Remember to add this
  * component to an active ActorHandler and to call start() afterwards. Subclasses should also call
  * enableDrawing() at initialization.
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2
  */
// TODO: Should be an abstract class
trait AnimatedTransitionLike extends ReflectionStackable with ReflectionComponentWrapper
	with ReflectionStackLeaf with MutableCustomDrawableWrapper
{
	// ABSTRACT	------------------------------------
	
	/**
	  * @return Transition duration
	  */
	protected def duration: FiniteDuration
	
	/**
	  * @return Animation that is used for calculating the drawn images. The images are drawn in the order they
	  *         are listed, meaning that the last image will appear on top.
	  */
	protected def imageAnimation: AnimationLike[Seq[Image], Any]
	
	/**
	  * @return Animation that is used for calculating component size
	  */
	protected def sizeAnimation: Animation[StackSize]
	
	/**
	  * @return Maximum rate this component may be refreshed (refresh includes image and size calculations,
	  *         as well as stack hierarchy updates. Size alteration and stack hierarchy updates are also
	  *         limited by StackHierarchyManager)
	  */
	protected def maxRefreshRate: Fps
	
	
	// ATTRIBUTES	--------------------------------
	
	private var _state: TransitionState = NotStarted
	private var passedDuration = Duration.Zero
	private var nextUpdateThreshold = Duration.Zero
	private val completionPromise = Promise[Unit]()
	
	private val cachedImages = ResettableLazy { imageAnimation(progress) }
	private val cachedSize = ResettableLazy { sizeAnimation(progress) }
	
	
	// COMPUTED	------------------------------------
	
	/**
	  * @return Current state of this transition
	  */
	def state = _state
	
	/**
	  * @return A future that resolves at the completion of this transition
	  */
	def completion = completionPromise.future
	
	/**
	  * @return Estimated completion time of this transition. None if this transition hasn't started yet.
	  */
	def estimatedCompletionTime = _state match
	{
		case Finished => Some(Instant.now())
		case Ongoing => Some(Instant.now() + duration - passedDuration)
		case NotStarted => None
	}
	
	private def progress = passedDuration / duration
	
	
	// IMPLEMENTED	--------------------------------
	
	override def children = super[ReflectionStackLeaf].children
	
	override def updateLayout() = ()
	
	override def stackSize = cachedSize.value
	
	override def resetCachedSize() = cachedSize.reset()
	
	override def stackId = hashCode()
	
	
	// OTHER	---------------------------------
	
	/**
	  * Enables custom drawing within this component. Subclasses should call this method when both component and
	  * animations have been initialized.
	  */
	protected def enableDrawing() = addCustomDrawer(Drawer)
	
	/**
	  * Starts this transition progress
	  * @return A future of transition completion
	  */
	def start(actorHandler: ActorHandler) =
	{
		if (_state == NotStarted) {
			// Starts transition & revalidation loop
			_state = Ongoing
			actorHandler += new Animator(actorHandler)
			revalidate()
		}
		completionPromise.future
	}
	
	
	// NESTED	---------------------------------
	
	private object Drawer extends CustomDrawer
	{
		override def drawLevel = Normal
		
		override def opaque = false
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			cachedImages.value
				.foreach { _.withSize(bounds.size, preserveShape = false).drawWith(drawer, bounds.position) }
		}
	}
	
	private class Animator(handler: ActorHandler) extends Actor
	{
		override def handleCondition: Flag = AlwaysTrue
		
		override def act(duration: FiniteDuration) = {
			// Advances the animation, may also finish transition and/or trigger component update
			passedDuration += duration
			if (passedDuration >= AnimatedTransitionLike.this.duration) {
				_state = Finished
				handler -= this
				passedDuration = AnimatedTransitionLike.this.duration
				completionPromise.success(())
				cachedImages.reset()
				revalidate()
				repaint()
			}
			else if (passedDuration > nextUpdateThreshold) {
				nextUpdateThreshold = passedDuration + maxRefreshRate.interval
				cachedImages.reset()
				revalidate()
				repaint()
			}
		}
	}
}

package utopia.reflection.component.template.layout.stack

import java.time.Instant

import utopia.flow.datastructure.mutable.Lazy
import utopia.flow.util.TimeExtensions._
import utopia.genesis.animation.Animation
import utopia.genesis.handling.Actor
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.{Drawer, Fps}
import utopia.inception.handling.{HandlerType, Mortal}
import utopia.reflection.component.drawing.mutable.{CustomDrawable, CustomDrawableWrapper}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.template.ComponentWrapper
import utopia.reflection.event.TransitionState
import utopia.reflection.event.TransitionState.{Finished, NotStarted, Ongoing}
import utopia.reflection.shape.StackSize

import scala.concurrent.Promise
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for animating component transitions in a stack layout environment. Remember to add this
  * component to an active ActorHandler and to call start() afterwards. Subclasses should also call
  * enableDrawing() at initialization.
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2
  */
trait AnimatedTransitionLike extends Stackable with ComponentWrapper with CustomDrawable
	with Actor with StackLeaf with CustomDrawableWrapper with Mortal
{
	// ABSTRACT	------------------------------------
	
	/**
	  * @return Transition duration
	  */
	protected def duration: FiniteDuration
	
	/**
	  * @return Animation that is used for calculating the drawn image
	  */
	protected def imageAnimation: Animation[Image]
	
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
	
	private val cachedImage = Lazy { imageAnimation(progress) }
	private val cachedSize = Lazy { sizeAnimation(progress) }
	
	
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
	
	override def children = super[StackLeaf].children
	
	// Dies when animation is completed
	override def isDead = _state == Finished
	
	override def updateLayout() = ()
	
	override def stackSize = cachedSize.get
	
	override def resetCachedSize() = cachedSize.reset()
	
	override def stackId = hashCode()
	
	override def act(duration: FiniteDuration) =
	{
		// Advances the animation, may also finish transition and/or trigger component update
		passedDuration += duration
		if (passedDuration >= this.duration)
		{
			_state = Finished
			passedDuration = this.duration
			completionPromise.success(())
			cachedImage.reset()
			revalidate()
			repaint()
		}
		else if (passedDuration > nextUpdateThreshold)
		{
			nextUpdateThreshold = passedDuration + maxRefreshRate.interval
			cachedImage.reset()
			revalidate()
			repaint()
		}
	}
	
	// Only allows handling if in progress of change
	override def allowsHandlingFrom(handlerType: HandlerType) = _state == Ongoing
	
	
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
	def start() =
	{
		if (_state == NotStarted)
		{
			// Starts transition & revalidation loop
			_state = Ongoing
			revalidate()
		}
		completionPromise.future
	}
	
	
	// NESTED	---------------------------------
	
	private object Drawer extends CustomDrawer
	{
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			cachedImage.get.withSize(bounds.size, preserveShape = false).drawWith(drawer, bounds.position)
		}
	}
}

package utopia.reflection.component.swing

import utopia.genesis.handling.Actor
import utopia.genesis.shape.Direction1D.{Negative, Positive}
import utopia.genesis.shape.{Axis2D, Direction1D}
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.inception.handling.{HandlerType, Mortal}
import utopia.reflection.component.drawing.mutable.{CustomDrawable, CustomDrawableWrapper}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.stack.{StackLeaf, Stackable}
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.flow.util.TimeExtensions._
import utopia.genesis.image.Image
import utopia.genesis.shape.path.ProjectilePath
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.shape.StackSize
import utopia.reflection.util.ComponentToImage

import scala.concurrent.Promise
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for animating component appearances and disappearances in a stack layout environment. Remember to add this
  * component to an active ActorHandler and to call start() afterwards
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2.1
  * @param original The original component being drawn (uses the initial state (except size) of the original component and doesn't
  *                 update visuals during transition)
  * @param transitionAxis The axis along which the appearance or disappearance happens
  * @param transitionDirection Whether transition should be appearance (positive) or disappearance (negative)
  *                            (default = positive)
  * @param duration The duration of this transition (default = 0.25 seconds)
  * @param useFading Whether fading (alpha change) should be used during the transition (default = true)
  * @param finalSize Estimated size of the component when it is finally presented (optional). If None, optimal stack
  *                  size of the component is used instead. Defaults to None.
  */
class AnimatedTransition(original: AwtComponentRelated with Stackable, transitionAxis: Axis2D,
						 transitionDirection: Direction1D = Positive, duration: FiniteDuration = 0.25.seconds,
						 useFading: Boolean = true, finalSize: Option[Size] = None)
	extends AwtComponentWrapperWrapper with Stackable with CustomDrawable with Actor with StackLeaf
		with CustomDrawableWrapper with Mortal
{
	// ATTRIBUTES	--------------------------------
	
	private val curve = ProjectilePath()
	private val targetSize = finalSize.getOrElse(original.stackSize.optimal)
	private val baseImage = ComponentToImage(original, targetSize)
	
	private var state: TransitionState = NotStarted
	private var passedDuration = Duration.Zero
	private val completionPromise = Promise[Unit]()
	
	private val label = new EmptyLabel()
	
	
	// INITIAL CODE	--------------------------------
	
	label.addCustomDrawer(Drawer)
	
	
	// COMPUTED	------------------------------------
	
	private def progress = passedDuration / duration
	
	private def visibility = transitionDirection match
	{
		case Positive =>
			state match
			{
				case NotStarted => Hidden
				case Ongoing => Appearing
				case Finished => FullyVisible
			}
		case Negative =>
			state match
			{
				case NotStarted => FullyVisible
				case Ongoing => Disappearing
				case Finished => Hidden
			}
	}
	
	private def currentTargetSize = visibility match
	{
		case Hidden => Size.zero
		case Appearing => targetSize.mapAxis(transitionAxis) { _ * curve(progress) }
		case Disappearing => targetSize.mapAxis(transitionAxis) { _ * (1 - curve(progress)) }
		case FullyVisible => targetSize
	}
	
	private def currentAlpha =
	{
		if (useFading)
		{
			visibility match
			{
				case Hidden => 0.0
				case Appearing => curve(progress)
				case Disappearing => 1 - curve(progress)
				case FullyVisible => 1.0
			}
		}
		else
			1.0
	}
	
	
	// IMPLEMENTED	--------------------------------
	
	// Dies when animation is completed
	override def isDead = state == Finished
	
	override def drawable = label
	
	override def updateLayout() = ()
	
	override def stackSize = StackSize.any(currentTargetSize)
	
	override def resetCachedSize() = ()
	
	override def stackId = hashCode()
	
	override protected def wrapped = label
	
	override def act(duration: FiniteDuration) =
	{
		// Advances the animation, may also finish transition
		passedDuration += duration
		if (passedDuration >= this.duration)
		{
			state = Finished
			passedDuration = this.duration
			completionPromise.success(())
		}
		revalidate()
	}
	
	// Only allows handling if in progress of change
	override def allowsHandlingFrom(handlerType: HandlerType) = state == Ongoing
	
	
	// OTHER	---------------------------------
	
	/**
	  * Starts this transition progress
	  * @return A future of transition completion
	  */
	def start() =
	{
		if (state == NotStarted)
		{
			println(s"Target size is $targetSize")
			
			// Starts transition & revalidation loop
			state = Ongoing
			revalidate()
		}
		completionPromise.future
	}
	
	
	// NESTED	---------------------------------
	
	private object Drawer extends CustomDrawer
	{
		private def image = state match
		{
			case NotStarted => Image.empty
			case Ongoing => baseImage.withAlpha(currentAlpha)
			case Finished => baseImage
		}
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			image.withSize(bounds.size, preserveShape = false).drawWith(drawer, bounds.position)
		}
	}
	
	private sealed trait TransitionState
	
	private case object NotStarted extends TransitionState
	
	private case object Ongoing extends TransitionState
	
	private case object Finished extends TransitionState
	
	private sealed trait VisibilityState
	
	private case object Hidden extends VisibilityState
	
	private case object Appearing extends VisibilityState
	
	private case object Disappearing extends VisibilityState
	
	private case object FullyVisible extends VisibilityState
}

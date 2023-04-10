package utopia.reflection.component.swing.animation

import utopia.genesis.util.Fps
import utopia.paradigm.animation.Animation
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.path.ProjectilePath
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.context.AnimationContextLike
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.{AwtComponentRelated, AwtComponentWrapperWrapper}
import utopia.reflection.component.template.layout.stack.{AnimatedTransitionLike, ReflectionStackable}
import utopia.reflection.event.TransitionState.{Finished, NotStarted, Ongoing}
import utopia.reflection.event.Visibility.{Invisible, Visible}
import utopia.reflection.event.VisibilityChange
import utopia.reflection.event.VisibilityChange.{Appearing, Disappearing}
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.{ComponentCreationDefaults, ComponentToImage}

import scala.concurrent.duration.FiniteDuration

object AnimatedVisibilityChange
{
	/**
	  * Creates a new animated transition by utilizing component creation context
	  * @param original The original component being drawn (uses the initial state (except size) of the original component and doesn't
	  *                 update visuals during transition)
	  * @param transitionAxis      The axis along which the appearance or disappearance happens.
	  *                            None if the transition should be applied to both axes (default).
	  * @param transition Whether transition should be appearance or disappearance (default = appearance)
	  * @param finalSize Estimated size of the component when it is finally presented (optional). If None, optimal stack
	  *                  size of the component is used instead. Defaults to None.
	  * @param context Component creation context (implicit)
	  * @return A new animated transition
	  */
	def contextual(original: AwtComponentRelated with ReflectionStackable, transitionAxis: Option[Axis2D] = None,
	               transition: VisibilityChange = Appearing, finalSize: Option[Size] = None)
				  (implicit context: AnimationContextLike) =
	{
		new AnimatedVisibilityChange(original, transitionAxis, transition, context.animationDuration,
			finalSize, context.maxAnimationRefreshRate, context.useFadingInAnimations)
	}
}

/**
  * Used for animating component appearances and disappearances in a stack layout environment. Remember to add this
  * component to an active ActorHandler and to call start() afterwards
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2
  * @param original The original component being drawn (uses the initial state (except size) of the original component and doesn't
  *                 update visuals during transition)
  * @param transitionAxis The axis along which the appearance or disappearance happens. None if transition should be
  *                       applied on both axes (default).
  * @param transition Whether transition should be appearance or disappearance (default = appearance)
  * @param duration The duration of this transition (default = 0.25 seconds)
  * @param finalSize Estimated size of the component when it is finally presented (optional). If None, optimal stack
  *                  size of the component is used instead. Defaults to None.
  * @param maxRefreshRate Maximum rate at which this component may refresh its size and image
  *                       (default = 120 frames/times per second)
  * @param useFading Whether fading (alpha change) should be used during the transition (default = true)
  */
class AnimatedVisibilityChange(original: AwtComponentRelated with ReflectionStackable, transitionAxis: Option[Axis2D] = None,
							   transition: VisibilityChange = Appearing,
							   override val duration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
							   finalSize: Option[Size] = None,
							   override val maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate,
							   useFading: Boolean = true)
	extends AnimatedTransitionLike with AwtComponentWrapperWrapper
{
	// ATTRIBUTES	--------------------------------
	
	private val curve = ProjectilePath()
	// TODO: Adjust to component stack size changes? (and how?)
	private lazy val targetSize = finalSize.getOrElse(original.stackSize.optimal)
	private lazy val baseImage = ComponentToImage(original, targetSize)
	
	private val label = new EmptyLabel()
	
	// TODO: Update image periodically?
	override protected val imageAnimation =
	{
		if (useFading)
			Animation { progress =>
				val alpha = visibility match
				{
					case Invisible => 0.0
					case Appearing => progress
					case Disappearing => 1.0 - progress
					case Visible => 1.0
				}
				Vector(baseImage.withAlpha(alpha))
			}
		else
			Animation.fixed(Vector(baseImage))
	}
	
	override protected val sizeAnimation = Animation { progress =>
		val curvedProgress = curve(progress)
		val scaling = transition match
		{
			case Appearing => curvedProgress
			case Disappearing => 1 - curvedProgress
		}
		StackSize.any(transitionAxis match
		{
			case Some(axis) => targetSize.mapDimension(axis) { _ * scaling }
			case None => targetSize * scaling
		})
	}
	
	
	// INITIAL CODE	--------------------------------
	
	enableDrawing()
	
	
	// COMPUTED	------------------------------------
	
	def visibility = state match
	{
		case NotStarted => Invisible
		case Ongoing => transition
		case Finished => Visible
	}
	
	
	// IMPLEMENTED	--------------------------------
	
	override def drawable = label
	
	override protected def wrapped = label
}

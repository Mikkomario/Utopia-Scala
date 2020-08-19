package utopia.reflection.component.swing.animation

import utopia.flow.async.Volatile
import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.genesis.util.Fps
import utopia.reflection.component.context.AnimationContextLike
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.wrapper.SwitchPanel
import utopia.reflection.event.{Visibility, VisibilityChange, VisibilityState}
import utopia.reflection.event.Visibility.{Invisible, Visible}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object AnimatedVisibility
{
	/**
	  * Creates a new animated visibility container with component creation context
	  * @param display Component displayed
	  * @param transitionAxis Axis along which the component is shrank. None if both axes should be affected (default)
	  * @param initialState Initial visibility state for this component's contents. Default = Invisible.
	  * @param context Component creation context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam C Type of displayed component
	  * @return A new animated visibility container
	  */
	def contextual[C <: AwtStackable](display: C, transitionAxis: Option[Axis2D] = None,
									  initialState: VisibilityState = Invisible)
	                                 (implicit context: AnimationContextLike, exc: ExecutionContext) =
		new AnimatedVisibility[C](display, context.actorHandler, transitionAxis, initialState, context.animationDuration,
			context.maxAnimationRefreshRate, context.useFadingInAnimations)
}

/**
  * This component wrapper animates component visibility changes (appearance & disappearance)
  * @author Mikko Hilpinen
  * @since 19.4.2020, v1.2
  */
class AnimatedVisibility[C <: AwtStackable](val display: C, actorHandler: ActorHandler,
											transitionAxis: Option[Axis2D] = None,
											initialState: VisibilityState = Invisible,
											duration: FiniteDuration = 0.25.seconds,
											maxRefreshRate: Fps = Fps(120),
											useFading: Boolean = true)
										   (implicit exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val panel = new SwitchPanel[AwtStackable]
	
	private var targetState = initialState match
	{
		case static: Visibility => static
		case transition: VisibilityChange => transition.targetState
	}
	// Currently active transition as a future.
	// If initial state was transitive, this is initialized as an active transition.
	private val lastTransition = Volatile(initialState match
	{
		case static: Visibility => Future.successful(static)
		case transition: VisibilityChange => startTransition(transition.targetState)
	})
	
	
	// INITIAL CODE	---------------------
	
	// If initially completely shown, sets panel content accordingly.
	if (initialState == Visible)
		panel.set(display)
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Current visibility state of this component
	  */
	def visibility = lastTransition.get.current.flatMap { _.toOption } match
	{
		case Some(staticState) => staticState
		case None => targetState.transitionIn
	}
	
	/**
	  * Changes whether the component should be shown or not. The actual visibility of the component is altered
	  * via a transition, so the visual effects may not be immediate
	  * @param newState New targeted visibility state
	  * @return A future of the transition completion, with the new shown state of this component
	  *         (component shown status may have been altered during the transition so that the returned state
	  *         doesn't always match 'newState')
	  */
	def visibility_=(newState: Visibility) =
	{
		if (newState != targetState)
		{
			// If this component is already transitioning, simply alters the target state.
			// Otherwise starts a new transition
			targetState = newState
			lastTransition.setIf { _.isCompleted } { startTransition(newState) }
			lastTransition.get
		}
		else
			Future.successful(newState)
	}
	
	/**
	  * @return Whether the wrapped component is currently being shown (won't count transitions)
	  */
	def isShown = targetState.isVisible
	/**
	  * Changes whether the component should be shown or not. The actual visibility of the component is altered
	  * via a transition, so the visual effects may not be immediate
	  * @param shouldBeShown Whether component should be shown
	  * @return A future of the transition completion, with the new shown state of this component
	  *         (component shown status may have been altered during the transition so that the returned state
	  *         doesn't always match 'newState')
	  */
	def isShown_=(shouldBeShown: Boolean) = visibility = if (shouldBeShown) Visible else Invisible
	
	
	// IMPLEMENTED	---------------------
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	
	// OTHER	-------------------------
	
	/**
	  * Specifies component visibility without triggering transition animations
	  * @param newState New visibility state
	  */
	def setStateWithoutTransition(newState: Visibility) =
	{
		if (targetState != newState)
		{
			targetState = newState
			if (newState.isVisible)
				panel.set(display)
			else
				panel.clear()
		}
	}
	
	// Returns the reached visibility target
	private def startTransition(target: Visibility): Future[Visibility] =
	{
		// Creates the transition
		val transition = new AnimatedVisibilityChange(display, transitionAxis, target.transitionIn, duration,
			None, maxRefreshRate, useFading)
		actorHandler += transition
		panel.set(transition)
		
		// Starts the transition in background
		transition.start().flatMap { _ =>
			// At transition end, may start a new transition if the target state was changed during
			// the first transition
			if (targetState == target)
			{
				// Case: Target state reached
				// Switches to original component or clears this panel
				if (target.isVisible)
					panel.set(display)
				else
					panel.clear()
				
				Future.successful(target)
			}
			else
			{
				// Case: Target state was switched
				// Starts a new transition
				startTransition(targetState)
			}
		}
	}
}

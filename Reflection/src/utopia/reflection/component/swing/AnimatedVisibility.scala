package utopia.reflection.component.swing

import utopia.flow.async.Volatile
import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.{Axis2D, Direction1D}
import utopia.reflection.container.swing.{AwtContainerRelated, SwitchPanel}
import utopia.reflection.container.swing.Stack.AwtStackable

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

/**
  * This component wrapper animates component visibility changes (appearance & disappearance)
  * @author Mikko Hilpinen
  * @since 19.4.2020, v1.2
  */
class AnimatedVisibility[C <: AwtStackable](comp: C, actorHandler: ActorHandler, transitionAxis: Axis2D,
											duration: FiniteDuration = 0.25.seconds,
											useFading: Boolean = true, isShownInitially: Boolean = false)
										   (implicit exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val panel = new SwitchPanel[AwtStackable]
	
	private var targetState = isShownInitially
	private val lastTransition = Volatile(Future.successful[Unit](()))
	
	
	// INITIAL CODE	---------------------
	
	// If initially shown, sets panel content accordingly
	if (isShownInitially)
		panel.set(comp)
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Whether the wrapped component is currently being shown (won't count transitions)
	  */
	def isShown = targetState
	/**
	  * Changes whether the component should be shown or not. The actual visibility of the component is altered
	  * via a transition, so the visual effects may not be immediate
	  * @param newState Whether component should be shown
	  */
	def isShown_=(newState: Boolean) =
	{
		if (newState != targetState)
		{
			// If this component is already transitioning, simply alters the target state.
			// Otherwise starts a new transition
			targetState = newState
			lastTransition.setIf { _.isCompleted } { startTransition(newState) }
		}
	}
	
	
	// IMPLEMENTED	---------------------
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	
	// OTHER	-------------------------
	
	private def startTransition(target: Boolean): Future[Unit] =
	{
		// Creates the transition
		val transition = new AnimatedTransition(comp, transitionAxis, Direction1D.matching(target), duration, useFading)
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
				if (target)
					panel.set(comp)
				else
					panel.clear()
				
				Future.successful[Unit](())
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

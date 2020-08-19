package utopia.reflection.container.swing.layout.wrapper

import utopia.flow.async.Volatile
import utopia.flow.util.TimeExtensions._
import utopia.genesis.animation.transform.AnimatedTransform
import utopia.genesis.handling.Actor
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.inception.handling.Mortal
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.animation.AnimatedVisibilityChange
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.shape.StackSize

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}


/**
  * This component wrapper animates transitions between components
  * @author Mikko Hilpinen
  * @since 19.4.2020, v1.2
  */
class AnimatedSwitchPanel[C <: AwtStackable](initialContent: C, actorHandler: ActorHandler,
											 transition: AnimatedTransform[(Image, Image), Image],
											 duration: FiniteDuration = 0.25.seconds)
											(implicit exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val panel = new SwitchPanel[AwtStackable]
	
	private var currentContent = initialContent
	private val lastTransition = Volatile(Future.successful(currentContent))
	
	
	// INITIAL CODE	---------------------
	
	panel.set(currentContent)
	
	
	// COMPUTED	-------------------------
	
	/*
	  * Changes whether the component should be shown or not. The actual visibility of the component is altered
	  * via a transition, so the visual effects may not be immediate
	  * @param newState Whether component should be shown
	  * @return A future of the transition completion, with the new shown state of this component
	  *         (component shown status may have been altered during the transition so that the returned state
	  *         doesn't always match 'newState')
	  */
		/*
	def isShown_=(newState: Boolean) =
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
	}*/
	
	
	// IMPLEMENTED	---------------------
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	
	// OTHER	-------------------------
	
	/*
	  * Specifies component visibility without triggering transition animations
	  * @param newState New visibility state
	  */
	/*
	def setStateWithoutTransition(newState: Boolean) =
	{
		if (targetState != newState)
		{
			targetState = newState
			if (newState)
				panel.set(display)
			else
				panel.clear()
		}
	}*/
	
	// Returns the reached visibility target
	/*
	private def startTransition(target: Boolean): Future[Boolean] =
	{
		// Creates the transition
		val transition = new AnimatedTransition(display, transitionAxis, Direction1D.matching(target), duration, useFading)
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
	}*/
	
	
	// NESTED	----------------------------------
	
	private class Transition(from: C, to: C, direction: Direction1D) extends Actor with Handleable with Mortal
	{
		override def act(duration: FiniteDuration) = ???
		
		override def isDead = ???
		
		private class TransitionDrawer(original: Image, target: Image) extends CustomDrawer
		{
			override def drawLevel = Normal
			
			override def draw(drawer: Drawer, bounds: Bounds) =
			{
				
				???
			}
		}
	}
}

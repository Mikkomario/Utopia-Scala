package utopia.reflection.container.swing

import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.Direction1D.Negative
import utopia.reflection.component.swing.AnimatedTransition
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.StackLength

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * This version of stack uses animations when adding / removing components
  * @author Mikko Hilpinen
  * @since 18.4.2020, v1.1.1
  */
class AnimatedStack(actorHandler: ActorHandler, direction: Axis2D, margin: StackLength = StackLength.any,
					cap: StackLength = StackLength.fixed(0), layout: StackLayout = Fit,
					transitionDuration: FiniteDuration = 0.25.seconds)(implicit exc: ExecutionContext)
	extends Stack[AwtStackable](direction, margin, cap, layout)
{
	override def -=(component: AwtStackable) =
	{
		// Replaces the component with a temporary transition
		indexOf(component).foreach { index =>
			val disappearance = new AnimatedTransition(component, direction, Negative, duration = transitionDuration)
			actorHandler += disappearance
			super.-=(component)
			super.insert(disappearance, index)
			disappearance.start().foreach { _ => super.-=(disappearance) }
		}
	}
	
	override def insert(component: AwtStackable, index: Int) =
	{
		// First adds a transition and then switches it to the actual component
		val appearance = new AnimatedTransition(component, direction, duration = transitionDuration)
		actorHandler += appearance
		super.insert(appearance, index)
		appearance.start().foreach { _ => indexOf(appearance).foreach { super.insert(component, _) } }
	}
}

package utopia.reach.component.factory.contextual

import utopia.firmament.context.color.VariableColorContextLike
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.reach.component.factory.VariableBackgroundRoleAssignable

/**
  * Common trait for component factories that support variable background colors using contextual information
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
trait VariableBackgroundRoleAssignableFactory[N <: VariableColorContextLike[N, _], +Repr]
	extends VariableBackgroundRoleAssignable[Repr] with VariableContextualFactory[N, Repr]
{
	// ABSTRACT ------------------------------
	
	/**
	  * Creates a new copy of this factory that draws background
	  * @param newContext New context to use
	  * @param backgroundDrawer New background drawer to assign
	  * @return Copy of this factory with background drawing enabled
	  */
	protected def withVariableBackgroundContext(newContext: N, backgroundDrawer: CustomDrawer): Repr
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def withBackgroundPointer(pointer: Either[(Changing[ColorRole], ColorLevel), Changing[Color]]): Repr = {
		// Alters the context according to the variable background color
		val newContext = pointer match {
			case Left((rolePointer, preference)) => context.withBackgroundRolePointer(rolePointer, preference)
			case Right(colorPointer) => context.withBackgroundPointer(colorPointer)
		}
		// Assigns a custom drawer to actuate that color
		val drawer = newContext.backgroundPointer.fixedValue match {
			case Some(background) => BackgroundDrawer(background)
			case None => BackgroundViewDrawer(newContext.backgroundPointer)
		}
		withVariableBackgroundContext(newContext, drawer)
	}
}

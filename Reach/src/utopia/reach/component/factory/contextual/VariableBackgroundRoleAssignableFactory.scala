package utopia.reach.component.factory.contextual

import utopia.firmament.context.ColorContextLike
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.reach.component.factory.VariableBackgroundRoleAssignable

/**
  * Common trait for component factories that support variable background colors using contextual information
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
trait VariableBackgroundRoleAssignableFactory[N <: ColorContextLike[N, _], +Repr]
	extends VariableBackgroundRoleAssignable[Repr] with VariableContextualFactory[N, Repr]
{
	// ABSTRACT ------------------------------
	
	/**
	  * Creates a new copy of this factory that draws background
	  * @param newContextPointer New context pointer to use
	  * @param backgroundDrawer New background drawer to assign
	  * @return Copy of this factory with background drawing enabled
	  */
	protected def withVariableBackgroundContext(newContextPointer: Changing[N], backgroundDrawer: CustomDrawer): Repr
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def withBackgroundPointer(pointer: Either[(Changing[ColorRole], ColorLevel), Changing[Color]]): Repr = {
		// Alters the context according to the variable background color
		val newContext = pointer match {
			case Left((rolePointer, preference)) =>
				contextPointer.mergeWith(rolePointer) { _.withBackground(_, preference) }
			case Right(colorPointer) => contextPointer.mergeWith(colorPointer) { _ against _ }
		}
		// Assigns a custom drawer to actuate that color
		val drawer = newContext.fixedValue match {
			case Some(context) => BackgroundDrawer(context.background)
			case None => BackgroundViewDrawer(View { newContext.value.background })
		}
		withVariableBackgroundContext(newContext, drawer)
	}
}

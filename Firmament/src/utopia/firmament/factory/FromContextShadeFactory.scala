package utopia.firmament.factory

import utopia.firmament.context.color.{ColorContextPropsView, StaticColorContext}
import utopia.paradigm.color.FromVariableShadeFactory

/**
  * Common trait for factories that can construct shaded items.
  * Includes functions for context-aware shading.
  * @tparam A Type of constructed (shaded) items
  * @author Mikko Hilpinen
  * @since 07.11.2024, v1.4
  */
trait FromContextShadeFactory[+A] extends FromVariableShadeFactory[A]
{
	/**
	  * @param context Implicit component creation context
	  * @return A black or a white icon, whichever is better suited to the current context
	  */
	def contextual(implicit context: StaticColorContext) = against(context.background)
	/**
	  * @param context Implicit component-creation context
	  * @return A pointer which contains either a black or a white version of this icon,
	  *         whichever is better suited against the current (possibly changing) context background
	  */
	def variableContextual(implicit context: ColorContextPropsView) =
		againstVariableBackground(context.backgroundPointer)
}

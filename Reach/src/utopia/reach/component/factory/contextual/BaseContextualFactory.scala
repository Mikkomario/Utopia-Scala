package utopia.reach.component.factory.contextual

import utopia.firmament.context.base.{StaticBaseContext, StaticBaseContextWrapper, VariableBaseContext}
import utopia.paradigm.color.Color

/**
  * Common trait for (component) factories that wrap and use a BaseContext
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait BaseContextualFactory[+Repr]
	extends ContextualFactory[StaticBaseContext, Repr] with StaticBaseContextWrapper[StaticBaseContext, Repr]
{
	override def base = context
	
	override def current: StaticBaseContext = base
	override def toVariableContext: VariableBaseContext = base.toVariableContext
	
	override def withBase(baseContext: StaticBaseContext): Repr = withContext(baseContext)
	override def against(background: Color): Repr = mapContext { _.against(background) }
	override def *(mod: Double): Repr = mapContext { _ * mod }
}

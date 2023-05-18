package utopia.reach.component.factory.contextual

import utopia.firmament.context.{BaseContext, BaseContextWrapper}
import utopia.paradigm.color.Color

/**
  * Common trait for (component) factories that wrap and use a BaseContext
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait BaseContextualFactory[+Repr] extends ContextualFactory[BaseContext, Repr] with BaseContextWrapper[Repr, Repr]
{
	override def base: BaseContext = context
	
	override def withBase(baseContext: BaseContext): Repr = withContext(baseContext)
	override def against(background: Color): Repr = mapContext { _.against(background) }
	override def *(mod: Double): Repr = mapContext { _ * mod }
}

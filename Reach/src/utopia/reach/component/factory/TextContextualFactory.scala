package utopia.reach.component.factory

import utopia.firmament.context.{TextContext, TextContextWrapper}

/**
  * Common trait for factories that use a TextContext (only)
  * @author Mikko Hilpinen
  * @since 17.4.2023, v0.1
  */
trait TextContextualFactory[+Repr] extends ContextualFactory[TextContext, Repr] with TextContextWrapper[Repr]
{
	override def textContext: TextContext = context
	override def withTextContext(base: TextContext): Repr = withContext(base)
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}

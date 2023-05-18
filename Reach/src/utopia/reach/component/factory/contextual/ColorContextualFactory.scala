package utopia.reach.component.factory.contextual

import utopia.firmament.context.{ColorContext, ColorContextWrapper}

/**
  * Common trait for factories that wrap and use a ColorContext instance
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ColorContextualFactory[+Repr] extends ContextualFactory[ColorContext, Repr] with ColorContextWrapper[Repr, Repr]
{
	override def colorContext: ColorContext = context
	override def withColorContext(base: ColorContext): Repr = withContext(base)
	
	override def forTextComponents: Repr = mapContext { _.forTextComponents }
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}

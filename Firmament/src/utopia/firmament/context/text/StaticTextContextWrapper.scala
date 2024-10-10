package utopia.firmament.context.text

import utopia.firmament.context.color.StaticColorContextWrapper
import utopia.firmament.model.stack.StackInsets
import utopia.genesis.text.Font

/**
  * Common implementations of [[StaticTextContextLike]] by wrapping another such instance
  * @author Mikko Hilpinen
  * @since 09.10.2024, v1.3.2
  */
trait StaticTextContextWrapper[Base <: StaticTextContextLike[Base], +Repr]
	extends StaticColorContextWrapper[Base, Repr] with TextContextWrapper2[Base, Repr] with StaticTextContextLike[Repr]
{
	override def promptFont: Font = base.promptFont
	override def textInsets: StackInsets = base.textInsets
	override def lineSplitThreshold: Option[Double] = base.lineSplitThreshold
}

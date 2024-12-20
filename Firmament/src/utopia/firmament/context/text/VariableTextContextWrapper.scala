package utopia.firmament.context.text

import utopia.firmament.context.color.VariableColorContextWrapper
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font

/**
  * Common trait for implementations of [[VariableTextContextLike]] by wrapping another variable text context.
  * @author Mikko Hilpinen
  * @since 09.10.2024, v1.4
  */
trait VariableTextContextWrapper[Base <: VariableTextContextLike[Base], +Repr]
	extends VariableColorContextWrapper[Base, Repr] with TextContextWrapper[Base, Repr]
		with VariableTextContextLike[Repr]
{
	override def withPromptFontPointer(p: Changing[Font]): Repr = mapBase { _.withPromptFontPointer(p) }
	override def withTextInsetsPointer(p: Changing[StackInsets]): Repr = mapBase { _.withTextInsetsPointer(p) }
	override def withLineSplitThresholdPointer(p: Option[Changing[Double]]): Repr =
		mapBase { _.withLineSplitThresholdPointer(p) }
}

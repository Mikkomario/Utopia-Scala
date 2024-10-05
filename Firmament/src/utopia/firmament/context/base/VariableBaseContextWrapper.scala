package utopia.firmament.context.base
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font

/**
  * Common trait for implementations of [[VariableBaseContextLike]] by wrapping one
  * @tparam Base Type of the wrapped base context
  * @tparam Repr Type of this context
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.3.2
  */
trait VariableBaseContextWrapper[Base <: VariableBaseContextLike[Base, _], +Repr]
	extends BaseContextWrapper2[Base, Repr] with VariableBaseContextLike[Repr, Repr]
{
	override def withFontPointer(p: Changing[Font]): Repr = mapBase { _.withFontPointer(p) }
	override def withStackMarginPointer(p: Changing[StackLength]): Repr = mapBase { _.withStackMarginPointer(p) }
	override def withAllowImageUpscalingFlag(f: Flag): Repr = mapBase { _.withAllowImageUpscalingFlag(f) }
}

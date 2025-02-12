package utopia.firmament.context.base
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackLength
import utopia.genesis.text.Font

/**
  * Common trait for implementations of [[StaticBaseContext]] via wrapping one
  * @tparam Base Type of the wrapped base context
  * @tparam Repr Type of this context
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.4
  */
trait StaticBaseContextWrapper[Base <: StaticBaseContextLike[Base, _], +Repr]
	extends BaseContextWrapper[Base, Repr] with StaticBaseContextLike[Repr, Repr]
{
	override def font: Font = base.font
	override def stackMargin: StackLength = base.stackMargin
	override def smallStackMargin: StackLength = base.smallStackMargin
	override def allowImageUpscaling: Boolean = base.allowImageUpscaling
	
	override def withStackMargin(size: SizeCategory) = super[BaseContextWrapper].withStackMargin(size)
	override def mapFont(f: Font => Font) = super[BaseContextWrapper].mapFont(f)
	override def mapStackMargin(f: StackLength => StackLength) = super[BaseContextWrapper].mapStackMargin(f)
}

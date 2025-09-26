package utopia.reach.component.factory.contextual

import utopia.firmament.context.HasContext
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.factory.VariableFramedFactory
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.StackInsets
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}

/**
  * Common trait for component creation factories that place insets around the created components and utilize
  * a component creation context
  * @author Mikko Hilpinen
  * @since 18.5.2023, v1.1
  */
trait ContextualFramedFactory[+Repr] extends VariableFramedFactory[Repr] with HasContext[BaseContextPropsView]
{
	def withTop(inset: SizeCategory) = withSide(Up, inset)
	def withTop(inset: SizeCategory, exclusive: Boolean) = withSide(Up, inset, exclusive)
	def withBottom(inset: SizeCategory) = withSide(Down, inset)
	def withBottom(inset: SizeCategory, exclusive: Boolean) = withSide(Down, inset, exclusive)
	def withLeft(inset: SizeCategory) = withSide(Direction2D.Left, inset)
	def withLeft(inset: SizeCategory, exclusive: Boolean) = withSide(Direction2D.Left, inset, exclusive)
	def withRight(inset: SizeCategory) = withSide(Direction2D.Right, inset)
	def withRight(inset: SizeCategory, exclusive: Boolean) = withSide(Direction2D.Right, inset, exclusive)
	
	def withHorizontalInsets(inset: SizeCategory) = withInsetsAlong(X, inset)
	def withVerticalInsets(inset: SizeCategory) = withInsetsAlong(Y, inset)
	
	/**
	  * @param insetSize Size of the insets to apply on all sides
	  * @return A copy of this factory with the specified symmetric insets
	  */
	def withInsets(insetSize: SizeCategory): Repr =
		withInsetsPointer(context.scaledStackMarginPointer(insetSize).map { _.toInsets })
	
	def withInsetsAlong(axis: Axis2D, insetSize: SizeCategory): Repr = {
		val lengthPointer = context.scaledStackMarginPointer(insetSize)
		flatMapInsets { insets => lengthPointer.map { insets.withAxis(axis, _) } }
	}
	def withSide(side: Direction2D, insetSize: SizeCategory): Repr = {
		val lengthPointer = context.scaledStackMarginPointer(insetSize)
		flatMapInsets { insets => lengthPointer.map { insets.withSide(side, _) } }
	}
	def withSide(side: Direction2D, insetSize: SizeCategory, exclusive: Boolean): Repr = {
		if (exclusive) {
			val lengthPointer = context.scaledStackMarginPointer(insetSize)
			withInsetsPointer(lengthPointer.map { StackInsets.apply(side, _) })
		}
		else
			withSide(side, insetSize)
	}
	@deprecated("Renamed to .withSide(...)", "v1.7")
	def withInset(side: Direction2D, insetSize: SizeCategory): Repr = withSide(side, insetSize)
}

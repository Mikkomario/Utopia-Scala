package utopia.reach.component.factory.contextual

import utopia.firmament.context.HasContext
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.factory.VariableFramedFactory
import utopia.firmament.model.enumeration.SizeCategory
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
	def withTop(inset: SizeCategory) = withInset(Up, inset)
	def withBottom(inset: SizeCategory) = withInset(Down, inset)
	def withLeft(inset: SizeCategory) = withInset(Direction2D.Left, inset)
	def withRight(inset: SizeCategory) = withInset(Direction2D.Right, inset)
	
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
	def withInset(side: Direction2D, insetSize: SizeCategory): Repr = {
		val lengthPointer = context.scaledStackMarginPointer(insetSize)
		flatMapInsets { insets => lengthPointer.map { insets.withSide(side, _) } }
	}
}

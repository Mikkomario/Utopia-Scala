package utopia.reach.component.factory

import utopia.firmament.context.BaseContext
import utopia.firmament.model.enumeration.SizeCategory
import utopia.paradigm.enumeration.{Axis2D, Direction2D}

/**
  * Common trait for component creation factories that place insets around the created components and utilize
  * a component creation context
  * @author Mikko Hilpinen
  * @since 18.5.2023, v1.1
  */
trait ContextualFramedFactory[+Repr] extends FramedFactory[Repr] with HasContext[BaseContext]
{
	/**
	  * @param insetSize Size of the insets to apply on all sides
	  * @return A copy of this factory with the specified symmetric insets
	  */
	def withInsets(insetSize: SizeCategory): Repr = withInsets(context.scaledStackMargin(insetSize))
	
	def withInsetsAlong(axis: Axis2D, insetSize: SizeCategory): Repr =
		withInsetsAlong(axis, context.scaledStackMargin(insetSize))
	def withInset(side: Direction2D, insetSize: SizeCategory): Repr =
		withInset(side, context.scaledStackMargin(insetSize))
}

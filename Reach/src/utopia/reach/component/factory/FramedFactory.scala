package utopia.reach.component.factory

import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible, StackLength}
import utopia.flow.collection.immutable.Pair
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}

/**
  * Common trait for (component) factories that utilize stack insets
  * @author Mikko Hilpinen
  * @since 18.5.2023, v1.1
  */
trait FramedFactory[+Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Insets placed around the created components
	  */
	def insets: StackInsets
	
	/**
	  * @param insets Insets to place around the created components
	  * @return Copy of this factory that uses the specified insets
	  */
	def withInsets(insets: StackInsetsConvertible): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this factory that doesn't place any insets around the image
	  */
	def withoutInsets = withInsets(StackInsets.zero)
	
	/**
	  * @return Copy of this factory without any horizontal insets applied
	  */
	def withoutHorizontalInsets = withoutInsetsAlong(X)
	/**
	  * @return Copy of this factory without any vertical insets applied
	  */
	def withoutVerticalInsets = withoutInsetsAlong(Y)
	
	/**
	  * @return Copy of this factory where the horizontal insets expand easily (within limits)
	  */
	def expandingHorizontally = expandingAlong(X)
	/**
	  * @return Copy of this factory where the vertical insets expand easily (within limits)
	  */
	def expandingVertically = expandingAlong(Y)
	
	/**
	  * @return Copy of this factory where the right side insets expand easily
	  */
	def expandingToRight = expandingTowards(Direction2D.Right)
	/**
	  * @return Copy of this factory where the left side insets expand easily
	  */
	def expandingToLeft = expandingTowards(Direction2D.Left)
	
	
	// OTHER    ------------------------
	
	def mapInsets(f: StackInsets => StackInsetsConvertible) = withInsets(f(insets))
	def mapInsetsAlong(axis: Axis2D)(f: StackLength => StackLength) =
		mapInsets { _.mapDimension(axis) { _.map(f) } }
	def mapInset(side: Direction2D)(f: StackLength => StackLength) = mapInsets { _.mapSide(side)(f) }
	
	def withInsetsAlong(axis: Axis2D, insets: Pair[StackLength]) = mapInsets { _.withDimension(axis, insets) }
	def withInsetsAlong(axis: Axis2D, insets: StackLength): Repr = withInsetsAlong(axis, Pair.twice(insets))
	def withoutInsetsAlong(axis: Axis2D) = mapInsets { _ - axis }
	
	def withInset(side: Direction2D, inset: StackLength) = mapInsets { _.withSide(side, inset) }
	def withoutInset(side: Direction2D) = mapInsets { _ - side }
	
	def expandingAlong(axis: Axis2D) = mapInsetsAlong(axis) { _.expanding }
	def expandingTowards(direction: Direction2D) = mapInset(direction) { _.expanding }
	
	/**
	  * @param mod A scaling modifier to apply to insets
	  * @return Copy of this factory with scaled insets
	  */
	def withInsetsScaledBy(mod: Double) = mapInsets { _ * mod }
}

package utopia.firmament.model.stack

import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.insets.{Insets, SidedBuilder, Sides, SidesFactory}

object StackInsets extends SidesFactory[StackLength, StackInsets]
{
	// TYPES    ---------------------------
	
	/**
	  * A builder that generates StackInsets
	  */
	type StackInsetsBuilder = SidedBuilder[StackLength, StackInsets]
	
	
	// ATTRIBUTES	-----------------------
	
	/**
	  * A set of insets where each side has "any" length (0 or more, preferring 0)
	  */
	val any = symmetric(StackLength.any)
	/**
	  * A set of insets where each side is fixed to 0
	  */
	val zero = symmetric(StackLength.fixedZero)
	
	
	// IMPLEMENTED  -----------------------
	
	override def withSides(sides: Map[Direction2D, StackLength]): StackInsets = apply(sides)
	
	
	// OTHER	---------------------------
	
	/**
	  * Creates a symmetric set of stack insets
	  * @param margins Combined stack insets on each side
	  * @return A symmetric set of insets where the total value is equal to the provided size
	  */
	def symmetric(margins: StackSize): StackInsets = symmetric(margins.width / 2, margins.height / 2)
}

/**
  * A set of insets made of stack lengths
  * @author Mikko Hilpinen
  * @since 2.2.2020, Reflection v1
  */
case class StackInsets(sides: Map[Direction2D, StackLength])
	extends Sides[StackLength] with StackInsetsLike[StackInsets] with StackInsetsConvertible
{
	// ATTRIBUTES	-----------------------
	
	override lazy val dimensions = super.dimensions
	
	/**
	  * The optimal insets within these insets
	  */
	lazy val optimal = mapToInsets { _.optimal }
	/**
	  * The minimum insets within these insets
	  */
	lazy val min = mapToInsets { _.min }
	
	override lazy val total: StackSize = super.total
	
	
	// COMPUTED	---------------------------
	
	@deprecated("Please use .sides instead", "v1.5")
	def amounts = sides
	
	
	// IMPLEMENTED	-----------------------
	
	override def self = this
	
	@deprecated("There's no need to call this method since 'this' already does this", "v2")
	override def toInsets = this
	
	override protected def withSides(sides: Map[Direction2D, StackLength]): StackInsets = StackInsets(sides)
	
	
	// OTHER	---------------------------
	
	/**
	  * Converts these stack insets to normal insets
	  * @param f A mapping function
	  * @return A set of insets with mapped side values
	  */
	def mapToInsets(f: StackLength => Double) = Insets(sides.map { case (d, l) => d -> f(l) })
}
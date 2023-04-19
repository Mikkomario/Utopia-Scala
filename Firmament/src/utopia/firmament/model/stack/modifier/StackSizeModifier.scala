package utopia.firmament.model.stack.modifier

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.firmament.model.stack.{StackLength, StackSize}

import scala.language.implicitConversions

/**
  * Used for modifying / constraining stack sizes
  * @author Mikko Hilpinen
  * @since 15.3.2020, Reflection v1
  */
trait StackSizeModifier
{
	// ABSTRACT	------------------------------
	
	/**
	  * Modifies the specified stack size
	  * @param size Size to modify
	  * @return Size after modification
	  */
	def apply(size: StackSize): StackSize
	
	
	// OTHER	------------------------------
	
	/**
	  * @param other Another stack size modifier
	  * @return A combination of these modifiers where this modifier is applied first and the specified modifier is
	  *         applied second
	  */
	def &&(other: StackSizeModifier) = new CombinedSizeModifier(this, other)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this modifier where all results are mapped with the specified function
	  */
	def map(f: StackSize => StackSize) = this && f
	
	/**
	  * @param side Targeted size side
	  * @param f A mapping function
	  * @return A copy of this modifier where all results are mapped with the specified function over the specified side
	  */
	def mapSide(side: Axis2D)(f: StackLength => StackLength) =
		this && (f: StackLengthModifier).over(side)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this modifier where all result widths are mapped with the specified function
	  */
	def mapWidth(f: StackLength => StackLength) = mapSide(X)(f)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this modifier where all result heights are mapped with the specified function
	  */
	def mapHeight(f: StackLength => StackLength) = mapSide(Y)(f)
}

object StackSizeModifier
{
	// IMPLICIT	----------------------------
	
	implicit def functionToModifier(f: StackSize => StackSize): StackSizeModifier = new FunctionalModifier(f)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param f A modifier function
	  * @return That function as a stack size modifier
	  */
	def apply(f: StackSize => StackSize): StackSizeModifier = new FunctionalModifier(f)
	/**
	  * Creates a modifier that only applies to a single axis
	  * @param axis Targeted axis
	  * @param modifier Stack length modifier to wrap
	  * @return A new stack size modifier
	  */
	def apply(axis: Axis2D)(modifier: StackLengthModifier): StackSizeModifier =
		s => s.mapDimension(axis)(modifier.apply)
	
	/**
	  * Creates a modifier that only applies to stack size width
	  * @param modifier Length modifier to wrap
	  * @return A new stack size modifier
	  */
	def horizontal(modifier: StackLengthModifier) = apply(X)(modifier)
	/**
	  * Creates a modifier that only applies to stack size height
	  * @param modifier Length modifier to wrap
	  * @return A new stack size modifier
	  */
	def vertical(modifier: StackLengthModifier) = apply(Y)(modifier)
	
	
	// NESTED	----------------------------
	
	private class FunctionalModifier(f: StackSize => StackSize) extends StackSizeModifier
	{
		override def apply(size: StackSize) = f(size)
	}
}

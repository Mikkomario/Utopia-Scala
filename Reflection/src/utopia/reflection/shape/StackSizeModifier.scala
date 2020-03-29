package utopia.reflection.shape

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Direction2D

import scala.language.implicitConversions

/**
  * Used for modifying / constraining stack sizes
  * @author Mikko Hilpinen
  * @since 15.3.2020, v1
  */
trait StackSizeModifier
{
	/**
	  * Modifies the specified stack size
	  * @param size Size to modify
	  * @return Size after modification
	  */
	def apply(size: StackSize): StackSize
}

object StackSizeModifier
{
	// IMPLICIT	----------------------------
	
	implicit def functionToModifier(f: StackSize => StackSize): StackSizeModifier = new FunctionalModifier(f)
	
	
	// OTHER	----------------------------
	
	/**
	  * Creates a modifier that only applies to a single axis
	  * @param axis Targeted axis
	  * @param modifier Stack length modifier to wrap
	  * @return A new stack size modifier
	  */
	def apply(axis: Axis2D)(modifier: StackLengthModifier): StackSizeModifier = s => s.mapSide(axis)(modifier.apply)
	
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

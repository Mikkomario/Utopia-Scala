package utopia.reflection.shape

import scala.language.implicitConversions

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D

/**
  * Used for modifying stack lengths
  * @author Mikko Hilpinen
  * @since 15.3.2020, v1
  */
trait StackLengthModifier
{
	// ABSTRACT	----------------------
	
	/**
	  * Modifies a stack length
	  * @param length original length
	  * @return Modified length
	  */
	def apply(length: StackLength): StackLength
	
	
	// COMPUTED	----------------------
	
	/**
	  * @return This modifier applied over stack size width
	  */
	def overHorizontal = over(X)
	
	/**
	  * @return This modifier applied over stack size height
	  */
	def overVertical = over(Y)
	
	/**
	  * @return This modifier applied over both stack width and height
	  */
	def symmetric: StackSizeModifier = s => s.map { (_, l) => apply(l) }
	
	
	// OTHER	----------------------
	
	/**
	  * @param axis Targeted axis
	  * @return This modifier applied over specified axis
	  */
	def over(axis: Axis2D) = StackSizeModifier(axis)(this)
}

object StackLengthModifier
{
	// IMPLICIT	---------------------
	
	implicit def functionToModifier(f: StackLength => StackLength): StackLengthModifier = new FunctionalModifier(f)
	
	
	// NESTED	---------------------
	
	private class FunctionalModifier(f: StackLength => StackLength) extends StackLengthModifier
	{
		override def apply(length: StackLength) = f(length)
	}
}
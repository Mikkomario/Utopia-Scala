package utopia.firmament.model.stack.modifier

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.firmament.model.stack.StackLength

import scala.language.implicitConversions

/**
  * Used for modifying stack lengths
  * @author Mikko Hilpinen
  * @since 15.3.2020, Reflection v1
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
	  * @param another Another stack length modifier
	  * @return A combination of these modifiers where this modifier is applied first and the other modifier
	  *         is applied after that
	  */
	def &&(another: StackLengthModifier) = new CombinedLengthModifier(this, another)
	
	/**
	  * @param f A mapping function applied to the results of this modifier
	  * @return A copy of this modifier with the mapping function applied to all results
	  */
	def mapResult(f: StackLength => StackLength) = this && f
	
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
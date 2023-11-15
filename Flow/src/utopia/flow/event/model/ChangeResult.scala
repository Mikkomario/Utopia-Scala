package utopia.flow.event.model

import utopia.flow.view.immutable.View
import utopia.flow.view.template.Extender

import scala.language.implicitConversions

object ChangeResult
{
	/**
	  * @param value A value
	  * @tparam A Type of the specified value
	  * @return A wrapper that indicates that the specified value is temporal (i.e. may be replaced later)
	  */
	implicit def temporal[A](value: A): ChangeResult[A] = apply(value)
	/*** @param value A value
	  
	  * @tparam A Type of the specified value
	  * @return A wrapper that indicates that the specified value is final (i.e. won't be replaced)
	  */
	def finalValue[A](value: A) = apply(value, isFinal = true)
}

/**
  * Combines a value with information concerning its temporality / role
  * (whether it is the final change value or an intermediate value instead)
  * @author Mikko Hilpinen
  * @since 14.11.2023, v2.3
  *
  * @param value Value to wrap
  * @param isFinal Whether this represents the final value of a no-longer changing item
  */
case class ChangeResult[+A](value: A, isFinal: Boolean = false) extends View[A] with Extender[A]
{
	// COMPUTED ----------------------
	
	/**
	  * @return Whether this value may eventually be replaced with another value
	  */
	def isTemporal = !isFinal
	
	/**
	  * @return This result marked as the final value
	  */
	def asFinal = if (isFinal) this else copy(isFinal = true)
	
	
	// IMPLEMENTED  ------------------
	
	override def wrapped: A = value
	
	override def toString = if (isFinal) s"$wrapped (final)" else wrapped.toString
	
	override def mapValue[B](f: A => B) = copy(value = f(value))
	
	
	// OTHER    ----------------------
	
	/**
	  * @param v A value to test
	  * @tparam B Type of the specified value
	  * @return Whether this result contains the specified value as a final state
	  */
	def containsFinal[B >: A](v: => B) = isFinal && value == v
	/**
	  * @param f A condition
	  * @return Whether this result contains a final value that matches the specified condition
	  */
	def existsFinal(f: A => Boolean) = isFinal && f(value)
}

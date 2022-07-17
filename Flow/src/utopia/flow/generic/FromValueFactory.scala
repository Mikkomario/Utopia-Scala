package utopia.flow.generic

import scala.language.implicitConversions

import utopia.flow.datastructure.immutable.Value

/**
  * A factory used for parsing items from values
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.16
  * @tparam A Type of instances returned by this factory
  */
trait FromValueFactory[+A]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The default parse result
	  */
	def default: A
	
	/**
	  * Parses an item from the specified value
	  * @param value A value
	  * @return Item parsed from that value. None if no item could be parsed.
	  */
	def fromValue(value: Value): Option[A]
	
	
	// IMPLICIT --------------------------
	
	implicit def unwrap(value: Value): A = getFromValue(value)
	implicit def unwrapOption(value: Value): Option[A] = fromValue(value)
	
	
	// OTHER    --------------------------
	
	/**
	  * Parses an item from the specified value. Returns the default value if parsing didn't succeed.
	  * @param value A value
	  * @return Item parsed from that value, or the default value
	  */
	def getFromValue(value: Value) = fromValue(value).getOrElse(default)
}

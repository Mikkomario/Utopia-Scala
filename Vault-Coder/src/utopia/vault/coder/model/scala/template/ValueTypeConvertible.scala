package utopia.vault.coder.model.scala.template

import utopia.vault.coder.model.scala.code.CodePiece

/**
  * A common trait for data types which may be somehow converted to the Value data type
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  */
trait ValueTypeConvertible
{
	/**
	  * @return The default value assigned for this type (in scala) (may be empty)
	  */
	def defaultValue: CodePiece
	
	/**
	  * Writes a code that reads this an instance of this type from a value
	  * @param valueCode Code for accessing a value
	  * @return Code for accessing a value and converting it to this type (in scala)
	  */
	def fromValueCode(valueCode: String): CodePiece
	/**
	  * Writes a code that reads a vector of instances of this type from a vector of values
	  * @param valuesCode Code that returns a vector of values
	  * @return Code for accessing the specified values and converting them to a vector of this type's instances in Scala
	  */
	def fromValuesCode(valuesCode: String): CodePiece
	/**
	  * Writes a code that converts an instance of this type into a Value.
	  * @param instanceCode Code for referring to the instance to convert
	  * @return A code that returns a Value based on the presented instance
	  */
	def toValueCode(instanceCode: String): CodePiece
}

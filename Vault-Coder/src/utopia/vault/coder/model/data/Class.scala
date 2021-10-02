package utopia.vault.coder.model.data

import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, LongNumber}
import utopia.vault.coder.model.enumeration.PropertyType.{CreationTime, Deprecation, EnumValue, Expiration}
import utopia.vault.coder.util.NamingUtils

object Class
{
	/**
	  * Creates a new class with automatic table-naming
	  * @param name Name of this class (in code)
	  * @param properties Properties in this class
	  * @param packageName Name of the package in which to wrap this class (default = empty)
	  * @param description A description of this class (default = empty)
	  * @param author Author who wrote this class (may be empty)
	  * @param useLongId Whether to use long instead of int in the id property (default = false)
	  * @return A new class
	  */
	def apply(name: Name, properties: Vector[Property], packageName: String = "", description: String = "",
	          author: String = "", useLongId: Boolean = false): Class =
		apply(name, NamingUtils.camelToUnderscore(name.singular), properties, packageName, description,
			author, useLongId)
}

/**
  * Represents a base class, from which multiple specific classes and interfaces are created
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Name of this class (in code)
  * @param tableName Name of this class' table
  * @param properties Properties in this class
  * @param packageName Name of the package in which to wrap this class (may be empty)
  * @param description A description of this class
  * @param useLongId Whether to use long instead of int in the id property
  */
case class Class(name: Name, tableName: String, properties: Vector[Property], packageName: String,
                 description: String, author: String, useLongId: Boolean)
{
	/**
	  * @return Type of the ids used in this class
	  */
	def idType =  if (useLongId) LongNumber else IntNumber
	
	/**
	  * @return Whether this class records a row / instance creation time
	  */
	def recordsCreationTime = properties.exists { _.dataType match {
		case CreationTime => true
		case _ => false
	} }
	
	/**
	  * @return Whether this class supports deprecation or expiration
	  */
	def isDeprecatable = properties.exists { _.dataType match {
		case Deprecation | Expiration => true
		case _ => false
	} }
	
	/**
	  * @return Whether this class refers to one or more enumerations in its properties
	  */
	def refersToEnumerations = properties.exists { _.dataType match {
		case _: EnumValue => true
		case _ => false
	} }
	
	/**
	  * @return The property in this class which contains instance creation time. None if no such property is present.
	  */
	def creationTimeProperty = properties.find { _.dataType match {
		case CreationTime => true
		case _ => false
	} }
	
	/**
	  * @return Property in this class which contains instance deprecation time. None if no such property is present.
	  */
	def deprecationProperty = properties.find { _.dataType match {
		case Deprecation => true
		case _ => false
	} }
	
	/**
	  * @return Property in this class which contains instance expiration time. None if no such property is present.
	  */
	def expirationProperty = properties.find { _.dataType match {
		case Expiration => true
		case _ => false
	} }
}

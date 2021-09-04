package utopia.citadel.coder.model.data

import utopia.citadel.coder.model.enumeration.BasicPropertyType.{BigInteger, Integer}
import utopia.citadel.coder.model.enumeration.PropertyType.CreationTime
import utopia.citadel.coder.util.NamingUtils

object Class
{
	/**
	  * Creates a new class with automatic table-naming
	  * @param name Name of this class (in code)
	  * @param properties Properties in this class
	  * @param packageName Name of the package in which to wrap this class (default = empty)
	  * @param description A description of this class (default = empty)
	  * @param useLongId Whether to use long instead of int in the id property (default = false)
	  * @return A new class
	  */
	def apply(name: Name, properties: Vector[Property], packageName: String = "", description: String = "",
	          useLongId: Boolean = false): Class =
		apply(name, NamingUtils.camelToUnderscore(name.singular), properties, packageName, description, useLongId)
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
                 description: String, useLongId: Boolean)
{
	/**
	  * @return Type of the ids used in this class
	  */
	def idType =  if (useLongId) BigInteger else Integer
	
	/**
	  * @return The property in this class which contains instance creation time. None if no such property is present.
	  */
	def creationTimeProperty = properties.find { _.dataType match {
		case CreationTime => true
		case _ => false
	} }
	
	/**
	  * @return Whether this class records a row / instance creation time
	  */
	def recordsCreationTime = properties.exists { _.dataType match {
		case CreationTime => true
		case _ => false
	} }
}

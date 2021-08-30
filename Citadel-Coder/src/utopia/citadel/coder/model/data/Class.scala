package utopia.citadel.coder.model.data

import utopia.citadel.coder.util.NamingUtils

object Class
{
	/**
	  * Creates a new class with automatic table-naming
	  * @param name Name of this class (in code)
	  * @param properties Properties in this class
	  * @param packageName Name of the package in which to wrap this class (default = empty)
	  * @param useLongId Whether to use long instead of int in the id property (default = false)
	  * @return A new class
	  */
	def apply(name: String, properties: Vector[Property], packageName: String = "", useLongId: Boolean = false): Class =
		apply(name, NamingUtils.camelToUnderscore(name), properties, packageName, useLongId)
}

/**
  * Represents a base class, from which multiple specific classes and interfaces are created
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Name of this class (in code)
  * @param tableName Name of this class' table
  * @param properties Properties in this class
  * @param packageName Name of the package in which to wrap this class (may be empty)
  * @param useLongId Whether to use long instead of int in the id property
  */
case class Class(name: String, tableName: String, properties: Vector[Property], packageName: String, useLongId: Boolean)

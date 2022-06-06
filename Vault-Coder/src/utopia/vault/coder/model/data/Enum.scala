package utopia.vault.coder.model.data

import utopia.vault.coder.model.scala.Package
import utopia.vault.coder.model.scala.datatype.Reference

/**
  * Represents a custom (user-defined) enumeration
  * @author Mikko Hilpinen
  * @since 25.9.2021, v1.1
  * @param name Name of this enumeration
  * @param values Available values of this enumeration (ordered)
  * @param packagePath Name of the package that contains this enumeration trait / values
  * @param author Author who wrote these enumerations (default = empty)
  */
case class Enum(name: String, values: Vector[String], packagePath: Package, author: String = "")
{
	/**
	  * Reference to this enumeration
	  */
	val reference = Reference(packagePath, name)
}

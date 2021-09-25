package utopia.vault.coder.model.data

import utopia.vault.coder.model.scala.Reference

/**
  * Represents a custom (user-defined) enumeration
  * @author Mikko Hilpinen
  * @since 25.9.2021, v1.1
  * @param name Name of this enumeration
  * @param values Available values of this enumeration (ordered)
  * @param packagePath Name of the package that contains this enumeration trait / values
  */
case class Enum(name: Name, values: Vector[Name], packagePath: String)
{
	/**
	  * Reference to this enumeration
	  */
	val reference = Reference(packagePath, name.singular)
}

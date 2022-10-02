package utopia.vault.coder.model.data

import utopia.vault.coder.model.datatype.BasicPropertyType.IntNumber
import utopia.vault.coder.model.datatype.PropertyType
import utopia.vault.coder.model.enumeration.IntSize.Tiny
import utopia.vault.coder.model.scala.Package
import utopia.vault.coder.model.scala.datatype.Reference

/**
  * Represents a custom (user-defined) enumeration
  * @author Mikko Hilpinen
  * @since 25.9.2021, v1.1
  * @param name Name of this enumeration
  * @param packagePath Name of the package that contains this enumeration trait / values
  * @param values Available values of this enumeration (ordered)
  * @param defaultValue The default value of this enumeration (if applicable)
  * @param idPropName Name of the id property used by this enumeration
  * @param idType Type of the id used by this enumeration (default = tiny int)
  * @param description Description / documentation of this enumeration (default = empty)
  * @param author Author who wrote this enumeration (default = empty)
  */
case class Enum(name: Name, packagePath: Package, values: Vector[EnumerationValue], defaultValue: Option[EnumerationValue] = None,
                idPropName: Name = "id", idType: PropertyType = IntNumber(Tiny),
                description: String = "", author: String = "")
{
	/**
	  * @return Whether this enumeration has a default value
	  */
	def hasDefault = defaultValue.isDefined
	/**
	  * @return Whether this enumeration has no default value
	  */
	def hasNoDefault = !hasDefault
	
	/**
	  * Reference to this enumeration
	  */
	def reference(implicit naming: NamingRules) = Reference(packagePath, name.enumName)
}

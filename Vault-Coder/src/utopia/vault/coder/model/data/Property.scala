package utopia.vault.coder.model.data

import utopia.vault.coder.model.enumeration.PropertyType
import utopia.vault.coder.util.NamingUtils

object Property
{
	/**
	  * Creates a new property with automatic naming
	  * @param name Name of this property
	  * @param dataType Type of this property
	  * @param description Description of this property (Default = empty)
	  * @param useDescription Description on how this property is used (Default = empty)
	  * @param customDefault Default value passed for this property (empty if no default (default))
	  * @return A new property
	  */
	def apply(name: Name, dataType: PropertyType, description: String = "", useDescription: String = "",
	          customDefault: String = ""): Property =
		apply(name, NamingUtils.camelToUnderscore(name.singular), dataType, description, useDescription, customDefault)
}

/**
  * Classes define certain typed properties that are present in every instance
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Name of this property
  * @param columnName Name of the column linked with this property
  * @param dataType Type of this property
  * @param description Description of this property (may be empty)
  * @param useDescription Description on how this property is used (may be empty)
  * @param customDefault Default value passed for this property (empty if no default)
  */
case class Property(name: Name, columnName: String, dataType: PropertyType, description: String,
                    useDescription: String, customDefault: String)
{
	/**
	  * @return Code for this property converted to a value. Expects ValueConversions to be imported.
	  */
	def toValueCode = dataType.toValueCode(name.singular)
	
	/**
	  * @return A nullable copy of this property
	  */
	def nullable = if (dataType.isNullable) this else copy(dataType = dataType.nullable)
}

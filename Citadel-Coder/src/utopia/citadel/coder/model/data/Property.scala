package utopia.citadel.coder.model.data

import utopia.citadel.coder.model.enumeration.PropertyType
import utopia.citadel.coder.util.NamingUtils

object Property
{
	/**
	  * Creates a new property with automatic naming
	  * @param name Name of this property
	  * @param dataType Type of this property
	  * @param description Description of this property (Default = empty)
	  * @param useDescription Description on how this property is used (Default = empty)
	  * @return A new property
	  */
	def apply(name: String, dataType: PropertyType, description: String = "", useDescription: String = ""): Property =
		apply(name, NamingUtils.camelToUnderscore(name), dataType, description, useDescription)
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
  */
case class Property(name: String, columnName: String, dataType: PropertyType, description: String,
                    useDescription: String)

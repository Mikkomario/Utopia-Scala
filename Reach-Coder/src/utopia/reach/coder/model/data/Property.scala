package utopia.reach.coder.model.data

import utopia.coder.model.data.{Name, Named, NamingRules}
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Reference, ScalaType}

object Property
{
	/**
	  * Creates a new property with default naming logic
	  * @param name The name of this property
	  * @param dataType Type of this property
	  * @param defaultValue The default value used for this property in settings (default = empty = no default)
	  * @param description Description of the function of this property (default = empty)
	  * @param mappingEnabled Whether mapping functions shall be used for this property
	  * @return A new property
	  */
	def simple(name: Name, dataType: ScalaType, defaultValue: CodePiece = CodePiece.empty, description: String = "",
	           mappingEnabled: Boolean = false) =
		apply(name, dataType, "with" +: name, name, defaultValue, description = description,
			mappingEnabled = mappingEnabled)
	
	/**
	  * Creates a property that refers to settings from another component
	  * @param factory Targeted component factory
	  * @param prefix Prefix added to all referenced properties (default = empty = no prefix)
	  * @param description Description of this property (default = empty)
	  * @param prefixDerivedProperties Whether derived (referenced) properties should be prefixed (default = true)
	  * @param naming Naming rules to apply
	  * @return A new property
	  */
	def referringTo(factory: ComponentFactory, prefix: Name = Name.empty, description: String = "",
	                prefixDerivedProperties: Boolean = true)
	               (implicit naming: NamingRules) =
	{
		val name = prefix + "Settings"
		val target = (factory.componentName + "Settings").className
		apply(name, Reference(factory.pck, target), "with" +: name, "settings", s"$target.default",
			Some(factory -> prefix), description, mappingEnabled = true,
			prefixDerivedProperties = prefixDerivedProperties)
	}
}

/**
  * Used for defining customizable component creation properties / parameters
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  * @constructor Defines a new property
  * @param name Name of this property. E.g. "property"
  * @param dataType Type of this property
  * @param setterName Name of the setter used for modifying this property. E.g. "withProperty"
  * @param setterParamName Name of this property when it appears as the setter parameter. E.g. "prop"
  * @param defaultValue The default value used for this property in settings
  * @param reference Referenced component factory, if applicable
  * @param description A description about the function of this property
  * @param mappingEnabled Whether mapping functions shall be used for this property
  * @param prefixDerivedProperties Whether derived (referenced) properties should be prefixed
  */
case class Property(name: Name, dataType: ScalaType, setterName: Name, setterParamName: Name,
                    defaultValue: CodePiece = CodePiece.empty, reference: Option[(ComponentFactory, Name)] = None,
                    description: String = "", mappingEnabled: Boolean = false, prefixDerivedProperties: Boolean = false)
	extends Named
{
	/**
	  * @param defaultValue New default value to use
	  * @return Copy of this property with the specified default value
	  */
	def withDefault(defaultValue: CodePiece) = copy(defaultValue = defaultValue)
}
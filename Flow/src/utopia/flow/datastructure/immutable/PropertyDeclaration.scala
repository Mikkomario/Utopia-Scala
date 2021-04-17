package utopia.flow.datastructure.immutable

import utopia.flow.generic.DataType

object PropertyDeclaration
{
    /**
      * Creates a new declaration
      * @param name Property name
      * @param dataType Property data type (primary)
      * @param defaultValue The default value set for the property (default = None)
      * @return A new property declaration
      */
    def apply(name: String, dataType: DataType, defaultValue: Option[Value]): PropertyDeclaration =
        PropertyDeclarationImpl(name, dataType, defaultValue)
    
    /**
      * Creates a new declaration
      * @param name Property name
      * @param dataType Property data type (primary)
      * @param defaultValue The default value set for the property
      * @return A new property declaration
      */
    def apply(name: String, dataType: DataType, defaultValue: Value): PropertyDeclaration = apply(name, dataType,
        Some(defaultValue))
    
    /**
      * Creates a new declaration
      * @param name Property name
      * @param dataType Property data type (primary)
      * @return A new property declaration
      */
    def apply(name: String, dataType: DataType): PropertyDeclaration = apply(name, dataType, None)
    
    /**
      * Creates a new declaration
      * @param name Property name
      * @param defaultValue The default value set for the property
      * @return A new property declaration
      */
    def apply(name: String, defaultValue: Value): PropertyDeclaration = apply(name, defaultValue.dataType, defaultValue)
}

/**
 * Property declarations are used for defining and instantiating model properties
 * @author Mikko Hilpinen
 * @since 1.12.2016
 */
trait PropertyDeclaration extends Equals
{
    // ABSTRACT -----------------------
    
    /**
      * @return The name for this property
      */
    def name: String
    /**
      * @return Primary data type for the value in this property
      */
    def dataType: DataType
    /**
      * @return A default value for this property
      */
    def defaultValue: Option[Value]
    
    
    // IMPLEMENTED  ------------------
    
    override def toString = s"$name ($dataType)${
        defaultValue.map { v => s" (default: ${v.description})" }.getOrElse("")}"
    
    
    // COMPUTED ----------------------
    
    /**
      * @return Whether this declaration specifies a default value
      */
    def hasDefault = defaultValue.isDefined
}

private case class PropertyDeclarationImpl(override val name: String, override val dataType: DataType,
                                           override val defaultValue: Option[Value]) extends PropertyDeclaration
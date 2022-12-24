package utopia.flow.generic.model.immutable

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType

import scala.collection.immutable.VectorBuilder

object PropertyDeclaration
{
    /**
      * Creates a new declaration
      * @param name Property name
      * @param dataType Property data type
      * @param alternativeNames Alternative names for this property (default = empty)
      * @param defaultValue The default value set for the property (default = empty value)
      * @param isOptional Whether the declared property may be empty or missing from the source material
      *                   (default = false = the property shall be required and must not be empty)
      * @return A new property declaration
      */
    def apply(name: String, dataType: DataType, alternativeNames: Vector[String] = Vector(),
              defaultValue: Value = Value.empty, isOptional: Boolean = false): PropertyDeclaration =
        PropertyDeclarationImpl(name, alternativeNames, dataType, defaultValue, isOptional)
    
    /**
      * Creates a new declaration
      * @param name Property name
      * @param dataType Property data type (primary)
      * @param alternativeNames Alternative names for this property (default = empty)
      * @return A new property declaration
      */
    def required(name: String, dataType: DataType, alternativeNames: Vector[String] = Vector()): PropertyDeclaration =
        apply(name, dataType, alternativeNames, Value.emptyWithType(dataType))
    
    /**
      * Creates a new optional property declaration.
      * Optional properties are such that may be left empty,
      * or for which no data must be provided when building a model.
      * @param name Name of the declared property
      * @param dataType Data type of the declared property
      * @param alternativeNames Alternative property names (default = empty)
      * @return A new property declaration
      */
    def optional(name: String, dataType: DataType, alternativeNames: Vector[String] = Vector()) =
        apply(name, dataType, alternativeNames, Value.emptyWithType(dataType), isOptional = true)
    
    /**
      * Creates a new optional declaration with a default value
      * @param name Property name
      * @param defaultValue The default value set for the property, which also determines the property's data type
      * @param alternativeNames Alternative property names (default = empty)
      * @return A new property declaration
      */
    def withDefault(name: String, defaultValue: Value, alternativeNames: Vector[String] = Vector()): PropertyDeclaration =
        apply(name, defaultValue.dataType, alternativeNames, defaultValue, isOptional = true)
    
    
    // NESTED   ----------------------
    
    private case class PropertyDeclarationImpl(override val name: String, override val alternativeNames: Vector[String],
                                               override val dataType: DataType,
                                               override val defaultValue: Value = Value.empty,
                                               override val isOptional: Boolean = false)
        extends PropertyDeclaration
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
      * @return Alternative names for this property
      */
    def alternativeNames: Vector[String]
    /**
      * @return Primary data type for the value in this property
      */
    def dataType: DataType
    /**
      * @return A default value for this property
      */
    def defaultValue: Value
    /**
      * @return Whether the declared property may be empty or missing from source material
      */
    def isOptional: Boolean
    
    
    // COMPUTED ----------------------
    
    /**
      * @return True if this property is required to be specified and non-empty.
      */
    def isRequired = !isOptional && !hasDefault
    
    /**
      * @return All name variations of the declared property
      */
    def names = name +: alternativeNames
    
    /**
      * @return A constant based on this property declaration, for writing schemas as models
      */
    def toConstant = {
        val value: Value = {
            // Case: Simple declaration with only name and data type => String type property
            if (alternativeNames.isEmpty && defaultValue.isEmpty && !isOptional)
                dataType.name
            // Case: More advanced declaration => Model type property
            else {
                val builder = new VectorBuilder[Constant]()
                builder += Constant("datatype", dataType.name)
                if (alternativeNames.nonEmpty)
                    builder += Constant("alt_names", alternativeNames)
                if (defaultValue.nonEmpty)
                    builder += Constant("default", defaultValue)
                if (isOptional)
                    builder += Constant("optional", isOptional)
                Model.withConstants(builder.result())
            }
        }
       Constant(name, value)
    }
    
    
    // IMPLEMENTED  ------------------
    
    override def toString = s"$name ($dataType)${
        defaultValue.notEmpty.map { v => s" (default: ${v.description})" }.getOrElse("")}"
    
    
    // COMPUTED ----------------------
    
    /**
      * @return Whether this declaration specifies a default value
      */
    def hasDefault = defaultValue.isDefined
}
package utopia.flow.generic

import utopia.flow.datastructure.template.Property
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.util.Equatable

/**
 * This property generator implementation uses a model declaration when creating new properties
 * @author Mikko Hilpinen
 * @since 16.12.2016
 */
abstract class DeclarationPropertyGenerator[T <: Property](val createProperty: (String, Value) => T, 
        val declaration: ModelDeclaration, val defaultValue: Value = Value.empty) extends
        PropertyGenerator[T]
{
    override def apply(propertyName: String, value: Option[Value] = None) = 
    {
        // Uses the declaration's data type (and default value, if possible and necessary)
        val propDec = declaration.find(propertyName)
        if (propDec.isDefined)
        {
            createProperty(propertyName, value.orElse(propDec.get.defaultValue).getOrElse(
                    defaultValue) withType propDec.get.dataType)
        }
        // Or just generates a new property with the default value
        else
        {
            createProperty(propertyName, value.getOrElse(defaultValue))
        }
    }
}
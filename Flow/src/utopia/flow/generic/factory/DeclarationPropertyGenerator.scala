package utopia.flow.generic.factory

import utopia.flow.generic.model.immutable.{ModelDeclaration, Value}
import utopia.flow.generic.model.template.Property
import utopia.flow.operator.equality.EqualsBy

/**
 * This property generator implementation uses a model declaration when creating new properties
 * @author Mikko Hilpinen
 * @since 16.12.2016
 */
@deprecated("Please use ModelDeclaration.toPropertyFactory(...) instead", "v2.0")
abstract class DeclarationPropertyGenerator[T <: Property](val createProperty: (String, Value) => T, 
        val declaration: ModelDeclaration, val defaultValue: Value = Value.empty)
    extends PropertyFactory[T] with EqualsBy
{
    override def apply(propertyName: String, value: Value = Value.empty) =
    {
        // Uses the declaration's data type (and default value, if possible and necessary)
        val propDec = declaration.find(propertyName)
        if (propDec.isDefined) {
            val actualValue = {
                if (value.isEmpty) {
                    if (propDec.get.hasDefault)
                        propDec.get.defaultValue
                    else
                        defaultValue
                }
                else
                    value
            }
            createProperty(propertyName, actualValue withType propDec.get.dataType)
        }
        // Or just generates a new property with the default value
        else
            createProperty(propertyName, value.notEmpty.getOrElse(defaultValue))
    }
}
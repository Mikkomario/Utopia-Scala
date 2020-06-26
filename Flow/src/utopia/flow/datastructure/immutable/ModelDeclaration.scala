package utopia.flow.datastructure.immutable

import utopia.flow.util.CollectionExtensions._
import utopia.flow.datastructure.template.{NoSuchAttributeException, Property}
import utopia.flow.datastructure.template
import utopia.flow.generic.{DataType, ModelType, StringType, VectorType}
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.VectorBuilder

object ModelDeclaration
{
    /**
      * Creates a new model declaration
      * @param declarations Property declarations
      */
    def apply(declarations: Seq[PropertyDeclaration]) = new ModelDeclaration(declarations.distinctWith {
        case (a, b) => a.name.equalsIgnoreCase(b.name) }.toSet)
    
    /**
      * Creates a model declaration with a single property
      * @param declaration property declaration
      */
    def apply(declaration: PropertyDeclaration) = new ModelDeclaration(Set(declaration))
    
    /**
      * Creates a new model declaration
      */
    def apply(first: PropertyDeclaration, second: PropertyDeclaration, more: PropertyDeclaration*): ModelDeclaration =
        apply(Vector(first, second) ++ more)
    
    /**
      * Creates a new model declaration from property name - data type -pairs
      */
    def apply(first: (String, DataType), second: (String, DataType), more: (String, DataType)*): ModelDeclaration =
        apply((Vector(first, second) ++ more).map { case (name, t) => PropertyDeclaration(name, t) })
}

/**
 * Used to access a number of property declarations
 * @author Mikko Hilpinen
 * @since 11.12.2016
 */
case class ModelDeclaration private(declarations: Set[PropertyDeclaration])
{
    // COMP. PROPERTIES    ----
    
    /**
     * The names of the properties declared in this declaration
     */
    def propertyNames = declarations.map { _.name }
    
    
    // OPERATORS    -----------
    
    /**
     * Creates a new declaration with the provided declaration included
     */
    def +(declaration: PropertyDeclaration) = new ModelDeclaration(declarations + declaration)
    
    /**
     * Creates a new declaration with the provided declarations included
     */
    def ++(declarations: IterableOnce[PropertyDeclaration]) = new ModelDeclaration(
            this.declarations ++ declarations)
    
    /**
     * Creates a new declaration that also contains the declarations of the other declaration
     */
    def ++(other: ModelDeclaration): ModelDeclaration = this ++ other.declarations
    
    /**
     * Creates a new declaration without the provided property declaration
     */
    def -(declaration: PropertyDeclaration) = new ModelDeclaration(declarations - declaration)
    
    /**
     * Creates a new declaration without any of the provided declarations
     */
    def --(declarations: IterableOnce[PropertyDeclaration]) = new ModelDeclaration(
            this.declarations -- declarations)
    
    /**
     * Creates a new declaration wihthout any declarations from the provided model
     */
    def --(other: ModelDeclaration): ModelDeclaration = this -- other.declarations
    
    
    // OTHER METHODS    -------
    
    /**
     * Finds a property declaration with the provided name, if one exists
     * @param propertyName The name of the property
     * @return the declaration for the property, if one exists
     */
    def find(propertyName: String) = declarations.find { _.name.equalsIgnoreCase(propertyName) }
    
    /**
     * Finds a property declaration with the provided name or fails
     * @param propertyName The name of the property
     * @return The declaration for the property
     * @throws NoSuchAttributeException if there is no such declaration
     */
    @throws(classOf[NoSuchAttributeException])
    def get(propertyName: String) = find(propertyName).getOrElse(
            throw new NoSuchAttributeException(s"No property named '$propertyName' declared"))
    
    /**
      * Returns whether this declaration declares a property with the specified name (case-insensitive)
      * @param propertyName Property name
      */
    def contains(propertyName: String) = declarations.exists { _.name.equalsIgnoreCase(propertyName) }
    
    /**
      * Checks the provided model whether all declared (non-default) properties have non-empty values and can be casted
      * to declared type
      * @param model Model to be validated
      * @return Validation results that either contain the modified model or a reason for validation failure (either
      *         missing properties or failed casting)
      */
    def validate(model: template.Model[Property]) =
    {
        // First checks for missing attributes
        val missing = declarations.filterNot { d => model.contains(d.name) }
        val (missingNonDefaults, missingDefaults) = missing.divideBy { _.defaultValue.isDefined }
        
        // Declarations with default values are replaced with their defaults
        if (missingNonDefaults.nonEmpty)
            ModelValidationResult.missing(model, missingNonDefaults)
        else
        {
            // Tries to convert all declared model properties to required types and checks that each declared (non-default)
            // property has been defined
            val keepBuilder = new VectorBuilder[Constant]()
            val castBuilder = new VectorBuilder[Constant]()
            val castFailedBuilder = new VectorBuilder[(Constant, DataType)]()
    
            model.attributesWithValue.foreach { att =>
                find(att.name) match
                {
                    case Some(declaration) =>
                        val castValue = att.value.castTo(declaration.dataType)
                        if (castValue.isDefined)
                            castBuilder += Constant(att.name, castValue.get)
                        else
                            castFailedBuilder += (Constant(att.name, att.value) -> declaration.dataType)
                    case None => keepBuilder += Constant(att.name, att.value)
                }
            }
            
            // If all values could be cast, proceeds to create the model, otherwise fails
            val castFailed = castFailedBuilder.result()
            if (castFailed.isEmpty)
            {
                // Makes sure all required values have a non-empty value associated with them
                // (works for strings, models and vectors)
                val castValues = castBuilder.result()
                val emptyValues = castValues.filter { c => valueIsEmpty(c.value) }
                if (emptyValues.nonEmpty)
                    ModelValidationResult.missing(model,
                        declarations.filter { d => emptyValues.exists { _.name ~== d.name } })
                else
                {
                    val resultConstants = keepBuilder.result() ++ castValues ++ missingDefaults.map {
                        d => Constant(d.name, d.defaultValue.get) }
                    ModelValidationResult.success(model, Model.withConstants(resultConstants))
                }
            }
            else
                ModelValidationResult.castFailed(model, castFailed.toSet)
        }
    }
    
    private def valueIsEmpty(value: Value): Boolean =
    {
        if (value.isEmpty)
            true
        else
        {
            value.dataType match
            {
                case StringType => value.getString.isEmpty
                case VectorType => value.getVector.forall(valueIsEmpty)
                case ModelType => value.getModel.attributes.map { _.value }.forall(valueIsEmpty)
                case _ => false
            }
        }
    }
}
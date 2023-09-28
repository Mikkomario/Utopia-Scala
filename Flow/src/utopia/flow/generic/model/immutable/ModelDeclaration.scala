package utopia.flow.generic.model.immutable

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.generic.model.{immutable, template}
import utopia.flow.generic.model.mutable.{DataType, Variable}
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.mutable.DataType.{ModelType, StringType, VectorType}

import scala.collection.immutable.VectorBuilder

object ModelDeclaration
{
    // ATTRIBUTES   ---------------------
    
    /**
     * An empty model declaration
     */
    val empty = new ModelDeclaration(Set(), Map())
    
    
    // OTHER    -------------------------
    
    /**
      * Creates a new model declaration
      * @param declarations Property declarations
      */
    def apply(declarations: Seq[PropertyDeclaration]) =
        new ModelDeclaration(declarations.distinctWith { case (a, b) => a.name.equalsIgnoreCase(b.name) }.toSet, Map())
    
    /**
      * Creates a model declaration with a single property
      * @param declaration property declaration
      */
    def apply(declaration: PropertyDeclaration) = new ModelDeclaration(Set(declaration), Map())
    
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
    
    /**
     * @param declaration A property declaration (name + data type)
     * @return A model declaration based on that property declaration
     */
    def apply(declaration: (String, DataType)): ModelDeclaration =
        apply(PropertyDeclaration(declaration._1, declaration._2))
    
    /**
      * Creates a new declaration that consists of all optional properties
      * @param first The first property as a name data type -pair
      * @param more More similar properties
      * @return A new declaration
      */
    def optional(first: (String, DataType), more: (String, DataType)*) =
        apply((first +: more).map { case (name, dt) => PropertyDeclaration.optional(name, dt) })
    /**
      * Creates a new declaration that consists of properties with default values
      * @param first First property name, default value -pair
      * @param more More similar properties
      * @return A new declaration
      */
    def defaults(first: (String, Value), more: (String, Value)*) =
        apply((first +: more).map { case (name, default) => PropertyDeclaration.withDefault(name, default) })
}

/**
 * Used to access a number of property declarations
 * @author Mikko Hilpinen
 * @since 11.12.2016
 */
case class ModelDeclaration private(declarations: Set[PropertyDeclaration],
                                    childDeclarations: Map[String, ModelDeclaration])
    extends ModelConvertible
{
    // COMP. PROPERTIES    ----
    
    /**
     * The names of the properties declared in this declaration
     */
    def propertyNames = declarations.map { _.name } ++ childDeclarations.keySet
    
    /**
      * @return This model declaration as a constant property factory.
      *         Utilizes information about property types, default values and alternative property names.
      */
    def toConstantFactory = toPropertyFactory(Constant)
    /**
      * @return This model declaration as a variable factory.
      *         Utilizes information about property types, default values and alternative property names.
      */
    def toVariableFactory = toPropertyFactory { Variable(_, _) }
    
    
    // IMPLEMENTED  -----------
    
    override def toModel: Model =
        Model.withConstants(declarations.toVector.sortBy { _.name }.map { _.toConstant } ++
            childDeclarations.map { case (childName, child) => Constant(childName, child.toModel) })
    
    
    // OPERATORS    -----------
    
    /**
     * Creates a new declaration with the provided declaration included
     */
    def +(declaration: PropertyDeclaration) = copy(declarations = declarations + declaration)
    /**
     * @param child Child name - child declaration -pair
     * @return A copy of this declaration with the specified child declaration included
     */
    def +(child: (String, ModelDeclaration)) = copy(childDeclarations = childDeclarations + child)
    /**
     * Creates a new declaration with the provided declarations included
     */
    def ++(declarations: IterableOnce[PropertyDeclaration]) =
        copy(declarations = this.declarations ++ declarations)
    /**
     * Creates a new declaration that also contains the declarations of the other declaration
     */
    def ++(other: ModelDeclaration): ModelDeclaration =
        new ModelDeclaration(declarations ++ other.declarations, childDeclarations ++ other.childDeclarations)
    /**
     * Creates a new declaration without the provided property declaration
     */
    def -(declaration: PropertyDeclaration) = copy(declarations = declarations - declaration)
    /**
      * @param declared A declared property or child name
      * @return A copy of this declaration without that property or child
      */
    def -(declared: String) = copy(declarations = declarations.filterNot { _.name ~== declared },
        childDeclarations = childDeclarations.filterNot { _._1 ~== declared })
    /**
     * Creates a new declaration without any of the provided declarations
     */
    def --(declarations: IterableOnce[PropertyDeclaration]) = copy(declarations = this.declarations -- declarations)
    /**
     * Creates a new declaration without any declarations from the provided model
     */
    def --(other: ModelDeclaration): ModelDeclaration =
        new ModelDeclaration(declarations -- other.declarations, childDeclarations -- other.childDeclarations.keys)
    
    
    // OTHER METHODS    -------
    
    /**
     * @param childName Name of the new child
     * @param childDeclaration A declaration for that child
     * @return A copy of this declaration including the specified child model declaration
     */
    def withChild(childName: String, childDeclaration: ModelDeclaration) =
        copy(childDeclarations = childDeclarations + (childName -> childDeclaration))
    
    /**
     * Finds a property declaration with the provided name, if one exists
     * @param propertyName The name of the property
     * @return the declaration for the property, if one exists
     */
    def find(propertyName: String) = {
        val lowerName = propertyName.toLowerCase
        declarations.find { _.names.exists { _.toLowerCase == lowerName } }
    }
    /**
     * @param childName Name of the child property (case-sensitive)
     * @return A child model declaration for that name, if one exists
     */
    def findChild(childName: String) = childDeclarations.get(childName)
    
    /**
     * Finds a property declaration with the provided name or fails
     * @param propertyName The name of the property
     * @return The declaration for the property
     * @throws NoSuchElementException if there is no such declaration
     */
    @throws(classOf[NoSuchElementException])
    def get(propertyName: String) = find(propertyName)
        .getOrElse(throw new NoSuchElementException(s"No property named '$propertyName' declared"))
    /**
     * Finds a child declaration with the provided name or throws
     * @param childName Name of the targeted child (case-sensitive)
     * @throws NoSuchElementException If no such child has been declared
     * @return Child model declaration for the specified name
     */
    @throws(classOf[NoSuchElementException])
    def child(childName: String) = findChild(childName)
        .getOrElse { throw new NoSuchElementException(s"No child named '$childName' has been declared") }
    
    /**
      * Returns whether this declaration declares a property with the specified name (case-insensitive)
      * @param propertyName Property name
      */
    def contains(propertyName: String) = {
        val lowerName = propertyName.toLowerCase
        declarations.exists { _.names.exists { _.toLowerCase == lowerName } }
    }
    /**
     * @param childName Name of the targeted child (case-sensitive)
     * @return Whether this declaration contains a child declaration for that name
     */
    def containsChild(childName: String) = childDeclarations.contains(childName)
    
    /**
      * Tests whether specified model probably matches this declaration. Will not test for data type integrity, only
      * the existence of required properties.
      * @param model Model being tested
      * @return Whether the model is likely to be valid
      */
    def isProbablyValid(model: template.ModelLike[Property]) =
    {
        // Checks whether there are any missing or empty required properties
        declarations.filterNot { _.hasDefault }.forall { declaration => model(declaration.names).isDefined } &&
            childDeclarations.keys.forall { childName => model.containsNonEmpty(childName) }
    }
    
    /**
      * Checks the provided model whether all declared (non-default) properties have non-empty values and can be casted
      * to declared type
      * @param model Model to be validated
      * @return Validation results that either contain the modified model or a reason for validation failure (either
      *         missing properties or failed casting)
      */
    def validate(model: template.ModelLike[Property]): ModelValidationResult = {
        // First checks for missing attributes
        val missing = declarations.filter { d => d.names.forNone(model.containsNonEmpty) }
        val (missingNonDefaults, missingDefaults) = missing.divideBy { d => d.hasDefault || d.isOptional }.toTuple
        
        // Declarations with default values are replaced with their defaults
        if (missingNonDefaults.nonEmpty)
            ModelValidationResult.missing(model, missingNonDefaults)
        else {
            // Tries to convert all declared model properties to required types
            // and checks that each declared (non-default) property has been defined
            val keepBuilder = new VectorBuilder[Constant]()
            // Contains also whether the property is optional
            val castBuilder = new VectorBuilder[(Constant, Boolean)]()
            val castFailedBuilder = new VectorBuilder[(Constant, DataType)]()
    
            model.nonEmptyProperties.foreach { att =>
                find(att.name) match {
                    case Some(declaration) =>
                        att.value.castTo(declaration.dataType) match {
                            // Case: Casting succeeded
                            case Some(castValue) =>
                                castBuilder += (Constant(declaration.name, castValue) -> declaration.isOptional)
                            // Case: Casting failed
                            case None =>
                                castFailedBuilder += (Constant(declaration.name, att.value) -> declaration.dataType)
                        }
                    // Case: No declaration for this property => keeps it (unless it is later used for a child)
                    case None =>
                        if (childDeclarations.keys.forall { _ !~== att.name })
                            keepBuilder += Constant(att.name, att.value)
                }
            }
            
            // If all values could be cast, proceeds to create the model, otherwise fails
            val castFailed = castFailedBuilder.result()
            if (castFailed.isEmpty)
            {
                // Makes sure all required values have a non-empty value associated with them
                // (works for strings, models and vectors)
                val castValues = castBuilder.result()
                val emptyValues = castValues.filter { case (c, optional) => !optional && valueIsEmpty(c.value) }
                
                if (emptyValues.nonEmpty)
                    ModelValidationResult.missing(model,
                        declarations.filter { d =>
                            emptyValues.exists { case (c, optional) => !optional && d.names.exists { _ ~== c.name } }
                        })
                else
                {
                    val resultConstants = keepBuilder.result() ++ castValues.map { _._1 } ++
                        missingDefaults.map { d => Constant(d.name, d.defaultValue) }
                    
                    // Also validates all declared children
                    val missingChildren = childDeclarations
                        .filterNot { case (childName, _) => model.containsNonEmpty(childName) }
                    if (missingChildren.nonEmpty)
                        ModelValidationResult.missingChildren(model, missingChildren)
                    else
                    {
                        val childResults = childDeclarations.iterator.map { case (childName, childDeclaration) =>
                            val childProp = model.get(childName)
                            val result = childProp.value.model match {
                                case Some(childModel) => childDeclaration.validate(childModel)
                                case None => ModelValidationResult.castFailed(model,
                                    Set(immutable.Constant(childProp.name, childProp.value) -> ModelType))
                            }
                            childName -> result
                        }.collectTo { _._2.isFailure }
                        childResults.lastOption.filter { _._2.isFailure } match {
                            case Some((_, childFailure)) => childFailure
                            case None =>
                                ModelValidationResult.success(model,
                                    Model.withConstants(resultConstants ++
                                        childResults.map { case (childName, result) =>
                                           Constant(childName, result.success.get) }))
                        }
                    }
                }
            }
            else
                ModelValidationResult.castFailed(model, castFailed.toSet)
        }
    }
    
    /**
      * Converts this model declaration into a property factory
      * @param build A function that accepts a modified property name and value and yields
      *              a property of the desired type
      * @tparam P Type of generated properties
      * @return A new property factory utilizing this declaration's information
      */
    def toPropertyFactory[P <: Property](build: (String, Value) => P) = new DeclaredPropertyFactory[P]()(build)
    /**
      * Converts this model declaration into a property factory.
      * Ignores all declared default values when generating property values.
      * @param build A function for building a property from a property name and its value
      * @tparam P Type of generated properties
      * @return A new property factory that utilizes this declaration's information,
      *         but not property default value information.
      */
    def toPropertyFactoryWithoutDefaults[P <: Property](build: (String, Value) => P) =
        new DeclaredPropertyFactory[P](disableDefaultValues = true)(build)
    
    private def valueIsEmpty(value: Value): Boolean =
    {
        if (value.isEmpty)
            true
        else
            value.dataType match {
                case StringType => value.getString.isEmpty
                case VectorType => value.getVector.forall(valueIsEmpty)
                case ModelType => value.getModel.properties.map { _.value }.forall(valueIsEmpty)
                case _ => false
            }
    }
    
    
    // NESTED   --------------------------
    
    /**
      * A property factory that utilizes information about declared properties
      * @param disableDefaultValues Whether the use of declared default values should be disabled.
      *                             If true, proposed empty values will be kept as empty, even when there's a declared
      *                             non-empty default value available.
      *                             False by default.
      * @param actualize A function that accepts a (modified) property name and value and yields a property
      * @tparam P Type of properties generated by this factory (via the specified function)
      */
    class DeclaredPropertyFactory[+P <: Property](disableDefaultValues: Boolean = false)(actualize: (String, Value) => P)
        extends PropertyFactory[P]
    {
        override def apply(propertyName: String, value: Value) = {
            // Finds the applicable property declaration
            declarations.find { _.name ~== propertyName }
                .orElse { declarations.find { _.alternativeNames.exists { _ ~== propertyName } } } match
            {
                // Case: Pre-declared property
                case Some(declaration) =>
                    val actualValue = {
                        // Passes the default value instead of empty values (this feature may be disabled)
                        if (!disableDefaultValues && value.isEmpty)
                            declaration.defaultValue
                        // Alternatively, casts the proposed value to the right data type
                        else
                            value.castTo(declaration.dataType)
                                // Uses the declared default value as a backup if casting fails (may be disabled)
                                .getOrElse { if (disableDefaultValues) value else declaration.defaultValue }
                    }
                    // Uses the "official" declared name
                    actualize(declaration.name, actualValue)
                // Case: Other property => Passes it as is
                case None => actualize(propertyName, value)
            }
        }
    }
}
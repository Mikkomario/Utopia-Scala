package utopia.flow.generic.factory

import utopia.flow.generic.factory.PropertyFactory.MappingFactory
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.mutable.{DataType, Variable}

import scala.language.implicitConversions

object PropertyFactory
{
	// TYPES    ---------------------------
	
	/**
	  * A factory that generates constants
	  */
	type ConstantFactory = PropertyFactory[Constant]
	/**
	  * A factory that generates variables
	  */
	type VariableFactory = PropertyFactory[Variable]
	
	
	// ATTRIBUTES   -----------------------
	
	/**
	  * A basic property factory that yields constants
	  */
	val forConstants = apply(Constant) { _ => false }
	/**
	  * A basic property factory that yields variables
	  */
	val forVariables = apply { Variable(_, _) } { _ => false }
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new property factory by wrapping a function
	  * @param f A function that accepts property name and proposed value (which may be empty), and yields a property
	  * @param generatesNonEmpty A function that determines whether a non-empty default value is/would be
	  *                          generated for the specified property
	  * @tparam A Type of properties created
	  * @return A new property factory that utilizes the specified functions
	  */
	def apply[A](f: (String, Value) => A)(generatesNonEmpty: String => Boolean): PropertyFactory[A] =
		new _PropertyFactory[A](f, generatesNonEmpty)
	
	/**
	  * Creates a new property factory that uses a default value instead of an empty value
	  * @param defaultValue The default value to assign when an empty value is proposed.
	  *                     Also determines the data type of the applicable property,
	  *                     to which proposed values will be cast.
	  * @param requireCastingSuccess Whether, on casting failures,
	  *                              the default value should be used instead of the original value.
	  *                              If false (which is the default state),
	  *                              the proposed value will be used in its original form when the casting fails.
	  *                              If true, the default value will be used.
	  * @param f A function that accepts the property name and the modified value and returns a complete property
	  * @tparam A Type of generated properties
	  * @return A new property factory
	  */
	def withDefault[A](defaultValue: Value, requireCastingSuccess: Boolean = false)(f: (String, Value) => A) =
	{
		// Case: Empty default => Same as no default
		if (defaultValue.isEmpty)
			apply(f) { _ => false }
		// Case: Data type conversion used
		else if (requireCastingSuccess) {
			val dataType = defaultValue.dataType
			apply { (name, value) =>
				f(name, value.notEmpty.flatMap { _.castTo(dataType) }.getOrElse(defaultValue))
			} { _ => true }
		}
		// Case: No data type conversion used
		else
			apply { (name, value) => f(name, value.nonEmptyOrElse(defaultValue)) } { _ => true }
	}
	
	/**
	  * Creates a new constant property factory that uses a default value instead of an empty value
	  * @param defaultValue The default value to assign when an empty value is proposed.
	  *                     Also determines the data type of the applicable property,
	  *                     to which proposed values will be cast.
	  * @param requireCastingSuccess Whether, on casting failures,
	  *                              the default value should be used instead of the original value.
	  *                              If false (which is the default state),
	  *                              the proposed value will be used in its original form when the casting fails.
	  *                              If true, the default value will be used.
	  * @return A new constant property factory
	  */
	def constantWithDefault(defaultValue: Value, requireCastingSuccess: Boolean = false) =
		withDefault(defaultValue, requireCastingSuccess)(Constant)
	/**
	  * Creates a new variable factory that uses a default value instead of an empty value
	  * @param defaultValue The default value to assign when an empty value is proposed.
	  *                     Also determines the data type of the applicable property,
	  *                     to which proposed values will be cast.
	  * @param requireCastingSuccess Whether, on casting failures,
	  *                              the default value should be used instead of the original value.
	  *                              If false (which is the default state),
	  *                              the proposed value will be used in its original form when the casting fails.
	  *                              If true, the default value will be used.
	  * @return A new variable factory
	  */
	def variableWithDefault(defaultValue: Value, requireCastingSuccess: Boolean = false) =
		withDefault(defaultValue, requireCastingSuccess) { Variable(_, _) }
	
	/**
	  * Creates a new property factory that, before assigning a value, casts it to a specific data type
	  * @param targetType The data type to which the proposed values will be cast before they are assigned to a property
	  * @param requireCastingSuccess Whether the type casting should be required to succeed in order for the
	  *                              proposed value to be assigned.
	  *                              If true, an empty value will be assigned on casting failures.
	  *                              If false, which is the default, the proposed value will be assigned without
	  *                              casting, if the casting fails.
	  * @param f A function that accepts the property name and the cast value and returns a complete property
	  * @tparam A Type of generated properties
	  * @return A new property factory
	  */
	def castingTo[A](targetType: DataType, requireCastingSuccess: Boolean = false)(f: (String, Value) => A) = {
		if (requireCastingSuccess)
			apply { (name, value) =>
				f(name, value.withType(targetType))
			} { _ => false }
		else
			apply { (name, value) => f(name, value.castTo(targetType).getOrElse(value)) } { _ => false }
	}
	
	/**
	  * Creates a new constant property factory that, before assigning a value, casts it to a specific data type.
	  * In situations where the casting fails, an empty value is assigned instead.
	  * @param targetType The data type to which the proposed values will be cast before they are assigned to a property
	  * @return A new constant property factory
	  */
	def constantOfType(targetType: DataType) =
		castingTo(targetType, requireCastingSuccess = true)(Constant)
	/**
	  * Creates a new variable factory that, before assigning a value, casts it to a specific data type.
	  * In situations where the casting fails, an empty value is assigned instead.
	  * @param targetType The data type to which the proposed values will be cast before they are assigned to a property
	  * @return A new variable factory
	  */
	def variableOfType(targetType: DataType) =
		castingTo(targetType, requireCastingSuccess = true) { Variable(_, _) }
	
	
	// NESTED   ------------------------
	
	private class _PropertyFactory[A](f: (String, Value) => A, nonEmptyCheck: String => Boolean)
		extends PropertyFactory[A]
	{
		override def apply(propertyName: String, value: Value) = f(propertyName, value)
		override def generatesNonEmpty(propertyName: String): Boolean = nonEmptyCheck(propertyName)
	}
	
	private class MappingFactory[-O, +R](wrapped: PropertyFactory[O], f: O => R) extends PropertyFactory[R]
	{
		override def apply(propertyName: String, value: Value) = f(wrapped(propertyName, value))
		override def generatesNonEmpty(propertyName: String): Boolean = wrapped.generatesNonEmpty(propertyName)
	}
}

/**
  * Used for generating properties for models
  */
trait PropertyFactory[+A]
{
	// ABSTRACT ------------------------
	
	/**
	  * Generates a new property
	  * @param propertyName The name for the new property
	  * @param value        The value for the new property (default = empty value)
	  * @return Generated property
	  */
	def apply(propertyName: String, value: Value = Value.empty): A
	
	/**
	  * @param propertyName A property name
	  * @return Whether this factory would produce a non-empty default value for the specified property
	  */
	def generatesNonEmpty(propertyName: String): Boolean
	
	
	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function for the generated properties
	  * @tparam B Type of the mapping results
	  * @return A factory that processes the results of this factory using the specified function
	  */
	def mapResult[B](f: A => B): PropertyFactory[B] = new MappingFactory[A, B](this, f)
}

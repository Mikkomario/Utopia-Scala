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
	val forConstants = apply(Constant)
	/**
	  * A basic property factory that yields variables
	  */
	val forVariables = apply { Variable(_, _) }
	
	
	// IMPLICIT ------------------------
	
	/**
	  * Creates a new property factory by wrapping a function
	  * @param f A function that accepts property name and proposed value (which may be empty), and yields a property
	  * @tparam A Type of properties created
	  * @return A new property factory that utilizes the specified functions
	  */
	implicit def apply[A](f: (String, Value) => A): PropertyFactory[A] = new _PropertyFactory[A](f)
	
	
	// OTHER    ------------------------
	
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
		apply { (name, value) =>
			val actualValue = value.notEmpty match {
				case Some(value) =>
					value.castTo(defaultValue.dataType).getOrElse { if (requireCastingSuccess) defaultValue else value }
				case None => defaultValue
			}
			f(name, actualValue)
		}
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
	def castingTo[A](targetType: DataType, requireCastingSuccess: Boolean = false)(f: (String, Value) => A) =
		apply { (name, value) =>
			val actualValue = value.castTo(targetType)
				.getOrElse { if (requireCastingSuccess) Value.emptyWithType(targetType) else value }
			f(name, actualValue)
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
	
	private class _PropertyFactory[A](f: (String, Value) => A) extends PropertyFactory[A]
	{
		override def apply(propertyName: String, value: Value) = f(propertyName, value)
	}
	
	private class MappingFactory[-O, +R](wrapped: PropertyFactory[O], f: O => R) extends PropertyFactory[R]
	{
		override def apply(propertyName: String, value: Value) = f(wrapped(propertyName, value))
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
	
	
	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function for the generated properties
	  * @tparam B Type of the mapping results
	  * @return A factory that processes the results of this factory using the specified function
	  */
	def mapResult[B](f: A => B): PropertyFactory[B] = new MappingFactory[A, B](this, f)
}

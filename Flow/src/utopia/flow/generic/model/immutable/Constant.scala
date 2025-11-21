package utopia.flow.generic.model.immutable

import utopia.flow.collection.immutable.{Pair, PairView}
import utopia.flow.generic.model.mutable.{DataType, Variable}
import utopia.flow.generic.model.template.{Property, ValueConvertible}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.{ApproxEquals, EqualsBy}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy

import scala.language.implicitConversions

object Constant
{
	// IMPLICIT -------------------------
	
	/**
	 * @param keyValue The name and value for this constant
	 * @return A new constant with the specified name and value
	 */
	def apply(keyValue: (String, Value)): Constant = apply(keyValue._1, keyValue._2)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param name Name of this constant
	 * @param value Value of this constant
	 * @return A new constant with the specified name & value
	 */
	def apply(name: String, value: Value): Constant = _Constant(name, value)
	
	/**
	 * @param name Name of this constant
	 * @param value A lazy container that will yield the value for this constant
	 * @return A new constant with the specified name & value
	 */
	def lazily(name: String, value: Lazy[Value]): Constant = value.current match {
		case Some(value) => apply(name, value)
		case None => new LazyConstant(name, value)
	}
	/**
	 * @param name Name of this constant
	 * @param getValue A function for acquiring the value of this constant
	 *                 (in some form convertible to a [[ValueConvertible]] instance)
	 * @param toValue Implicit value conversion for the value type.
	 *                Usually from [[utopia.flow.generic.casting.ValueConversions]].
	 * @tparam V Type of the value yielded by 'getValue'
	 * @return A constant where the value is initialized lazily
	 */
	def lazily[V](name: String)(getValue: => V)(implicit toValue: V => ValueConvertible): Constant =
		new LazyConstant(name, Lazy { toValue(getValue).toValue })
	
	/**
	 * @param property A property
	 * @return A constant with the same name & (current) value as that property
	 */
	def from(property: Property) = property match {
		case c: Constant => c
		case p => apply(p.name, p.value)
	}
	
	
	// NESTED   -------------------------
	
	private class LazyConstant(override val name: String, lazyValue: Lazy[Value]) extends Constant
	{
		// IMPLEMENTED  -----------------
		
		override def value: Value = lazyValue.value
		
		override protected def equalsProperties: IterableOnce[Any] = PairView(name, value)
		
		override def withName(name: String): Constant = lazyValue.current match {
			case Some(value) => Constant(name, value)
			case None => new LazyConstant(name, lazyValue)
		}
		override def mapValue(f: Value => Value): Constant = lazyValue.current match {
			case Some(value) => Constant(name, f(value))
			case None => new LazyConstant(name, lazyValue.lightMap(f))
		}
	}
	
	private case class _Constant(name: String, value: Value) extends Constant
}

/**
  * Constants are named properties whose value can't be changed
  * @author Mikko Hilpinen
  * @since 29.11.2016
  */
trait Constant extends Property with EqualsBy with ApproxEquals[Property]
{
	// COMPUTED    -----------------
	
	/**
	  * Converts this constant to a variable
	  * @param log Implicit logging implementation for handling failures in change event -handling
	  */
	def toVariable(implicit log: Logger) = Variable(name, value)
	
	
	// IMPLEMENTED  ----------------
	
	override def dataType: DataType = value.dataType
	
	override protected def equalsProperties: IterableOnce[Any] = Pair(name, value)
	
	override def ~==(other: Property): Boolean = (name ~== other.name) && (value ~== other.value)
	
	
	// OTHER METHODS    ------------
	
	/**
	  * Creates a new constant that has the provided value but the same name
	  * @param value the value the new constant will have
	  */
	def withValue(value: Value) = Constant(name, value)
	/**
	  * @param name New name for constant
	  * @return A copy of this constant with provided name
	  */
	def withName(name: String) = Constant(name, value)
	
	/**
	  * @param f A mapping function for this constant's value
	  * @return A mapped copy of this constant
	  */
	def mapValue(f: Value => Value) = withValue(f(value))
	/**
	  * @param f A mapping function for this constant's name
	  * @return A copy of this constant with a mapped name
	  */
	def mapName(f: String => String) = withName(f(name))
}

package utopia.flow.generic.model.immutable

import utopia.flow.generic.model.mutable.Variable
import utopia.flow.generic.model.template.Property
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.ApproxEquals
import utopia.flow.util.logging.Logger

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
	 * @param property A property
	 * @return A constant with the same name & (current) value as that property
	 */
	def from(property: Property) = property match {
		case c: Constant => c
		case p => apply(p.name, p.value)
	}
}

/**
  * Constants are named properties whose value can't be changed
  * @author Mikko Hilpinen
  * @since 29.11.2016
  */
case class Constant(name: String, value: Value) extends Property with ApproxEquals[Property]
{
	// COMP. PROPERTIES    ---------
	
	override val dataType = value.dataType
	
	
	// COMPUTED    -----------------
	
	/**
	  * Converts this constant to a variable
	  * @param log Implicit logging implementation for handling failures in change event -handling
	  */
	def toVariable(implicit log: Logger) = Variable(name, value)
	
	
	// IMPLEMENTED  ----------------
	
	override def ~==(other: Property): Boolean = (name ~== other.name) && (value ~== other.value)
	
	
	// OTHER METHODS    ------------
	
	/**
	  * Creates a new constant that has the provided value but the same name
	  * @param value the value the new constant will have
	  */
	def withValue(value: Value) = copy(value = value)
	/**
	  * @param name New name for constant
	  * @return A copy of this constant with provided name
	  */
	def withName(name: String) = copy(name = name)
	
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

package utopia.flow.generic.model.immutable

import utopia.flow.generic.model.mutable.Variable
import utopia.flow.generic.model.template.Property
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.ApproxEquals

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
	  */
	def toVariable = Variable(name, value)
	
	
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

package utopia.flow.operator.equality

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsBy.hashCodeFrom

object EqualsBy
{
	def hashCodeFrom(first: Any, second: Any, more: Any*): Int = hashCodeFrom(Pair(first, second) ++ more)
	/**
	 * @param properties A set of properties
	 * @return A hashcode constructed by combining the hashcodes of the specified properties
	 */
	def hashCodeFrom(properties: Iterable[Any]) =
		properties.foldLeft(1) { (result, property) => 31 * result + property.hashCode() }
}

/**
  * This is an utility interface into the Equals -trait, providing a sample implementation.
  * This trait should only be implemented by immutable elements that have value semantics
  * @author Mikko Hilpinen
  * @since 6.11.2016
  */
trait EqualsBy extends Equals
{
	// ATTRIBUTES   ----------------------
	
	private lazy val _hashCode = hashCodeFrom(equalsProperties)
	
	
	// COMPUTED PROPS    -----------------
	
	/**
	  * The properties that define whether this instance equals with another instance. Two instances
	  * of same class, which also have equal properties are considered equal.
	  * The ordering of the properties must also match.
	  */
	protected def equalsProperties: Seq[Any]
	
	
	// IMPLEMENTED METHODS    ------------
	
	override def hashCode() = _hashCode
	
	override def canEqual(a: Any) = getClass.isInstance(a)
	
	// Default implementation is: canEqual(a) && hashCode() == a.hashCode()
	override def equals(a: Any) = a match {
		case eb: EqualsBy =>
			val props1 = equalsProperties
			val props2 = eb.equalsProperties
			val s1 = props1.knownSize
			val s2 = props2.knownSize
			if (s1 != s2 && s1 >= 0 && s2 >= 0)
				false
			else {
				val iter1 = props1.iterator
				val iter2 = props2.iterator
				var equal = true
				while (equal && iter1.hasNext && iter2.hasNext) {
					if (iter1.next() != iter2.next())
						equal = false
				}
				equal && !iter1.hasNext && !iter2.hasNext
			}
		case _ => false
	}
}

package utopia.flow.operator

/**
  * This is an utility interface into the Equals -trait, providing a sample implementation.
  * This trait should only be implemented by immutable elements that have value semantics
  * @author Mikko Hilpinen
  * @since 6.11.2016
  */
trait Equatable extends Equals
{
	// COMPUTED PROPS    -----------------
	
	/**
	  * The properties that define whether this instance equals with another instance. Two instances
	  * of same class, which also have equal properties are considered equal
	  */
	def properties: IterableOnce[Any]
	
	
	// IMPLEMENTED METHODS    ------------
	
	override def canEqual(a: Any) = this.getClass.isInstance(a)
	
	override def hashCode() = properties.iterator.foldLeft(1)((result, property) => 31 * result + property.hashCode())
	
	override def equals(a: Any) = canEqual(a) && this.hashCode() == a.hashCode()
}

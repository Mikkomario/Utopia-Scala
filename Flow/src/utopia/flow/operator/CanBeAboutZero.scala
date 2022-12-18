package utopia.flow.operator

/**
  * A common trait for items which may be zero and which support approximate comparisons
  * @author Mikko Hilpinen
  * @since 8.8.2022, v1.16
  * @tparam A Type of item to which this item may be compared
  * @tparam Repr actual representation type of this item
  */
trait CanBeAboutZero[-A, +Repr] extends Any with ApproxEquals[A] with CanBeZero[Repr]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Whether this item is very close to zero
	  */
	def isAboutZero: Boolean
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return Whether this item is not very close to zero
	  */
	def isNotAboutZero = !isAboutZero
	
	
	// OTHER    ---------------------
	
	/**
	  * @return None if about zero, Some(this) otherwise
	  */
	def notCloseZero = if (isAboutZero) None else Some(self)
}

package utopia.flow.operator

/**
  * A common trait for items for which it is possible to recognize whether it is zero or not
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait LengthLike[+Repr] extends Any
{
	// ABSTRACT --------------------------
	
	/**
	  * @return 'This' item
	  */
	def repr: Repr
	
	/**
	  * @return Whether this item is of zero length
	  */
	def isZero: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Whether this item is not of zero length
	  */
	def nonZero = !isZero
	
	/**
	  * @return This item if not zero. None otherwise.
	  */
	def notZero = if (isZero) None else Some(repr)
}

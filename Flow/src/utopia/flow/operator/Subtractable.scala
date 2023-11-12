package utopia.flow.operator

/**
  * Common trait for classes that support subtraction
  * @author Mikko Hilpinen
  * @since 12.11.2023, v2.3
  * @tparam A Type of items that may be subtracted from this item
  * @tparam R Type of subtraction result
  */
trait Subtractable[-A, +R] extends Any
{
	// ABSTRACT ----------------------
	
	/**
	  * @param other An item to subtract from this one
	  * @return Subtraction result
	  */
	def -(other: A): R
}

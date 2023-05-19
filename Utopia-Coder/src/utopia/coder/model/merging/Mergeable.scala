package utopia.coder.model.merging

/**
  * Common trait for items which can be merged with others
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  * @tparam I Type of merge input
  * @tparam O Type of merge output
  */
trait Mergeable[-I, +O]
{
	/**
	  * @param other Another item
	  * @return Whether this item matches the other item.
	  *         I.e. whether these two items should be merged together when different.
	  */
	def matches(other: I): Boolean
	
	/**
	  * Merges this item with another item
	  * @param other Another item
	  * @return Merged item + possible merge conflicts
	  */
	def mergeWith(other: I): (O, Vector[MergeConflict])
}

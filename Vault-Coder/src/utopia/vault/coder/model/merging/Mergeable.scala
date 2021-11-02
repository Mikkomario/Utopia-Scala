package utopia.vault.coder.model.merging

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
	  * Merges this item with another item
	  * @param other Another item
	  * @return Merged item + possible merge conflicts
	  */
	def mergeWith(other: I): (O, Vector[MergeConflict])
}

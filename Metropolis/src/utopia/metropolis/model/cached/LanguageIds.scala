package utopia.metropolis.model.cached

import utopia.flow.util.Extender

object LanguageIds
{
	/**
	  * @param firstId Id of the most preferred language
	  * @param moreIds Ids of the other languages from most preferred to least preferred
	  * @return A language id list
	  */
	def apply(firstId: Int, moreIds: Int*): LanguageIds = apply(firstId +: moreIds.toVector)
}

/**
  * Represents a prioritized list of languages (based on their ids). The wrapped languages are from most preferred to
  * least preferred.
  * @author Mikko Hilpinen
  * @since 15.10.2021, v1.3
  */
case class LanguageIds(wrapped: Vector[Int]) extends Extender[Vector[Int]]
{
	/**
	  * @return Id of the most preferred language in this list
	  * @throws IllegalStateException If this list is empty
	  */
	@throws[IllegalStateException]
	def mostPreferred = wrapped.head
	
	/**
	  * @return A language id list with the first id removed
	  * @throws IllegalStateException If this list is empty
	  */
	@throws[IllegalStateException]
	def tail = LanguageIds(wrapped.tail)
}

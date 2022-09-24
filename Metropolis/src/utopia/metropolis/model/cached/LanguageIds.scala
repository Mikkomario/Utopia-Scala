package utopia.metropolis.model.cached

import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.Extender

object LanguageIds
{
	/**
	  * @param ids Language ids to wrap (call-by-name)
	  * @return A language ids instance based on those ids (lazy)
	  */
	def apply(ids: => Vector[Int]) = new LanguageIds(Lazy(ids))
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
class LanguageIds private(ids: Lazy[Vector[Int]]) extends Extender[Vector[Int]]
{
	// COMPUTED ------------------------------
	
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
	def tail = new LanguageIds(Lazy { wrapped.tail })
	
	
	// IMPLEMENTED  -------------------------
	
	override def wrapped = ids.value
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param languageId A language id
	 * @return A copy of this language id list with that language as the one most preferred
	 */
	def preferringLanguageWithId(languageId: Int) =
		new LanguageIds(Lazy { languageId +: wrapped.filter { _ != languageId } })
}

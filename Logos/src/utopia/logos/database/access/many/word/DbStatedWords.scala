package utopia.logos.database.access.many.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple stated words at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
object DbStatedWords extends ManyStatedWordsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted stated words
	  * @return An access point to stated words with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbStatedWordsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbStatedWordsSubset(targetIds: Set[Int]) extends ManyStatedWordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}


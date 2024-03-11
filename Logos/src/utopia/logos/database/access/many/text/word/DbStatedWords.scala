package utopia.logos.database.access.many.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple stated words at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
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


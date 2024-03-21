package utopia.logos.database.access.many.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.word.WordModel
import utopia.logos.model.partial.word.WordData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple words at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DbWords extends ManyWordsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted words
	  * @return An access point to words with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbWordsSubset(ids)
	
	/**
	  * Stores the specified words to the database. Avoids inserting duplicate entries.
	  * @param words Words to insert to the database, if not present already
	  * @param connection Implicit DB Connection
	  * @return Map where keys are the specified words and the values are ids matching those words
	  */
	def store(words: Set[String])(implicit connection: Connection) = {
		// Case: No words to store => Returns an empty map
		if (words.isEmpty)
			Map[String, Int]()
		else {
			// Finds existing word entries
			val existingWordIds = matching(words).toMap
			// Inserts the missing entries, if applicable
			val newWords = words -- existingWordIds.keySet
			if (newWords.isEmpty)
				existingWordIds
			else
				existingWordIds ++ 
					WordModel.insert(newWords.toVector.map { WordData(_) }).map { w => w.text -> w.id }
		}
	}
	
	
	// NESTED	--------------------
	
	class DbWordsSubset(targetIds: Set[Int]) extends ManyWordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}


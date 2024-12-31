package utopia.logos.database.access.many.text.word

import utopia.flow.collection.CollectionExtensions._
import utopia.logos.database.storable.text.WordDbModel
import utopia.logos.model.partial.text.WordData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple words at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbWords extends ManyWordsAccess with UnconditionalView with ViewManyByIntIds[ManyWordsAccess]
{
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
			val existingWordIds = matchingWords(words).toMap
			// Inserts the missing entries, if applicable
			val newWords = words -- existingWordIds.keySet
			if (newWords.isEmpty)
				existingWordIds
			else
				existingWordIds ++
					WordDbModel.insert(newWords.view.map { WordData(_) }.toOptimizedSeq).map { w => w.text -> w.id }
		}
	}
}
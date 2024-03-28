package utopia.logos.database.access.many.word.statement

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.many.url.link.DbLinks
import utopia.logos.database.access.many.word.DbWords
import utopia.logos.database.access.many.word.delimiter.DbDelimiters
import utopia.logos.database.access.single.word.statement.DbStatement
import utopia.logos.model.cached.StatementText
import utopia.logos.model.stored.word.Statement
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple statements at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DbStatements extends ManyStatementsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted statements
	  * @return An access point to statements with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbStatementsSubset(ids)
	
	/**
	  * Stores the specified text to the database as a sequence of statements.
	  * Avoids inserting duplicate entries.
	  * @param text Text to store as statements
	  * @param connection Implicit DB connection
	  * @return Stored statements, where each entry is either right, if it existed already, or left,
	  * if it was newly inserted
	  */
	def store(text: String)(implicit connection: Connection): Seq[Sided[Statement]] = store(StatementText.allFrom(text))
	/**
	 * Stores the specified text to the database as a sequence of statements.
	 * Avoids inserting duplicate entries.
	 * @param statementData Statement texts to store
	 * @param connection Implicit DB connection
	 * @return Stored statements, where each entry is either right, if it existed already, or left,
	 * if it was newly inserted
	 */
	def store(statementData: Seq[StatementText])(implicit connection: Connection) = {
		// Stores the delimiters first
		val delimiterMap = DbDelimiters.store(statementData.map { _.delimiter }.toSet.filterNot { _.isEmpty })
		
		// Next stores the words, the links and the statements
		// First element is words, second is links
		val wordsAndLinks = Pair.tupleToPair(statementData.splitFlatMap { _.wordsAndLinks }).map { _.toSet }
		val wordMap = DbWords.store(wordsAndLinks.first)
		val linkMap = DbLinks.store(wordsAndLinks.second)
			.merge { _ ++ _ }.map { l => l.toString.toLowerCase -> l.id }.toMap
		
		statementData.map { statement =>
			val wordIds = statement.words.flatMap { word =>
				val result = if (word.isLink) linkMap.get(word.text.toLowerCase) else wordMap.get(word.text)
				if (result.isEmpty)
					println(s"Warning: Failed to match $word with options: ${
						(if (word.isLink) linkMap else wordMap).keys.mkString(", ")}")
				result.map { _ -> word.isLink }
			}
			DbStatement.store(wordIds, delimiterMap.get(statement.delimiter))
		}
	}
	
	
	// NESTED	--------------------
	
	class DbStatementsSubset(targetIds: Set[Int]) extends ManyStatementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}


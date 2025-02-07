package utopia.logos.database.access.many.text.statement

import com.vdurmont.emoji.EmojiParser
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.NotEmpty
import utopia.logos.database.access.many.text.delimiter.DbDelimiters
import utopia.logos.database.access.many.text.word.DbWords
import utopia.logos.database.access.many.text.word.placement.DbWordPlacements
import utopia.logos.database.access.many.url.link.DbLinks
import utopia.logos.database.access.many.url.link.placement.DbLinkPlacements
import utopia.logos.database.access.single.text.statement.DbStatement
import utopia.logos.model.cached.{PreparedWordOrLinkPlacement, Statement}
import utopia.logos.model.combined.text.{DetailedStatement, StatedWord}
import utopia.logos.model.combined.url.{DetailedLink, DetailedLinkPlacement}
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple statements at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbStatements 
	extends ManyStatementsAccess with UnconditionalView with ViewManyByIntIds[ManyStatementsAccess]
{
	/**
	 * Attaches textual details to a set of statements
	 * @param statements Statements to enhance
	 * @param connection Implicit DB connection
	 * @return Detailed copies of the specified statements
	 */
	def attachDetailsTo(statements: Seq[StoredStatement])(implicit connection: Connection) = {
		if (statements.isEmpty)
			Empty
		else {
			// Pulls words involved
			val statementIds = statements.view.map { _.id }.toIntSet
			val wordPlacements = DbWordPlacements.withinStatements(statementIds).pull
			val wordMap = DbWords(wordPlacements.view.map { _.wordId }.toIntSet).toMapBy { _.id }
			val detailedWordPlacementsPerStatementId = wordPlacements
				.map { p => StatedWord(wordMap(p.wordId), p) }
				.groupBy { _.useCase.statementId }.withDefaultValue(Empty)
			
			// Pulls links involved
			val linkPlacements = DbLinkPlacements.withinStatements(statementIds).pull
			val linkMap = NotEmpty(linkPlacements) match {
				case Some(placements) =>
					DbLinks(placements.view.map { _.linkId }.toIntSet).pullDetailed.view.map { l => l.id -> l }.toMap
				case None => Map[Int, DetailedLink]()
			}
			val detailedLinkPlacementsPerStatementId = linkPlacements
				.map { p => DetailedLinkPlacement(p, linkMap(p.linkId)) }
				.groupBy { _.statementId }.withDefaultValue(Empty)
			
			// Pulls all statements and delimiters involved
			val delimiterMap = DbDelimiters(statements.view.flatMap { _.delimiterId }.toIntSet).toMapBy { _.id }
			
			// Combines the information together
			statements.map { s =>
				DetailedStatement(s,
					detailedWordPlacementsPerStatementId(s.id),
					detailedLinkPlacementsPerStatementId(s.id),
					s.delimiterId.flatMap(delimiterMap.get)
				)
			}
		}
	}
	
	/**
	 * Stores n texts, attaching the stored statements back to these instances
	 * @param texts Texts to store
	 * @param extractText A function that extracts the text portion from the stored values
	 * @param connection Implicit DB connection
	 * @tparam O Type of stored texts
	 * @tparam R Type of the resulting / combined models
	 * @return Copy of the 'texts' sequence, where each item is accompanied by the statements matching that text
	 */
	def storeFrom[O, R](texts: Seq[O])(extractText: O => String)(mergeBack: (Seq[StoredStatement], O) => R)
	                   (implicit connection: Connection) =
	{
		val groupedStatements = texts.map { text =>
			text -> Statement.allFrom(EmojiParser.parseToAliases(extractText(text)))
		}
		// Note: Here assumes that stored statements count matches the input
		val storedStatementsIter = store(groupedStatements.flatMap { _._2 }).iterator.map { _.either }
		groupedStatements.map { case (text, preparedStatements) =>
			mergeBack(storedStatementsIter.collectNext(preparedStatements.size), text)
		}
	}
	/**
	  * Stores the specified text to the database as a sequence of statements.
	  * Avoids inserting duplicate entries.
	  * @param text Text to store as statements
	  * @param connection Implicit DB connection
	  * @return Stored statements, where each entry is either right, if it existed already, or left,
	  * if it was newly inserted
	  */
	def store(text: String)(implicit connection: Connection): Seq[Sided[StoredStatement]] =
		store(Statement.allFrom(EmojiParser.parseToAliases(text)))
	/**
	  * Stores the specified text to the database as a sequence of statements.
	  * Avoids inserting duplicate entries.
	  * @param statementData Statement texts to store
	  * @param connection Implicit DB connection
	  * @return Stored statements, where each entry is either right,
	 *         if it existed already, or left, if it was newly inserted.
	 *         There are as many returned statements as there are entries in 'statementData'
	  */
	def store(statementData: Seq[Statement])(implicit connection: Connection) = {
		// Stores the delimiters first
		val delimiterMap = DbDelimiters.store(statementData.map { _.delimiter }.toSet.filterNot { _.isEmpty })
		
		// Next stores the words, the links and the statements
		// First element is words, second is links
		val wordMap = DbWords.store(statementData.view.flatMap { _.standardizedWords.map { _._1 } }.toSet)
		val linkMap = DbLinks.store(statementData.view.flatMap { _.links }.toSet)
			.merge { _ ++ _ }.map { l => l.toString.toLowerCase -> l.id }.toMap
		
		statementData.map { statement =>
			val words = statement.words.flatMap { word =>
				val result = {
					if (word.isLink)
						linkMap.get(word.text.toLowerCase).map(PreparedWordOrLinkPlacement.link)
					else
						wordMap.get(word.standardizedText).map { PreparedWordOrLinkPlacement(_, word.style) }
				}
				if (result.isEmpty)
					println(s"Warning: Failed to match $word with options: ${
						(if (word.isLink) linkMap else wordMap).keys.mkString(", ")}")
				result
			}
			DbStatement.store(words, delimiterMap.get(statement.delimiter))
		}
	}
}
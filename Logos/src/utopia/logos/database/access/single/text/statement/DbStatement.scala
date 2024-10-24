package utopia.logos.database.access.single.text.statement

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.NotEmpty
import utopia.logos.database.access.many.text.statement.DbStatements
import utopia.logos.database.access.many.text.word.placement.DbWordPlacements
import utopia.logos.database.access.many.url.link.placement.DbLinkPlacements
import utopia.logos.database.factory.text.StatementDbFactory
import utopia.logos.database.storable.text.{StatementDbModel, WordPlacementDbModel}
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.partial.text.{StatementData, WordPlacementData}
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual statements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbStatement extends SingleRowModelAccess[StoredStatement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	private def model = StatementDbModel
	
	/**
	  * Model used for interacting with statement-word links
	  */
	private def wordLinkModel = WordPlacementDbModel
	/**
	  * Model used for interacting with statement-link links
	  */
	private def linkLinkModel = LinkPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatementDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted statement
	  * @return An access point to that statement
	  */
	def apply(id: Int) = DbSingleStatement(id)
	
	/**
	  * Stores a statement to the database. Avoids inserting duplicate entries.
	  * @param wordIds Ids of the words that form this statement
	  * @param delimiterId Id of the delimiter that ends this statement.
	  * None if this statement doesn't end with a delimiter.
	  * @param connection Implicit DB connection
	  * @return Existing (right) or inserted (left) statement
	  */
	def store(wordIds: Seq[(Int, Boolean)], delimiterId: Option[Int])(implicit connection: Connection) = {
		// Case: Empty statement => Pulls or inserts
		if (wordIds.isEmpty)
			DbStatements.endingWith(delimiterId).pullEmpty.headOption
				.toRight { model.insert(StatementData(delimiterId)) }
		// Case: Non-empty statement => Finds potential matches
		else {
			val (firstWordId, firstWordIsLink) = wordIds.head
			val initialMatchIds = DbStatements.endingWith(delimiterId)
				.findStartingWith(firstWordId, isLink = firstWordIsLink)
				.map { _.id }.toSet
			// Reduces the number of potential matches by including more words
			val remainingMatchIds = wordIds.zipWithIndex.tail
				.foldLeft(initialMatchIds) { case (potentialStatementIds, ((wordId, isLink), wordIndex)) =>
					if (potentialStatementIds.isEmpty)
						potentialStatementIds
					else if (isLink)
						DbLinkPlacements
							.withinStatements(potentialStatementIds).placingLink(wordId).at(wordIndex)
							.statementIds.toSet
					else
						DbWordPlacements.withinStatements(potentialStatementIds)
							.placingWordAtPosition(wordId, wordIndex)
							.statementIds.toSet
				}
			NotEmpty(remainingMatchIds)
				// Only accepts statements of specific length
				.flatMap { remaining => DbStatements(remaining).findShorterThan(wordIds.size + 1).headOption }
				// If no such statement exists, inserts it
				.toRight {
					val statement = model.insert(StatementData(delimiterId))
					val (linkData, wordData) = wordIds.zipWithIndex.divideWith { case ((wordId, isLink),
					index) =>
						if (isLink)
							Left(LinkPlacementData(statement.id, wordId, index))
						else
							Right(WordPlacementData(statement.id, wordId, index))
					}
					linkLinkModel.insert(linkData)
					wordLinkModel.insert(wordData)
					statement
				}
		}
	}
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique statements.
	  * @return An access point to the statement that satisfies the specified condition
	  */
	private def distinct(condition: Condition) = UniqueStatementAccess(condition)
}


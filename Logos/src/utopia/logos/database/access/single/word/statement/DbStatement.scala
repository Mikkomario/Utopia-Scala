package utopia.logos.database.access.single.word.statement

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.NotEmpty
import utopia.logos.database.access.many.url.link_placement.DbLinkPlacements
import utopia.logos.database.access.many.word.placement.DbWordPlacements
import utopia.logos.database.access.many.word.statement.DbStatements
import utopia.logos.database.factory.word.StatementDbFactory
import utopia.logos.database.storable.url.LinkPlacementModel
import utopia.logos.database.storable.word.{StatementModel, WordPlacementModel}
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.partial.word.{StatementData, WordPlacementData}
import utopia.logos.model.stored.word.Statement
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual statements
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DbStatement extends SingleRowModelAccess[Statement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StatementModel
	
	/**
	  * Model used for interacting with statement-word links
	  */
	protected def wordLinkModel = WordPlacementModel
	
	/**
	  * Model used for interacting with statement-link links
	  */
	protected def linkLinkModel = LinkPlacementModel
	
	
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
							.inStatements(potentialStatementIds).ofLink(wordId).atPosition(wordIndex)
							.statementIds.toSet
					else
						DbWordPlacements.inStatements(potentialStatementIds).ofWordAtPosition(wordId, 
							wordIndex)
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
	protected def filterDistinct(condition: Condition) = UniqueStatementAccess(mergeCondition(condition))
}


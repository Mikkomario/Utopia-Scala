package utopia.logos.database.access.many.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.text.StatementFactory
import utopia.logos.database.model.text.{StatementLinkedModel, WordPlacementModel}
import utopia.logos.database.model.url.LinkPlacementModel
import utopia.logos.model.stored.text.Statement
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

object ManyStatementsAccess
{
	// NESTED	--------------------
	
	private class ManyStatementsSubView(condition: Condition) extends ManyStatementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple statements at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait ManyStatementsAccess 
	extends ManyStatementsAccessLike[Statement, ManyStatementsAccess] with ManyRowModelAccess[Statement] 
		with ChronoRowFactoryView[Statement, ManyStatementsAccess]
{
	// COMPUTED ------------------------
	
	/**
	 * @return Model used for interacting with statement-word links
	 */
	protected def wordLinkModel = WordPlacementModel
	/**
	 * @return Model used for interacting with statement-link links
	 */
	protected def linkLinkModel = LinkPlacementModel
	
	/**
	 * @param connection Implicit DB Connection
	 * @return Accessible empty statements (i.e. statements without any words)
	 */
	def pullEmpty(implicit connection: Connection) = findShorterThan(1)
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatementFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyStatementsAccess = 
		new ManyStatementsAccess.ManyStatementsSubView(mergeCondition(filterCondition))
		
	
	// OTHER    ------------------------
	
	/**
	 * @param wordId     Id of the searched word
	 * @param connection Implicit DB Connection
	 * @return Accessible statements that mention the specified word at the specified location
	 */
	def findWithWordAtIndex(wordId: Int, index: Int)(implicit connection: Connection) =
		findWithReferenceAtIndex(wordLinkModel, wordLinkModel.wordIdColumn, wordId, index)
	/**
	 * @param linkId     Id of the searched link
	 * @param connection Implicit DB Connection
	 * @return Accessible statements that mention the specified word at the specified location
	 */
	def findWithLinkAtIndex(linkId: Int, index: Int)(implicit connection: Connection) =
		findWithReferenceAtIndex(linkLinkModel, linkLinkModel.linkIdColumn, linkId, index)
	/**
	 * @param wordOrLinkId     Id of the searched word or link
	 * @param isLink Whether the searched item is a link and not a word.
	 *               Default = false.
	 * @param connection Implicit DB Connection
	 * @return Accessible statements that start with the specified word
	 */
	def findStartingWith(wordOrLinkId: Int, isLink: Boolean = false)
	                    (implicit connection: Connection) =
	{
		if (isLink)
			findWithLinkAtIndex(wordOrLinkId, 0)
		else
			findWithWordAtIndex(wordOrLinkId, 0)
	}
	
	/**
	 * @param length Targeted length (> 0)
	 * @param connection Implicit DB Connection
	 * @return Accessible statements that are shorter than the specified length
	 */
	def findShorterThan(length: Int)(implicit connection: Connection) = {
		val lengthLimit = Some(length).filter { _ > 1 }.map { _ - 1 }
		forNotLinkedTo(wordLinkModel.table,
			lengthLimit.map { wordLinkModel.withOrderIndex(_).toCondition }) { (wordCondition, wordJoin) =>
			forNotLinkedTo(linkLinkModel.table,
				lengthLimit.map { linkLinkModel.withOrderIndex(_).toCondition }) { (linkCondition, linkJoin) =>
				find(wordCondition && linkCondition, joins = Vector(wordJoin, linkJoin))
			} { find(wordCondition, joins = Vector(wordJoin)) }
		} { findNotLinkedTo(linkLinkModel.table) }
	}
	
	private def findWithReferenceAtIndex(model: StatementLinkedModel, refColumn: Column, refId: Int, index: Int)
	                                    (implicit connection: Connection) =
		find((refColumn <=> refId) && (model.orderIndexColumn <=> index), joins = Vector(model.table))
}


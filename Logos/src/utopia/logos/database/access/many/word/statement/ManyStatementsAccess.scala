package utopia.logos.database.access.many.word.statement

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.word.StatementDbFactory
import utopia.logos.database.storable.url.LinkPlacementModel
import utopia.logos.database.storable.word.{StatementLinkedModel, WordPlacementModel}
import utopia.logos.model.stored.word.Statement
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{ChronoRowFactoryView, ViewFactory}
import utopia.vault.sql.Condition

object ManyStatementsAccess extends ViewFactory[ManyStatementsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyStatementsAccess = new _ManyStatementsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyStatementsAccess(condition: Condition) extends ManyStatementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple statements at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyStatementsAccess 
	extends ManyStatementsAccessLike[Statement, ManyStatementsAccess] with ManyRowModelAccess[Statement] 
		with ChronoRowFactoryView[Statement, ManyStatementsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * Accessible empty statements (i.e. statements without any words)
	  * @param connection Implicit DB Connection
	  */
	def pullEmpty(implicit connection: Connection) = findShorterThan(1)
	
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
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyStatementsAccess = ManyStatementsAccess(condition)
	
	
	// OTHER	--------------------
	
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
				lengthLimit.map { linkLinkModel.withOrderIndex(_).toCondition }) { (linkCondition, 
					linkJoin) =>
				find(wordCondition && linkCondition, joins = Pair(wordJoin, linkJoin))
			} { find(wordCondition, joins = Single(wordJoin)) }
		} { findNotLinkedTo(linkLinkModel.table) }
	}
	
	/**
	  * @param wordOrLinkId     Id of the searched word or link
	  * @param isLink Whether the searched item is a link and not a word.
	  * Default = false.
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that start with the specified word
	  */
	def findStartingWith(wordOrLinkId: Int, isLink: Boolean = false)(implicit connection: Connection) = {
		if (isLink)
			findWithLinkAtIndex(wordOrLinkId, 0)
		else
			findWithWordAtIndex(wordOrLinkId, 0)
	}
	
	/**
	  * @param linkId     Id of the searched link
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that mention the specified word at the specified location
	  */
	def findWithLinkAtIndex(linkId: Int, index: Int)(implicit connection: Connection) = 
		findWithReferenceAtIndex(linkLinkModel, linkLinkModel.linkId.column, linkId, index)
	
	/**
	  * @param wordId     Id of the searched word
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that mention the specified word at the specified location
	  */
	def findWithWordAtIndex(wordId: Int, index: Int)(implicit connection: Connection) = 
		findWithReferenceAtIndex(wordLinkModel, wordLinkModel.wordId.column, wordId, index)
	
	private def findWithReferenceAtIndex(model: StatementLinkedModel, refColumn: Column, refId: Int, 
		index: Int)(implicit connection: Connection) = 
		find((refColumn <=> refId) && (model.orderIndexColumn <=> index), joins = Single(model.table))
}


package utopia.logos.database.access.many.text.statement

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.text.StatementDbFactory
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.stored.text.StoredStatement
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.HasTable
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
	override def apply(condition: Condition): ManyStatementsAccess = _ManyStatementsAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyStatementsAccess(override val accessCondition: Option[Condition]) 
		extends ManyStatementsAccess
}

/**
  * A common trait for access points which target multiple statements at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyStatementsAccess 
	extends ManyStatementsAccessLike[StoredStatement, ManyStatementsAccess] with ManyRowModelAccess[StoredStatement]
		with ChronoRowFactoryView[StoredStatement, ManyStatementsAccess]
{
	// COMPUTED ------------------------
	
	/**
	  * Accessible empty statements (i.e. statements without any words)
	  * @param connection Implicit DB Connection
	  */
	def pullEmpty(implicit connection: Connection) = findShorterThan(1)
	
	/**
	  * Model used for interacting with statement-word links
	  */
	protected def wordLinkModel = WordPlacementDbModel
	/**
	  * Model used for interacting with statement-link links
	  */
	protected def linkLinkModel = LinkPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = StatementDbFactory
	override protected def self = this
	
	override def apply(condition: Condition): ManyStatementsAccess = ManyStatementsAccess(condition)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param length Targeted length (> 0)
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that are shorter than the specified length
	  */
	def findShorterThan(length: Int)(implicit connection: Connection) = {
		val lengthLimit = Some(length).filter { _ > 1 }.map { _ - 1 }
		forNotLinkedTo(wordLinkModel.table, lengthLimit.map { wordLinkModel.withOrderIndex(_).toCondition }) {
			(wordCondition, wordJoin) =>
				forNotLinkedTo(linkLinkModel.table, lengthLimit.map { linkLinkModel.withOrderIndex(_).toCondition }) {
					(linkCondition, linkJoin) => find(wordCondition && linkCondition, joins = Pair(wordJoin, linkJoin))
				} { find(wordCondition, joins = Single(wordJoin)) }
		} { findNotLinkedTo(linkLinkModel.table) }
	}
	
	/**
	  * @param wordOrLinkId     Id of the searched word or link
	  * @param requiredStyle Style in which the word must appear.
	 *                      Only applied to words.
	  * @param isLink Whether the searched item is a link and not a word. Default = false.
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that start with the specified word
	  */
	def findStartingWith(wordOrLinkId: Int, requiredStyle: => Option[DisplayStyle] = None, isLink: Boolean = false)
	                    (implicit connection: Connection) =
	{
		if (isLink)
			findWithLinkAtIndex(wordOrLinkId, 0)
		else
			findWithWordAtIndex(wordOrLinkId, 0, requiredStyle)
	}
	
	/**
	  * @param linkId     Id of the searched link
	  * @param index 0-based index at which the specified link appears
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that mention the specified word at the specified location
	  */
	def findWithLinkAtIndex(linkId: Int, index: Int)(implicit connection: Connection) =
		findWithReferenceAtIndex(linkLinkModel, linkLinkModel.linkId.column, linkId, index)
	/**
	  * @param wordId     Id of the searched word
	  * @param index 0-based index at which the specified word appears
	  * @param requiredStyle Style in which the word must appear
	  * @param connection Implicit DB Connection
	  * @return Accessible statements that mention the specified word at the specified location
	  */
	def findWithWordAtIndex(wordId: Int, index: Int, requiredStyle: Option[DisplayStyle] = None)
	                       (implicit connection: Connection) =
		findWithReferenceAtIndex(wordLinkModel, wordLinkModel.wordId.column, wordId, index,
			requiredStyle.map { wordLinkModel.style <=> _ })
	
	private def findWithReferenceAtIndex(model: TextPlacementDbProps with HasTable, refColumn: Column, refId: Int,
	                                     index: Int, additionalCondition: Option[Condition] = None)
	                                    (implicit connection: Connection) =
		find(Condition.and(Pair(refColumn <=> refId, model.orderIndex <=> index) ++ additionalCondition),
			joins = Single(model.table))
}


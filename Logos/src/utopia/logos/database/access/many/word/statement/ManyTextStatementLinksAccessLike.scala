package utopia.logos.database.access.many.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.template.PlacedAccessLike
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points that target multiple text statement links or similar instances at a time
  * @author Mikko Hilpinen
  * @since 31.07.2024
  */
trait ManyTextStatementLinksAccessLike[+A, +Repr] 
	extends ManyModelAccess[A] with FilterableView[Repr] with Indexed with PlacedAccessLike[Repr]
{
	// ABSTRACT	--------------------
	
	protected def config: StatementLinkDbConfig
	
	
	// COMPUTED	--------------------
	
	/**
	  * Ids of the accessible items
	  * @param connection Implicit DB connection
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { _.getInt }
	
	/**
	  * Ids of the linked texts
	  * @param connection Implicit DB connection
	  */
	def textIds(implicit connection: Connection) = pullColumn(config.textIdColumn).map { _.getInt }
	
	/**
	  * Ids of the linked statements
	  * @param connection Implicit DB connection
	  */
	def statementIds(implicit connection: Connection) = pullColumn(config.statementIdColumn).map { _.getInt }
	
	/**
	  * Statement order/position indices
	  * @param connection Implicit DB connection
	  */
	def orderIndices(implicit connection: Connection) = pullColumn(config.orderIndexColumn).map { _.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	override protected def orderIndexColumn: Column = config.orderIndexColumn
	
	
	// OTHER	--------------------
	
	/**
	  * @param textId Id of the targeted text
	  * @return Access to links to that text
	  */
	def inText(textId: Int) = filter(config.textIdColumn <=> textId)
	
	/**
	  * @param textIds Ids of the targeted texts
	  * @return Access to links to those texts
	  */
	def inTexts(textIds: Iterable[Int]) = filter(config.textIdColumn.in(textIds))
	
	/**
	  * @param statementId Id of the targeted statement
	  * @return Access to links to that statement
	  */
	def toStatement(statementId: Int) = filter(config.statementIdColumn <=> statementId)
	
	/**
	  * @param statementIds Ids of the targeted statements
	  * @return Access to links to those statements
	  */
	def toStatements(statementIds: Iterable[Int]) = filter(config.statementIdColumn.in(statementIds))
}


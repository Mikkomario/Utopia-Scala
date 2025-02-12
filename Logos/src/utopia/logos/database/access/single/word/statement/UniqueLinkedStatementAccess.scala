package utopia.logos.database.access.single.word.statement

import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.logos.model.combined.word.LinkedStatement
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.Condition

@deprecated("Deprecated for removal", "v0.3")
object UniqueLinkedStatementAccess
{
	// OTHER	--------------------
	
	/**
	  * @param factory Factory used for parsing the linked statements from row models
	  * @param linkConfig Configurations used for targeting text statement links
	  * @param accessCondition The applied search condition that yields unique results
	  * @return An access point that utilizes the specified search condition
	  */
	def apply(factory: FromRowFactory[LinkedStatement], linkConfig: StatementLinkDbConfig, 
		accessCondition: Condition): UniqueLinkedStatementAccess = 
		SubAccess(factory, linkConfig, Some(accessCondition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(factory: FromRowFactory[LinkedStatement], linkConfig: StatementLinkDbConfig, 
		accessCondition: Option[Condition]) 
		extends UniqueLinkedStatementAccess
}

/**
  * Common trait for access point that return a single generic linked statement for any query
  * @author Mikko Hilpinen
  * @since 31.07.2024
  */
@deprecated("Deprecated for removal", "v0.3")
trait UniqueLinkedStatementAccess 
	extends UniqueLinkedStatementAccessLike[LinkedStatement, UniqueLinkedStatementAccess] 
		with SingleRowModelAccess[LinkedStatement]
{
	// IMPLEMENTED	--------------------
	
	override protected def self: UniqueLinkedStatementAccess = this
	
	
	// OTHER	--------------------
	
	def apply(condition: Condition): UniqueLinkedStatementAccess =
		UniqueLinkedStatementAccess(factory, linkConfig, condition)
}


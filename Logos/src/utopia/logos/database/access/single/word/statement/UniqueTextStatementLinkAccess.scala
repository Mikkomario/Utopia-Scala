package utopia.logos.database.access.single.word.statement

import utopia.logos.database.factory.word.TextStatementLinkDbFactory
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.logos.model.stored.word.TextStatementLink
import utopia.vault.sql.Condition

object UniqueTextStatementLinkAccess
{
	// OTHER    ---------------------------
	
	/**
	 * @param factory A factory used for parsing link data from DB row models
	 * @param accessCondition Filter condition applied to all queries.
	 *                        Should yield unique rows.
	 * @return A new access point that utilizes the specified access condition.
	 */
	def apply(factory: TextStatementLinkDbFactory, accessCondition: Condition): UniqueTextStatementLinkAccess =
		SubAccess(factory, Some(accessCondition))
	
	
	// NESTED   ---------------------------
	
	private case class SubAccess(factory: TextStatementLinkDbFactory, accessCondition: Option[Condition])
		extends UniqueTextStatementLinkAccess
}

/**
 * Common trait for access points that return individual text statement links (in the default model form) at a time
 *
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
trait UniqueTextStatementLinkAccess
	extends UniqueTextStatementLinkAccessLike[TextStatementLink, UniqueTextStatementLinkAccess]
{
	// ABSTRACT ---------------------------
	
	override def factory: TextStatementLinkDbFactory
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def self: UniqueTextStatementLinkAccess = this
	override protected def config: StatementLinkDbConfig = factory.config
	
	override def filter(additionalCondition: Condition): UniqueTextStatementLinkAccess =
		UniqueTextStatementLinkAccess(factory, mergeCondition(additionalCondition))
}

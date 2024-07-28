package utopia.logos.database.access.single.word.statement

import utopia.logos.database.factory.word.TextStatementLinkDbFactory
import utopia.logos.model.stored.word.TextStatementLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
 * An access point used for targeting individual text statement links
 *
 * @author Mikko Hilpinen
 * @since 16/03/2024, v0.2
 */
case class DbSingleTextStatementLink(factory: TextStatementLinkDbFactory, id: Int)
	extends UniqueTextStatementLinkAccess with SingleIntIdModelAccess[TextStatementLink]
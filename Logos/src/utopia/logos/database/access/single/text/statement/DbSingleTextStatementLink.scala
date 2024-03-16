package utopia.logos.database.access.single.text.statement

import utopia.logos.database.factory.text.TextStatementLinkDbFactory
import utopia.logos.model.stored.text.TextStatementLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
 * An access point used for targeting individual text statement links
 *
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
case class DbSingleTextStatementLink(factory: TextStatementLinkDbFactory, id: Int)
	extends UniqueTextStatementLinkAccess with SingleIntIdModelAccess[TextStatementLink]
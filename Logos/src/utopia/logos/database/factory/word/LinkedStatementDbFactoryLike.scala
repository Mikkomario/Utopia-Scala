package utopia.logos.database.factory.word

import utopia.logos.model.stored.word.Statement
import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.vault.nosql.factory.row.model.FromRowModelFactory

/**
 * Common trait for factory classes which parse linked statement data from DB rows
 *
 * @tparam Combined Type of the returned linked statements
 * @tparam Link Type of the attached links
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
trait LinkedStatementDbFactoryLike[+Combined, Link] extends CombiningFactory[Combined, Statement, Link]
{
	override def parentFactory: FromRowModelFactory[Statement] = StatementDbFactory
}

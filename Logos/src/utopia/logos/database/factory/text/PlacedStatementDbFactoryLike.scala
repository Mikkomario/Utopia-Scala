package utopia.logos.database.factory.text

import utopia.logos.model.stored.text.Statement
import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.vault.nosql.factory.row.model.FromRowModelFactory

/**
  * Common trait for factories which combine statements with some type of placements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait PlacedStatementDbFactoryLike[+Combined, Placement] extends CombiningFactory[Combined, Statement, Placement]
{
	override def parentFactory: FromRowModelFactory[Statement] = StatementDbFactory
}

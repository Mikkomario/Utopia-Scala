package utopia.logos.database.factory.word

import utopia.logos.model.combined.word.LinkedStatement
import utopia.logos.model.stored.word.{Statement, TextStatementLink}
import utopia.vault.nosql.factory.row.FromRowFactory

@deprecated("Replaced with PlacedStatementDbFactory", "v0.3")
object LinkedStatementDbFactory
{
	// OTHER    --------------------------
	
	/**
	 * @param linkFactory Factory instance used for parsing text statement links
	 * @return A factory used for reading (generic) linked statements
	 */
	def apply(linkFactory: FromRowFactory[TextStatementLink]): LinkedStatementDbFactory =
		new _LinkedStatementDbFactory(linkFactory)
	
	
	// NESTED   --------------------------
	
	private class _LinkedStatementDbFactory(override val childFactory: FromRowFactory[TextStatementLink])
		extends LinkedStatementDbFactory
}

/**
 * Common trait for factories used for parsing (generic) linked statement data
 *
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
@deprecated("Replaced with PlacedStatementDbFactory", "v0.3")
trait LinkedStatementDbFactory extends LinkedStatementDbFactoryLike[LinkedStatement, TextStatementLink]
{
	override def apply(parent: Statement, child: TextStatementLink): LinkedStatement = LinkedStatement(parent, child)
}

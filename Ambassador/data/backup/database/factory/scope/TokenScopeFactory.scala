package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.model.token.TokenScopeLinkModel
import utopia.ambassador.model.combined.scope.TokenScope
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.JoinType.Inner

/**
  * Used for reading scope information from the DB, including links to tokens
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object TokenScopeFactory extends FromRowFactory[TokenScope]
{
	// COMPUTED -------------------------------
	
	private def scopeFactory = ScopeFactory
	private def linkModel = TokenScopeLinkModel
	
	private def linkTable = linkModel.table
	private def scopeTable = scopeFactory.table
	
	
	// IMPLEMENTED  ---------------------------
	
	override def table = linkTable
	
	override def joinedTables = Vector(scopeTable)
	
	override def joinType = Inner
	
	// Makes sure the row contains link data
	override def apply(row: Row) = linkTable.requirementDeclaration.validate(row(linkTable)).toTry
		.flatMap { link =>
			// Parses scope data
			scopeFactory(row).map { scope =>
				// Combines the data
				TokenScope(scope, link(linkModel.tokenIdAttName).getInt, link("id").getInt)
			}
		}
}

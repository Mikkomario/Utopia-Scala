package utopia.logos.database.access.single.word.statement

import utopia.logos.database.factory.word.TextStatementLinkDbFactory
import utopia.logos.database.storable.word.{TextStatementLinkModel, TextStatementLinkModelFactory}
import utopia.logos.model.stored.word.TextStatementLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
 * Root access point for individual generic text statement links
 *
 * @author Mikko Hilpinen
 * @since 16/03/2024, v1.0
 */
case class DbTextStatementLink(factory: TextStatementLinkDbFactory)
	extends SingleRowModelAccess[TextStatementLink] with UnconditionalView with Indexed
{
	// ATTRIBUTES   -------------------
	
	private lazy val model = TextStatementLinkModel(factory)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param id Targeted statement link id
	 * @return Access to that link
	 */
	def apply(id: Int) = DbSingleTextStatementLink(factory, id)
	
	/**
	 * @param textId Id of the linked text
	 * @param statementId Id of the linked statement
	 * @return Access to a link between those two instances
	 */
	def between(textId: Int, statementId: Int) =
		filterDistinct(model.withTextId(textId).withStatementId(statementId).toCondition)
	
	/**
	 * @param textId Id of the linked text
	 * @param orderIndex Position where the statement appears
	 * @return Access to that link
	 */
	def at(textId: Int, orderIndex: Int) =
		filterDistinct(model.withTextId(textId).at(orderIndex).toCondition)
	
	/**
	 * @param condition A condition that yields a unique link
	 * @return Access to that link
	 */
	def filterDistinct(condition: Condition) = UniqueTextStatementLinkAccess(factory, condition)
}

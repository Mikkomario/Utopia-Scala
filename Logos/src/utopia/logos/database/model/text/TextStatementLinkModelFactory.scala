package utopia.logos.database.model.text

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.text.TextStatementLinkDbFactory
import utopia.logos.model.partial.text.TextStatementLinkData
import utopia.logos.model.stored.text.TextStatementLink
import utopia.vault.model.immutable.Table

/**
 * A factory used for constructing DB models for text-statement links and for inserting new links to the DB.
 *
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
case class TextStatementLinkModelFactory(factory: TextStatementLinkDbFactory)
	extends TextStatementLinkModelFactoryLike[TextStatementLinkModel, TextStatementLink, TextStatementLinkData]
{
	// IMPLEMENTED  ---------------------
	
	override def table: Table = factory.table
	
	override def withId(id: Int): TextStatementLinkModel = apply(Some(id))
	override def withTextId(textId: Int): TextStatementLinkModel = apply(textId = Some(textId))
	override def withStatementId(statementId: Int): TextStatementLinkModel = apply(statementId = Some(statementId))
	override def at(orderIndex: Int): TextStatementLinkModel = apply(orderIndex = Some(orderIndex))
	
	override def apply(data: TextStatementLinkData): TextStatementLinkModel =
		apply(None, Some(data.textId), Some(data.statementId), Some(data.orderIndex))
	
	override protected def complete(id: Value, data: TextStatementLinkData): TextStatementLink =
		TextStatementLink(id.getInt, data)
	
	
	// OTHER    ------------------------
	
	/**
	 * Creates a new text statement link DB model
	 * @param id Id to assign to the model (optional)
	 * @param textId Id of the linked text (optional)
	 * @param statementId Id of the linked statement (optional)
	 * @param orderIndex Index that determines the statement's location (optional)
	 * @return A new DB model with the specified properties
	 */
	def apply(id: Option[Int] = None, textId: Option[Int] = None, statementId: Option[Int] = None,
	          orderIndex: Option[Int] = None) =
		TextStatementLinkModel(factory, id, textId, statementId, orderIndex)
}
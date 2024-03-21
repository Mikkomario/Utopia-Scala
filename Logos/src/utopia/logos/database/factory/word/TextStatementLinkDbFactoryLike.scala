package utopia.logos.database.factory.word

import utopia.flow.generic.model.immutable.Model
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
 * Common trait for DB factories which read text statement links or similar instances
 * @tparam A Type of the links read
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
trait TextStatementLinkDbFactoryLike[+A] extends FromValidatedRowModelFactory[A]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The configurations used in this factory
	 */
	def config: StatementLinkDbConfig
	
	/**
	 * Creates a new text statement link instance from a validated row model
	 * @param id Id to assign
	 * @param textId Id of the linked text
	 * @param statementId Id of the linked statement
	 * @param orderIndex 0-based order-index for the statement within the text
	 * @param model Model for retrieving additional properties
	 * @return A new text statement link
	 */
	protected def fromValidatedModel(id: Int, textId: Int, statementId: Int, orderIndex: Int, model: Model): A
	
	
	// IMPLEMENTED  --------------------
	
	override def table: Table = config.table
	override def defaultOrdering: Option[OrderBy] = Some(OrderBy.ascending(config.orderIndexColumn))
	
	override protected def fromValidatedModel(model: Model): A =
		fromValidatedModel(model("id").getInt, model(config.textIdAttName).getInt,
			model(config.statementIdAttName).getInt, model(config.orderIndexAttName).getInt, model)
}
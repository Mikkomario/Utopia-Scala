package utopia.logos.database.model.text

import utopia.logos.database.factory.text.TextStatementLinkDbFactory

object TextStatementLinkModel
{
	// OTHER    --------------------------
	
	/**
	 * @param factory A text-statement-link factory to wrap
	 * @return A factory for constructing DB-interaction models to interact with that factory's table
	 */
	def apply(factory: TextStatementLinkDbFactory) = TextStatementLinkModelFactory(factory)
	
	/**
	 * Creates a new DB-interaction model for text statement links
	 * @param factory Wrapped factory instance
	 * @param id Targeted row id (optional)
	 * @param textId Targeted text id (optional)
	 * @param statementId Targeted statement id (optional)
	 * @param orderIndex Targeted order index (optional)
	 * @return A new DB-interaction model (optional)
	 */
	def apply(factory: TextStatementLinkDbFactory, id: Option[Int] = None,
	          textId: Option[Int] = None, statementId: Option[Int] = None,
	          orderIndex: Option[Int] = None): TextStatementLinkModel =
		_TextStatementLinkModel(factory, id, textId, statementId, orderIndex)
	
	
	// NESTED   --------------------------
	
	private case class _TextStatementLinkModel(factory: TextStatementLinkDbFactory, id: Option[Int],
	                                           textId: Option[Int], statementId: Option[Int], orderIndex: Option[Int])
		extends TextStatementLinkModel
}

/**
 * A DB model used for interacting with text statement links in a table
 *
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
trait TextStatementLinkModel extends TextStatementLinkModelLike[TextStatementLinkModel]
{
	// IMPLEMENTED  -------------------------
	
	override protected def additionalValueProperties = Iterable.empty
	
	override protected def buildCopy(id: Option[Int], textId: Option[Int], statementId: Option[Int],
	                                 orderIndex: Option[Int]) =
		TextStatementLinkModel(factory, id, textId, statementId, orderIndex)
}
package utopia.logos.model.cached

import utopia.flow.view.immutable.caching.ConditionalLazy
import utopia.logos.database.LogosContext

import scala.util.Try

/**
 * Used for configuring the database-interface for a statement link model / table
 * @constructor Creates a new configuration for a unique statement link table
 * @param tableName Name of the table from which link data is read
 * @param textIdAttName Name of the DB property that refers to the text where the statement appears.
 *                      Default = textId
 * @param statementIdAttName Name of the DB property that refers to the statement that appears in the text.
 *                           Default = statementId.
 * @param orderIndexAttName Name of the property that acts as a
 *                          0-based index which shows the location of the linked statement in the linked text.
 *                          Default = orderIndex.
 * @author Mikko Hilpinen
 * @since 14/03/2024, v0.2
 */
@deprecated("Replaced with TextPlacementDbProps", "v0.3")
case class StatementLinkDbConfig(tableName: String, textIdAttName: String = "textId",
                                 statementIdAttName: String = "statementId",
                                 orderIndexAttName: String = "orderIndex")
{
	// ATTRIBUTES   -----------------------
	
	private val lazyTable = ConditionalLazy.ifSuccessful { Try { LogosContext.table(tableName) } }
	
	
	// COMPUTED --------------------------
	
	/**
	 * @throws IllegalStateException If [[LogosContext]] has not been set up yet
	 * @return Table used for storing these links
	 */
	@throws[IllegalStateException]("If LogosContext has not been set up yet")
	def table = lazyTable.value.get
	
	/**
	 * @throws IllegalStateException If [[LogosContext]] has not been set up yet
	 * @return Column that refers to the text where the statement appears
	 */
	@throws[IllegalStateException]("If LogosContext has not been set up yet")
	def textIdColumn = table(textIdAttName)
	/**
	 * @throws IllegalStateException If [[LogosContext]] has not been set up yet
	 * @return Column that refers to the statement that appears in the text
	 */
	@throws[IllegalStateException]("If LogosContext has not been set up yet")
	def statementIdColumn = table(statementIdAttName)
	/**
	 * @throws IllegalStateException If [[LogosContext]] has not been set up yet
	 * @return Column that acts as a 0-based index which shows the location of the linked statement in the linked text
	 */
	@throws[IllegalStateException]("If LogosContext has not been set up yet")
	def orderIndexColumn = table(orderIndexAttName)
}

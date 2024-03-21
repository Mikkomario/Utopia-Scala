package utopia.logos.database.storable.word

import utopia.vault.model.immutable.Table

/**
 * Common trait for models that are used with statement links
 * @author Mikko Hilpinen
 * @since 15.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
 */
trait StatementLinkedModel
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Table used by this model
	 */
	def table: Table
	
	/**
	 * @return Name of the property that represents a statement id
	 */
	def statementIdAttName: String
	/**
	 * @return Name of the property that specifies the statement's relative position
	 */
	def orderIndexAttName: String
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Column that represents a statement id
	 */
	def statementIdColumn = table(statementIdAttName)
	/**
	 * @return Column that specifies the statement's relative position
	 */
	def orderIndexColumn = table(orderIndexAttName)
}

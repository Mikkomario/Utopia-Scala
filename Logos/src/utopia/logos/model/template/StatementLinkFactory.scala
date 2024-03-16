package utopia.logos.model.template

/**
 * Common trait for classes that generate statement link models by assigning properties to them
 *
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
trait StatementLinkFactory[+A] extends PlacedFactory[A]
{
	/**
	 * @param textId Id of the associated text instance
	 * @return Copy of this item with the specified text id
	 */
	def withTextId(textId: Int): A
	/**
	 * @param statementId Id of the linked statement
	 * @return Copy of this item with the specified statement id
	 */
	def withStatementId(statementId: Int): A
}

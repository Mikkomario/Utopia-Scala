package utopia.vault.model.template

import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vault.nosql.template.Indexed

/**
 * Common trait for interfaces which provide an `id: DbPropertyDeclaration` property
 * @author Mikko Hilpinen
 * @since 16.06.2024, v1.19
 */
trait HasIdProperty extends Indexed
{
	/**
	 * @return A database property which defines the primary row id
	 */
	def id: DbPropertyDeclaration
}

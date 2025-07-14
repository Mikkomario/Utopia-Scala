package utopia.vault.model.template

import utopia.vault.model.enumeration.SelectTarget

/**
 * Common trait for interfaces which specify their select target based on their list of tables
 *
 * @author Mikko Hilpinen
 * @since 14.07.2025, v1.22
 */
trait SelectsTables extends HasSelectTarget with HasTables
{
	override def selectTarget: SelectTarget = tables
}

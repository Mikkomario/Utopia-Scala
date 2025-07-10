package utopia.vault.model.template

import utopia.vault.model.enumeration.SelectTarget

/**
  * Common trait for classes which select data from a single table only
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait SelectsTable extends HasSelectTarget with HasTable
{
	override def selectTarget: SelectTarget = table
}

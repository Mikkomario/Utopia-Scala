package utopia.vault.nosql.access.template.column

import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that are used for accessing row ids (primary keys)
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait IdAccess[+ID, +A] extends ColumnAccess[ID, A] with Indexed
{
	// IMPLEMENTED  -------------------
	
	override def column = index
}

package utopia.vault.nosql.access.template.column

import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that are used for accessing row ids (primary keys).
  *
  * Note: Use of this class is discouraged, since it is likely to get deprecated and removed.
  * Please use [[utopia.vault.nosql.targeting.columns.AccessColumns]] instead.
  *
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait IdAccess[+ID, +A] extends ColumnAccess[ID, A] with Indexed
{
	// IMPLEMENTED  -------------------
	
	override def column = index
}

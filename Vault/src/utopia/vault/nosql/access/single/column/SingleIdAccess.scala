package utopia.vault.nosql.access.single.column

import utopia.vault.nosql.access.template.column.IdAccess

/**
  * Used for accessing individual ids in a table
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait SingleIdAccess[+ID] extends IdAccess[Option[ID], Option[ID]] with SingleColumnAccess[ID]

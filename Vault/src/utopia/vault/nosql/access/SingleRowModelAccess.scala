package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value

/**
  * Used for accessing individual models where each model occupies exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait SingleRowModelAccess[+A] extends RowModelAccess[A, Option[A], Value] with SingleModelAccess[A]

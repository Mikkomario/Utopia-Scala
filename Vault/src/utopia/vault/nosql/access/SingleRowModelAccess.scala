package utopia.vault.nosql.access

/**
  * Used for accessing individual models where each model occupies exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait SingleRowModelAccess[+A] extends RowModelAccess[A, Option[A]] with SingleModelAccess[A]

package utopia.vault.nosql.access

/**
  * Used for accessing multiple models at once, each model occupying exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait ManyRowModelAccess[+A] extends RowModelAccess[A, Vector[A]] with ManyModelAccess[A]

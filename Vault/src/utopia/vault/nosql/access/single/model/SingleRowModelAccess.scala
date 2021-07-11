package utopia.vault.nosql.access.single.model

import utopia.vault.nosql.view.RowFactoryView

/**
  * Used for accessing individual models where each model occupies exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait SingleRowModelAccess[+A] extends SingleModelAccess[A] with RowFactoryView[A]

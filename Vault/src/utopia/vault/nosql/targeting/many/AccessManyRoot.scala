package utopia.vault.nosql.targeting.many

import utopia.vault.model.immutable.Table
import utopia.vault.model.template.HasTable
import utopia.vault.nosql.view.{ViewFactory, ViewManyByIntIds}
import utopia.vault.sql.Condition

object AccessManyRoot
{
	// IMPLICIT -------------------------
	
	// Provides implicit access to the root property
	def autoAccessRoot[A <: ViewFactory[A] with HasTable](access: AccessManyRoot[A]): A = access.root
}

/**
  * Common trait for root database access objects
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait AccessManyRoot[+A <: ViewFactory[A] with HasTable] extends ViewManyByIntIds[A]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The wrapped root access point
	  */
	def root: A
	
	
	// IMPLEMENTED  -------------------
	
	override def table: Table = root.table
	
	override def apply(condition: Condition): A = root(condition)
}

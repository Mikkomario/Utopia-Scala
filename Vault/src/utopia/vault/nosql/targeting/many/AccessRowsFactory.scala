package utopia.vault.nosql.targeting.many

import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.factory.row.FromRowFactory

/**
  * A trait for facilitating many rows -wrapping access construction
  * @author Mikko Hilpinen
  * @since 30.06.2025, v1.21.1
  */
trait AccessRowsFactory[+T[_]]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @param accessRows A generic row-based access-point
	  * @tparam A Type of parsed items
	  * @return An access point wrapping the specified access
	  */
	def wrap[A](accessRows: AccessManyRows[A]): T[A]
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param factory A factory for parsing items from database rows
	  * @tparam A Type of parsed items
	  * @return Access to all items in the targeted factory's table(s)
	  */
	def apply[A](factory: FromRowFactory[A]) = wrap(AccessManyRows(factory))
	/**
	  * @param factory A factory for parsing items from database rows
	  * @tparam A Type of parsed items
	  * @return Access to currently active (i.e. non-deprecated) items in the targeted factory's table(s)
	  */
	def active[A](factory: FromRowFactory[A] with Deprecates) = wrap(AccessManyRows.active(factory))
}

package utopia.vault.store

import utopia.flow.util.EitherExtensions._
import utopia.flow.view.template.Extender

import scala.language.implicitConversions

object StoreResult
{
	// IMPLICIT -----------------------
	
	/**
	 * @param stored The item that was inserted to the DB (if left), or already existed in the DB (if right)
	 * @tparam A Type of the item to wrap
	 * @return A store result wrapping the specified item
	 */
	implicit def apply[A <: HasId[Int]](stored: Sided[A]): StoreResult[A] = apply(stored.either, isNew = stored.isLeft)
	
	implicit def accessData[D](r: StoreResult[Stored[D, Int]]): D = r.stored.data
	
	
	// OTHER    ----------------------
	
	/**
	 * @param item A newly inserted item to wrap
	 * @tparam A Type of the wrapped item
	 * @return A store result wrapping that item
	 */
	def inserted[A <: HasId[Int]](item: A) = apply(item, isNew = true)
	/**
	 * @param item A newly inserted item to wrap
	 * @param id ID of the specified item
	 * @tparam A Type of the wrapped item
	 * @return A store result wrapping that item
	 */
	def inserted[A](item: A, id: Int) = apply(item, id, isNew = true)
	/**
	 * @param item An existing DB entry to wrap
	 * @tparam A Type of the wrapped item
	 * @return A store result wrapping that item
	 */
	def existed[A <: HasId[Int]](item: A) = apply(item)
	/**
	 * @param item An existing DB entry to wrap
	 * @param id ID of the specified item
	 * @tparam A Type of the wrapped item
	 * @return A store result wrapping that item
	 */
	def existed[A](item: A, id: Int) = apply(item, id, isNew = false)
	
	/**
	 * @param item An item to wrap (either inserted or pulled from the DB)
	 * @param isNew Whether this item was newly inserted (default = false)
	 * @tparam A Type of the specified item
	 * @return A new store result wrapping the specified item
	 */
	def apply[A <: HasId[Int]](item: A, isNew: Boolean = false): StoreResult[A] = apply(item, item.id, isNew)
}

/**
 * Represents the results of a "store" operation, where data is searched and inserted if not found.
 * @tparam A Type of the wrapped item
 * @param stored The wrapped item
 * @param id Unique ID of the wrapped item
 * @param isNew Whether the wrapped item was newly inserted to the DB
 * @author Mikko Hilpinen
 * @since 27.07.2025, v1.22
 */
case class StoreResult[+A](stored: A, id: Int, isNew: Boolean)
	extends Extender[A] with IdOrInserted[A]
{
	// IMPLEMENTED  ----------------------
	
	override def wrapped: A = stored
	
	override def inserted: Option[A] = if (isNew) Some(stored) else None
	override def existingId: Option[Int] = if (isNew) None else Some(id)
	
	override def toString = s"$stored (${ if (isNew) "inserted" else "existed" })"
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function applied to the stored element
	  * @tparam B Type of mapping results
	  * @return A copy of this result with mapped contents.
	 *         Note: [[isNew]] and [[id]] are preserved.
	  */
	def map[B](f: A => B) = copy(stored = f(stored))
}

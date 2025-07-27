package utopia.vault.model.immutable

import utopia.flow.util.EitherExtensions._
import utopia.flow.view.template.Extender
import utopia.vault.model.template.{HasId, Stored}

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
	 * @param item An existing DB entry to wrap
	 * @tparam A Type of the wrapped item
	 * @return A store result wrapping that item
	 */
	def existed[A <: HasId[Int]](item: A) = apply(item)
}

/**
 * Represents the results of a "store" operation, where data is searched and inserted if not found.
 * @tparam A Type of the wrapped item
 * @param stored The wrapped item
 * @param isNew Whether the wrapped item was newly inserted to the DB
 * @author Mikko Hilpinen
 * @since 27.07.2025, v1.22
 */
case class StoreResult[+A <: HasId[Int]](stored: A, isNew: Boolean = false)
	extends Extender[A] with IdOrInserted[A]
{
	// IMPLEMENTED  ----------------------
	
	override def wrapped: A = stored
	
	override def id: Int = stored.id
	
	override def inserted: Option[A] = if (isNew) Some(stored) else None
	override def existingId: Option[Int] = if (isNew) None else Some(id)
}

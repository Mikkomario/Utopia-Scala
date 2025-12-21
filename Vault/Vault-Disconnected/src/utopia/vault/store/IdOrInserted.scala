package utopia.vault.store

import utopia.flow.util.EitherExtensions._

import scala.language.implicitConversions

object IdOrInserted
{
	// IMPLICIT ------------------------
	
	/**
	 * @param idOrInserted Either:
	 *                          - Right: ID of an existing match in the DB
	 *                          - Left: A newly inserted instance
	 * @tparam A Type of the newly inserted instance, if included
	 * @return A wrapper for the specified item
	 */
	implicit def apply[A <: HasId[Int]](idOrInserted: Either[A, Int]): IdOrInserted[A] =
		new IdOrExtractFromInserted(idOrInserted)
	/**
	 * @param id The ID to wrap
	 * @tparam A Type of the theoretical inserted instance
	 * @return A new wrapper for the specified ID
	 */
	implicit def apply[A](id: Int): IdOrInserted[A] = new ExistingIdWrapper(id)
	
	
	// OTHER    ------------------------
	
	/**
	 * @param id ID of the inserted or existing item
	 * @param inserted If a new item was inserted, pass it here
	 * @tparam A Type of the item, if inserted
	 * @return A new ID or inserted -wrapper for the specified item and/or ID
	 */
	def apply[A](id: Int, inserted: Option[A]): IdOrInserted[A] = new _IdOrInserted[A](id, inserted)
	
	
	// NESTED   ------------------------
	
	private class ExistingIdWrapper[+A](override val id: Int) extends IdOrInserted[A]
	{
		override val isNew: Boolean = false
		override val inserted: Option[A] = None
		override val existingId: Option[Int] = Some(id)
	}
	
	private class IdOrExtractFromInserted[+A <: HasId[Int]](data: Either[A, Int]) extends IdOrInserted[A]
	{
		// ATTRIBUTES   ----------------
		
		override val id: Int = data.rightOrMap { _.id }
		
		
		// IMPLEMENTED  ----------------
		
		override def isNew: Boolean = data.isLeft
		
		override def inserted: Option[A] = data.leftOption
		override def existingId: Option[Int] = data.toOption
	}
	
	private class _IdOrInserted[+A](override val id: Int, override val inserted: Option[A]) extends IdOrInserted[A]
	{
		override def isNew: Boolean = inserted.isDefined
		override def existingId: Option[Int] = if (isNew) None else Some(id)
	}
}

/**
 * Used for wrapping results of "store" functions, which only insert non-duplicate data.
 * Wraps either the inserted data entry, or the ID of the matching instance in the DB.
 * @tparam A Type of the inserted item, if/when applicable
 * @author Mikko Hilpinen
 * @since 27.07.2025, v1.22
 */
trait IdOrInserted[+A] extends StoredId
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return If this represents a newly inserted instance, yields that instance. Otherwise, yields None.
	 */
	def inserted: Option[A]
}

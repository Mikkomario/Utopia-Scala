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
	implicit def apply[A <: HasId[Int]](idOrInserted: Either[A, Int]): IdOrInserted[A] = _IdOrInserted(idOrInserted)
	
	
	// NESTED   ------------------------
	
	private case class _IdOrInserted[+A <: HasId[Int]](data: Either[A, Int]) extends IdOrInserted[A]
	{
		// ATTRIBUTES   ----------------
		
		override lazy val id: Int = data.rightOrMap { _.id }
		override lazy val isNew: Boolean = data.isLeft
		
		
		// IMPLEMENTED  ----------------
		
		override def inserted: Option[A] = data.leftOption
		override def existingId: Option[Int] = data.toOption
	}
}

/**
 * Used for wrapping results of "store" functions, which only insert non-duplicate data.
 * Wraps either the inserted data entry, or the ID of the matching instance in the DB.
 * @tparam A Type of the inserted item, if/when applicable
 * @author Mikko Hilpinen
 * @since 27.07.2025, v1.22
 */
trait IdOrInserted[+A <: HasId[Int]] extends HasId[Int]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Whether this represents / wraps a newly inserted instance
	 */
	def isNew: Boolean
	
	/**
	 * @return If this represents a newly inserted instance, yields that instance. Otherwise, yields None.
	 */
	def inserted: Option[A]
	/**
	 * @return If this represents an instance that already existed in the DB, yields the ID of that instance.
	 *         Otherwise, yields None.
	 */
	def existingId: Option[Int]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether this represents an instance that already existed in the DB
	 */
	def existed = !isNew
}

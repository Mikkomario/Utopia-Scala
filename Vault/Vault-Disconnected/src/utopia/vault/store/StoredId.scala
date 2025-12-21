package utopia.vault.store

object StoredId
{
	// OTHER    -----------------------
	
	/**
	 * @param id ID of a newly inserted item
	 * @return Stored version of that ID
	 */
	def inserted(id: Int) = apply(id, isNew = true)
	/**
	 * @param id ID of an item that already existed in the DB
	 * @return Stored version of that ID
	 */
	def existed(id: Int) = apply(id, isNew = false)
	
	/**
	 * @param id ID to wrap
	 * @param isNew Whether this ID was acquired from a newly inserted instance
	 * @return A stored version of the specified ID
	 */
	def apply(id: Int, isNew: Boolean): StoredId = new _StoredId(id, isNew)
	
	
	// NESTED   -----------------------
	
	private class _StoredId(override val id: Int, override val isNew: Boolean) extends StoredId
	{
		override def existingId: Option[Int] = if (isNew) None else Some(id)
	}
}

/**
 * Used for representing the ID of an instance that was stored,
 * i.e. an instance that was either inserted as a new entry, or existed already in the DB.
 * @author Mikko Hilpinen
 * @since 21.12.2025, v2.1
 */
trait StoredId extends HasId[Int]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Whether this represents / wraps a newly inserted instance
	 */
	def isNew: Boolean
	
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

package utopia.vault.database

import utopia.flow.collection.immutable.{IntSet, OptimizedIndexedSeq}
import utopia.flow.collection.template.MapAccess
import utopia.vault.database.ReplaceHandler.MappingReplaceHandler
import utopia.vault.nosql.targeting.columns.AccessColumnValue
import utopia.vault.nosql.view.{TimeDeprecatableView, ViewManyByIntIds}
import utopia.vault.store.HasId

object ReplaceHandler
{
	// OTHER    -------------------------
	
	/**
	 * Creates a replace-handler which applies a deprecating timestamp to the replaced items / rows
	 * @param rootAccess Root-level access to the items in the database. Used for performing the deprecation.
	 * @param shouldReplace A function that accepts the proposed item and the item that already exists in the DB.
	 *                          - Yields true if the new item should be considered a new version
	 *                            and replace the existing item.
	 *                          - Yields false if the new item should be considered a duplicate
	 *                            and ignored / not inserted to the DB.
	 * @tparam In Type of new values
	 * @tparam E Type of existing DB entries
	 * @return A new replace-handler, which deprecates by updating a timestamp column
	 */
	def deprecating[In, E <: HasId[Int]](rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])
	                                    (shouldReplace: (In, E) => Boolean) =
		DeprecatingReplaceHandler[In, E](rootAccess)(shouldReplace)
	
	/**
	 * Creates a replace-implementation for database entries
	 * that are deprecated by specifying the ID of the replacing entry.
	 * @param shouldReplace A function that accepts the proposed item and the item that already exists in the DB.
	 *                          - Yields true if the new item should be considered a new version
	 *                            and replace the existing item.
	 *                          - Yields false if the new item should be considered a duplicate
	 *                            and ignored / not inserted to the DB.
	 * @param accessReplacingIdColumn A function which accepts an item that will be replaced,
	 *                                and provides access to that item's replacing ID -column
	 * @tparam In Type of the stored items
	 * @tparam E Type of the existing items
	 * @return A new replace-handler
	 */
	def applyReplacingIds[In, E](shouldReplace: (In, E) => Boolean)
	                            (accessReplacingIdColumn: E => AccessColumnValue[_, _, Int]) =
		apply[In, E, HasId[Int]](shouldReplace) { (replacements, connection) =>
			connection.use { implicit c =>
				replacements.foreach { case (newEntry, existing) => accessReplacingIdColumn(existing).set(newEntry.id) }
			}
		}
		
	
	/**
	 * Creates a new replace-handler, where the replacements are performed after the inserts
	 * @param shouldReplace A function that accepts the proposed item and the item that already exists in the DB.
	 *                          - Yields true if the new item should be considered a new version
	 *                            and replace the existing item.
	 *                          - Yields false if the new item should be considered a duplicate
	 *                            and ignored / not inserted to the DB.
	 * @param replace A function that performs the actual replacement, based on two parameters:
	 *                      1. Replacements to perform. Never empty.
	 *                         Each entry is a pair containing two stored versions:
	 *                              1. The newly inserted version
	 *                              1. The earlier version, which needs to be replaced / deprecated
	 *                      1. Database connection to use
	 * @tparam In Type of new items, before they've been inserted
	 * @tparam E Type of the existing items
	 * @tparam S Type of stored / inserted items
	 * @return A new replace-handler
	 */
	def apply[In, E, S](shouldReplace: (In, E) => Boolean)
	                   (replace: (IndexedSeq[(S, E)], Connection) => Unit): ReplaceHandler[In, E, S] =
		new _ReplaceHandler[In, E, S](shouldReplace)(replace)
	
	
	// NESTED   -------------------------
	
	object DeprecatingReplaceHandler
	{
		/**
		 * @param rootAccess Root-level access point to items which may be deprecated
		 * @param shouldReplace A function for testing whether a new item (1) should replace an existing item (2).
		 * @tparam In Type of evaluated new items
		 * @tparam E Type of existing items
		 * @return A new replace-handler
		 */
		def apply[In, E <: HasId[Int]](rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])
		                              (shouldReplace: (In, E) => Boolean) =
			new DeprecatingReplaceHandler[In, E](rootAccess)(shouldReplace)(_.id)
	}
	class DeprecatingReplaceHandler[-In, -E](rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])
	                                        (shouldReplace: (In, E) => Boolean)(idOf: E => Int)
		extends ReplaceHandler[In, E, Any]
	{
		// ATTRIBUTES   -----------------
		
		// Collects the IDs which need to be deprecated
		private val idsToReplaceBuilder = IntSet.newBuilder
		
		
		// IMPLEMENTED  -----------------
		
		override def handleMatch(key: Any, newItem: In, existingItem: E): Boolean = {
			// Case: Replacement => Remembers the old version's ID
			if (shouldReplace(newItem, existingItem)) {
				idsToReplaceBuilder += idOf(existingItem)
				true
			}
			else
				false
		}
		
		override def replace(inserted: MapAccess[Any, Any])(implicit connection: Connection): Unit = {
			// Deprecates the rows matching the collected IDs
			rootAccess(idsToReplaceBuilder.result()).deprecate()
			idsToReplaceBuilder.clear()
		}
	}
	
	private class _ReplaceHandler[-In, E, S](shouldReplace: (In, E) => Boolean)
	                                        (replace: (IndexedSeq[(S, E)], Connection) => Unit)
		extends ReplaceHandler[In, E, S]
	{
		// ATTRIBUTES   -----------------------
		
		// Collects the items to replace, mapped to their unique keys
		private val replacementsBuilder = OptimizedIndexedSeq.newBuilder[(Any, E)]
		
		
		// IMPLEMENTED  -----------------------
		
		override def handleMatch(key: Any, newItem: In, existingItem: E): Boolean = {
			// Case: Replacement => Remembers the old version, as well as its key
			if (shouldReplace(newItem, existingItem)) {
				replacementsBuilder += (key -> existingItem)
				true
			}
			else
				false
		}
		
		override def replace(inserted: MapAccess[Any, S])(implicit connection: Connection): Unit = {
			// Performs the replacements. Safely assumes that at least one replacement was prepared.
			replace(replacementsBuilder.result().map { case (key, existing) => inserted(key) -> existing },
				connection)
			replacementsBuilder.clear()
		}
	}
	private class MappingReplaceHandler[-In, M, E, -S](delegate: ReplaceHandler[M, E, S], f: In => M)
		extends ReplaceHandler[In, E, S]
	{
		override def handleMatch(key: Any, newItem: In, existingItem: E): Boolean =
			delegate.handleMatch(key, f(newItem), existingItem)
		
		override def replace(inserted: MapAccess[Any, S])(implicit connection: Connection): Unit =
			delegate.replace(inserted)
	}
}

/**
 * Common trait for interfaces which handle item replacement / deprecation.
 *
 * ReplaceHandlers are expected to be stateful,
 * but to reset their state after the replace operation has been performed.
 *
 * @tparam In Type of new items
 * @tparam E Type of existing items
 * @author Mikko Hilpinen
 * @since 03.08.2025, v2.0
 */
trait ReplaceHandler[-In, -E, -S]
{
	// ABSTRACT --------------------------
	
	/**
	 * Checks whether a new item is to be considered a duplicate or a new version of an existing item.
	 * Prepares the replacement, if appropriate.
	 * @param key A unique key common to both items
	 * @param newItem The item proposed to be stored
	 * @param existingItem The item that already existed in the DB
	 * @return Whether 'newItem' should be considered an updated version of 'existingItem'.
	 *         False if 'newItem' should be considered a duplicate and ignored.
	 */
	def handleMatch(key: Any, newItem: In, existingItem: E): Boolean
	
	/**
	 * Performs the prepared replacements.
	 * Only called if replacements have been prepared, i.e. if 'handleMatch' yielded true at least once.
	 * @param inserted Access to the inserted items based on their unique keys.
	 *
	 *                 Note: If no value is queried, the insert will be performed AFTER this replace function call.
	 *                       On the other hand, if a value is requested, the insert will be performed immediately.
	 *                       ReplaceHandler implementations may use this property to control when the inserts
	 *                       should be performed. Typically, the optimal approach would be not to use this
	 *                       property, unless necessary.
	 *
	 * @param connection Implicit DB connection
	 */
	def replace(inserted: MapAccess[Any, S])(implicit connection: Connection): Unit
	
	
	// OTHER    ------------------------
	
	/**
	 * @param f A mapping function applied to the stored items, before they're passed to this handler
	 * @tparam I2 Type of mapping results
	 * @return A copy of this handler, which accepts the items before mapping them
	 */
	def mapInput[I2](f: I2 => In): ReplaceHandler[I2, E, S] = new MappingReplaceHandler[I2, In, E, S](this, f)
}
package utopia.vault.database

import utopia.vault.database.ReplaceHandler.DeprecatingReplaceHandler
import utopia.vault.nosql.targeting.columns.AccessColumnValue
import utopia.vault.nosql.view.{TimeDeprecatableView, ViewManyByIntIds}
import utopia.vault.store.HasId

object CanUseReplaceHandler
{
	// EXTENSIONS   --------------------
	
	implicit class CanUseReplaceHandlerWithIds[+In, E, +S <: HasId[Int], +A](val user: CanUseReplaceHandler[In, E, S, A])
		extends AnyVal
	{
		/**
		 * @param shouldReplace A function that accepts the proposed item and the item that already exists in the DB.
		 *                          - Yields true if the new item should be considered a new version
		 *                            and replace the existing item.
		 *                          - Yields false if the new item should be considered a duplicate
		 *                            and ignored / not inserted to the DB.
		 * @param accessReplacingIdColumn A function which receives an existing item that will be replaced,
		 *                                and yields access to that item's replacing ID -column.
		 * @return A copy of this item applying ID-based replacement logic
		 */
		def replacingIds(shouldReplace: (In, E) => Boolean)(accessReplacingIdColumn: E => AccessColumnValue[_, _, Int]) =
			user.using(ReplaceHandler.applyReplacingIds(shouldReplace)(accessReplacingIdColumn))
	}
}

/**
 * Common trait for store interfaces which support the application of replace-handlers
 * @tparam In Type of accepted input
 * @tparam E Type of the existing (potentially replaced) items
 * @tparam Repr Implementing type
 * @author Mikko Hilpinen
 * @since 03.08.2025, v2.0
 */
trait CanUseReplaceHandler[+In, E, +S, +Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @param handler A new replace-handler to perform replacements with
	 * @return A copy of this interface, which uses the specified replace-handler
	 */
	def using(handler: ReplaceHandler[In, E, S]): Repr
	
	
	// OTHER    -----------------------
	
	/**
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
	 * @return Copy of this interface, which deprecates older item versions by using the specified replace function
	 */
	def replacing(shouldReplace: (In, E) => Boolean)(replace: (IndexedSeq[(S, E)], Connection) => Unit) =
		using(ReplaceHandler(shouldReplace)(replace))
	
	/**
	 * @param rootAccess Root-level access to the matching database entries.
	 *                   Used for deprecating older item versions. Supports timestamp-based deprecation.
	 * @param shouldReplace A function that accepts the proposed item and the item that already exists in the DB.
	 *                          - Yields true if the new item should be considered a new version
	 *                            and replace the existing item.
	 *                          - Yields false if the new item should be considered a duplicate
	 *                            and ignored / not inserted to the DB.
	 * @return Copy of this interface, which deprecates older item versions by specifying a deprecation timestamp
	 */
	def deprecating(rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])(shouldReplace: (In, E) => Boolean)
	               (implicit ev: E <:< HasId[Int]) =
		using(new DeprecatingReplaceHandler(rootAccess)(shouldReplace)(_.id))
	
	/**
	 * @param rootAccess Root-level access to the matching database entries.
	 *                   Used for deprecating older item versions. Supports timestamp-based deprecation.
	 * @param shouldReplace A function that accepts the proposed item (value) and the item that already exists in the DB.
	 *                          - Yields true if the new item should be considered a new version
	 *                            and replace the existing item.
	 *                          - Yields false if the new item should be considered a duplicate
	 *                            and ignored / not inserted to the DB.
	 * @return Copy of this interface, which deprecates older item versions by specifying a deprecation timestamp
	 */
	def deprecatingValues[Val](rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])
	                          (shouldReplace: (Val, E) => Boolean)
	                          (implicit evIn: In <:< (_, Val), evEx: E <:< HasId[Int]) =
		using(new DeprecatingReplaceHandler(rootAccess)(shouldReplace)(_.id).mapInput { _._2 })
	/**
	 * @param shouldReplace A function that accepts the proposed item (value) and the item that already exists in the DB.
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
	 * @tparam Val Type of the key-mapped input values
	 * @return Copy of this interface, which deprecates older item versions by using the specified replace function
	 */
	def replacingValues[Val](shouldReplace: (Val, E) => Boolean)(replace: (IndexedSeq[(S, E)], Connection) => Unit)
	                        (implicit ev: In <:< (_, Val)) =
		using(ReplaceHandler(shouldReplace)(replace).mapInput { _._2 })
}
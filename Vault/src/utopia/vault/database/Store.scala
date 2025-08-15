package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{IntSet, OptimizedIndexedSeq, Pair}
import utopia.flow.collection.template.MapAccess
import utopia.flow.operator.Identity
import utopia.flow.operator.MaybeEmpty.collectionMayBeEmpty
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.Extender
import utopia.vault.database.Store.ReplaceHandler
import utopia.vault.database.Store.ReplaceHandler.MappingReplaceHandler
import utopia.vault.nosql.view.{TimeDeprecatableView, ViewManyByIntIds}
import utopia.vault.store.{HasId, StoreResult}

import scala.collection.View

/**
  * Provides simple interfaces for facilitating duplicate-aware insertion logic, a.k.a. storing.
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
object Store
{
	// OTHER    ----------------------
	
	/**
	 * @param model Interface used for inserting new items to the database
	 * @tparam D Type of inserted data
	 * @tparam S Type of stored instances
	 * @return A factory for constructing store interfaces which use the specified model
	 */
	def apply[D, S <: HasId[Int] with Extender[D]](model: Inserter[D, S]) = new PreparedStoreFactory[D, S](model)
	
	/**
	 * @param model Interface used for inserting new items to the database
	 * @param toData A function which converts the accepted input into insertable data
	 * @tparam V Type of accepted input
	 * @tparam D Type of stored data
	 * @tparam S Type of the items that have already been stored to the DB
	 * @return An interface for storing items to the DB
	 */
	def using[V, D, S <: HasId[Int]](model: Inserter[D, S])(toData: V => D) = new PreparedStore[V, D, S](model)(toData)
	/**
	 * @param model Interface used for inserting new items to the database
	 * @tparam D Type of stored data
	 * @tparam S Type of the items that have already been stored to the DB
	 * @return An interface for storing prepared data to the DB
	 */
	def dataUsing[D, S <: HasId[Int] with Extender[D]](model: Inserter[D, S]) = new PreparedStoreData[D, S](model)
	/**
	 * @param model Interface used for inserting new items to the database
	 * @param toData A function which converts the accepted input (key + value) into insertable data
	 * @tparam K Type of keys attached to the stored values
	 * @tparam V Type of stored values
	 * @tparam D Type of stored / prepared data
	 * @tparam S Type of the items that have already been stored to the DB
	 * @return An interface for storing items to the DB
	 */
	 def keyMappedUsing[K, V, D, S <: HasId[Int]](model: Inserter[D, S])(toData: (K, V) => D) =
		 new PreparedKeyMappedStore[K, V, D, S](model, None)(toData)
	
	
	// NESTED   ---------------------------
	
	class PreparedStoreFactory[D, S <: HasId[Int] with Extender[D]](model: Inserter[D, S])
	{
		// COMPUTED ---------------------------
		
		/**
		 * @return An interface for storing prepared data to the DB
		 */
		def data = new PreparedStoreData[D, S](model)
		
		
		// OTHER    ---------------------------
		
		/**
		 * @param toData A function which converts the accepted input into insertable data
		 * @tparam V Type of accepted input
		 * @return An interface for storing items to the DB
		 */
		def apply[V](toData: V => D) = new PreparedStore[V, D, S](model)(toData)
		/**
		 * @param toData A function which converts the accepted input (key + value) into insertable data
		 * @tparam K Type of keys attached to the stored values
		 * @tparam V Type of stored values
		 * @return An interface for storing items to the DB
		 */
		def keyMapped[K, V](toData: (K, V) => D) = new PreparedKeyMappedStore[K, V, D, S](model, None)(toData)
	}
	
	class PreparedStore[V, -D, S <: HasId[Int]](model: Inserter[D, S],
	                                            replaceHandler: Option[ReplaceHandler[V, S]] = None)
	                                           (toData: V => D)
		extends Store[V, S, PreparedStore[V, D, S]]
	{
		// IMPLEMENTED  ---------------------------
		
		override def using(handler: ReplaceHandler[V, S]): PreparedStore[V, D, S] =
			new PreparedStore(model, Some(handler))(toData)
		
		override def single(item: V, existingMatch: Option[S])(implicit connection: Connection) =
			existingMatch match {
				// Case: There already existed a new item => Performs the replacement check, if appropriate
				case Some(existing) =>
					replaceHandler match {
						// Case: Replacement may be utilized => Checks whether the item is a new version or a duplicate
						case Some(replacer) =>
							// Case: The item is a new version => Replaces the old version with it
							if (replacer.handleMatch(item, item, existing)) {
								val lazyInserted = Lazy { model.insert(toData(item)) }
								replacer.replace(MapAccess { _ => lazyInserted.value })
								StoreResult.inserted(lazyInserted.value)
							}
							// Case: Duplicate => Yields the existing item
							else
								StoreResult.existed(existing)
						
						// Case: Replacement is not used
						//       => Considers the new item a duplicate and yields the existing item
						case None => StoreResult.existed(existing)
					}
				// Case: No existing match => Inserts the item
				case None => StoreResult.inserted(model.insert(toData(item)))
			}
		
		override def keyMapped[K](itemsToStore: IterableOnce[(K, V)], existingItems: Map[K, S])
		                (implicit connection: Connection) =
		{
			val existingResultView = existingItems.view.mapValues(StoreResult.existed)
			itemsToStore.nonEmptyIterator match {
				case Some(itemsIterator) =>
					// Checks against the existing items,
					// preparing the non-duplicates as new inserts and handling possible replacements
					val insertsBuilder = OptimizedIndexedSeq.newBuilder[(K, V)]
					itemsIterator.foreach { case (key, item) =>
						existingItems.get(key) match {
							// Case: Matches an existing item
							//       => Handles it using the replace-handler, if appropriate.
							//          If no handler has been specified,
							//          treats the item as a duplicate and won't insert it.
							case Some(existing) =>
								if (replaceHandler.exists { _.handleMatch(key, item, existing) })
									insertsBuilder += (key -> item)
							
							// Case: A non-duplicate item => Prepares to insert it
							case None => insertsBuilder += (key -> item)
						}
					}
					
					insertsBuilder.result().notEmpty match {
						// Case: At least one insert is necessary
						case Some(inserts) =>
							// Performs the inserts before or after the replacement
							val lazyInserted = Lazy {
								model.insertFrom(inserts) { case (_, item) => toData(item) } {
									case (inserted, (key, _)) => key -> inserted }
							}
							// Performs the replacement, if appropriate
							replaceHandler.foreach { replacer =>
								val lazyInsertedMap = lazyInserted.map { _.toMap[Any, S] }
								replacer.replace(MapAccess.wrapLazily[Any, S](lazyInsertedMap.value))
							}
							// Converts the insertion results into store-results
							// If the insert was not performed yet, it is performed here
							// Combines the existing and inserted results into a map
							View.concat(
								existingResultView,
								lazyInserted.value.view.map { case (key, inserted) =>
									key -> StoreResult.inserted(inserted)
								}
							).toMap
						
						// Case: None of the specified items were new => Returns the existing items
						case None => existingResultView.toMap
					}
					
				// Case: There were no items to store => Yields the existing items
				case None => existingResultView.toMap
			}
		}
	}
	class PreparedStoreData[D, S <: Extender[D] with HasId[Int]](model: Inserter[D, S],
	                                                             replaceHandler: Option[ReplaceHandler[D, S]] = None)
		extends PreparedStore[D, D, S](model, replaceHandler)(Identity) with Store[D, S, PreparedStoreData[D, S]]
	{
		// IMPLEMENTED  ------------------------
		
		override def using(handler: ReplaceHandler[D, S]): PreparedStoreData[D, S] =
			new PreparedStoreData[D, S](model, Some(handler))
		
		
		// OTHER    ----------------------------
		
		/**
		 * Maps the items into unique keys and uses that mapping to check for duplicates
		 * @param itemsToStore Items that should be inserted, unless they're duplicates
		 * @param existingItems Potentially matching items that already exist in the database
		 * @param dataToKey A function that converts the data form of an item into a unique key
		 * @param connection Implicit DB connection
		 * @tparam K Type of keys used
		 * @return A map where each encountered key is mapped to the stored item (either existing or inserted)
		 */
		def keyMap[K](itemsToStore: IterableOnce[D], existingItems: IterableOnce[S])(dataToKey: D => K)
		             (implicit connection: Connection) =
			super.keyMap[K](itemsToStore, existingItems) { dataToKey(_) } { e => dataToKey(e.wrapped) }
	}
	class PreparedKeyMappedStore[K, V, -D, S <: HasId[Int]](model: Inserter[D, S],
	                                                       replaceHandler: Option[ReplaceHandler[(K, V), S]])
	                                                      (toData: (K, V) => D)
		extends PreparedStore[(K, V), D, S](model, replaceHandler)({ case (key, value) => toData(key, value) })
			with Store[(K, V), S, PreparedKeyMappedStore[K, V, D, S]]
	{
		// IMPLEMENTED  ---------------------
		
		override def using(handler: ReplaceHandler[(K, V), S]): PreparedKeyMappedStore[K, V, D, S] =
			new PreparedKeyMappedStore[K, V, D, S](model, Some(handler))(toData)
			
		
		// OTHER    -------------------------
		
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
		def deprecatingValues(rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])(shouldReplace: (V, S) => Boolean) =
			using(ReplaceHandler.deprecating(rootAccess)(shouldReplace).mapInput { _._2 })
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
		 * @return Copy of this interface, which deprecates older item versions by using the specified replace function
		 */
		def replacingValues(shouldReplace: (V, S) => Boolean)(replace: (IndexedSeq[Pair[S]], Connection) => Unit) =
			using(ReplaceHandler(shouldReplace)(replace).mapInput { _._2 })
		
		/**
		 * @param itemsToStore Items to store, including unique keys
		 * @param existingItems Existing DB entries
		 * @param existingToKey A function which converts an existing item into a unique key
		 * @param connection Implicit DB connection
		 * @return A map where each encountered key is mapped to the stored item (either existing or inserted)
		 */
		def apply(itemsToStore: IterableOnce[(K, V)], existingItems: IterableOnce[S])(existingToKey: S => K)
		         (implicit connection: Connection) =
			keyMap(itemsToStore, existingItems) { _._1 }(existingToKey)
		
		/**
		 * Stores items after mapping them to keys and values
		 * @param itemsToStore The items to store
		 * @param existingItems Existing DB entries
		 * @param itemToKey A function which maps an item to a unique key
		 * @param itemToValue A function that converts an item to a value to store
		 * @param existingToKey A function which maps an existing database entry into a unique key
		 * @param connection Implicit DB connection
		 * @tparam A Type of accepted items
		 * @return A map where each encountered key is mapped to the stored item (either existing or inserted)
		 */
		def keyValueMap[A](itemsToStore: IterableOnce[A], existingItems: IterableOnce[S])
		                  (itemToKey: A => K)(itemToValue: A => V)(existingToKey: S => K)
		                  (implicit connection: Connection) =
		{
			apply(itemsToStore.iterator.map { a => itemToKey(a) -> itemToValue(a) }, existingItems)(existingToKey)
		}
	}
	
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
		 * @tparam V Type of new values
		 * @tparam S Type of existing DB entries
		 * @return A new replace-handler, which deprecates by updating a timestamp column
		 */
		def deprecating[V, S <: HasId[Int]](rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])
		                                   (shouldReplace: (V, S) => Boolean) =
			new DeprecatingReplaceHandler[V, S](rootAccess)(shouldReplace)
		
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
		 * @tparam V Type of new items, before they've been inserted
		 * @tparam S Type of items after they've been inserted
		 * @return A new replace-handler
		 */
		def apply[V, S](shouldReplace: (V, S) => Boolean)
		               (replace: (IndexedSeq[Pair[S]], Connection) => Unit): ReplaceHandler[V, S] =
			new _ReplaceHandler[V, S](shouldReplace)(replace)
		
		
		// NESTED   -------------------------
		
		class DeprecatingReplaceHandler[-V, -S <: HasId[Int]](rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])
		                                                     (shouldReplace: (V, S) => Boolean)
			extends ReplaceHandler[V, S]
		{
			// ATTRIBUTES   -----------------
			
			// Collects the IDs which need to be deprecated
			private val idsToReplaceBuilder = IntSet.newBuilder
			
			
			// IMPLEMENTED  -----------------
			
			override def handleMatch(key: Any, newItem: V, existingItem: S): Boolean = {
				// Case: Replacement => Remembers the old version's ID
				if (shouldReplace(newItem, existingItem)) {
					idsToReplaceBuilder += existingItem.id
					true
				}
				else
					false
			}
			
			override def replace(inserted: MapAccess[Any, S])(implicit connection: Connection): Unit = {
				// Deprecates the rows matching the collected IDs
				rootAccess(idsToReplaceBuilder.result()).deprecate()
				idsToReplaceBuilder.clear()
			}
		}
		
		private class _ReplaceHandler[-V, S](shouldReplace: (V, S) => Boolean)
		                                   (replace: (IndexedSeq[Pair[S]], Connection) => Unit)
			extends ReplaceHandler[V, S]
		{
			// ATTRIBUTES   -----------------------
			
			// Collects the items to replace, mapped to their unique keys
			private val replacementsBuilder = OptimizedIndexedSeq.newBuilder[(Any, S)]
			
			
			// IMPLEMENTED  -----------------------
			
			override def handleMatch(key: Any, newItem: V, existingItem: S): Boolean = {
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
				replace(replacementsBuilder.result().map { case (key, existing) => Pair(inserted(key), existing) },
					connection)
				replacementsBuilder.clear()
			}
		}
		private class MappingReplaceHandler[-I, V, S](delegate: ReplaceHandler[V, S], f: I => V)
			extends ReplaceHandler[I, S]
		{
			override def handleMatch(key: Any, newItem: I, existingItem: S): Boolean =
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
	 * @tparam V Type of new items
	 * @tparam S Type of existing items
	 */
	trait ReplaceHandler[-V, -S]
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
		def handleMatch(key: Any, newItem: V, existingItem: S): Boolean
		
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
		 * @tparam V2 Type of mapping results
		 * @return A copy of this handler, which accepts the items before mapping them
		 */
		def mapInput[V2](f: V2 => V): ReplaceHandler[V2, S] = new MappingReplaceHandler[V2, V, S](this, f)
	}
}

/**
 * A prepared interface for performing various store functions
 * @tparam V Type of accepted input / items to store
 * @tparam S Type of existing database entries / inserted items
 * @tparam Repr Type of this store interface
 */
trait Store[V, S <: HasId[Int], +Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @param handler A new replace-handler to perform replacements with
	 * @return A copy of this interface, which uses the specified replace-handler
	 */
	def using(handler: ReplaceHandler[V, S]): Repr
	
	/**
	 * Stores an individual item to the database.
	 * If the specified item was a duplicate entry, no insertion is performed.
	 * @param item The item to store
	 * @param existingMatch An existing matching entry from the database. None if there was no matching entry.
	 * @param connection Implicit DB connection
	 * @return Result of this store operation, either containing the inserted entry, or 'existingMatch'.
	 */
	def single(item: V, existingMatch: Option[S])(implicit connection: Connection): StoreResult[S]
	
	/**
	 * Stores 0-n items to the database. Checks against existing data and won't insert any duplicate entries.
	 * @param itemsToStore The items that should be stored to the DB.
	 *                     Each item is mapped to a unique key,
	 *                     which is used for matching it with a possible existing DB entry.
	 * @param existingItems Matching items from the database. These, also, are mapped to unique (matching) keys.
	 * @param connection Implicit DB connection
	 * @tparam K type of keys used
	 * @return A map where keys are the specified keys and values are store results,
	 *         either containing a newly inserted item, or one of the existing DB entries.
	 */
	def keyMapped[K](itemsToStore: IterableOnce[(K, V)], existingItems: Map[K, S])
	                (implicit connection: Connection): Map[K, StoreResult[S]]
	
	
	// OTHER    ------------------------
	
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
	def deprecating(rootAccess: ViewManyByIntIds[TimeDeprecatableView[_]])(shouldReplace: (V, S) => Boolean) =
		using(ReplaceHandler.deprecating(rootAccess)(shouldReplace))
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
	def replacing(shouldReplace: (V, S) => Boolean)(replace: (IndexedSeq[Pair[S]], Connection) => Unit) =
		using(ReplaceHandler(shouldReplace)(replace))
	
	/**
	 * Inserts a new item to the database. Assumes that no matching entry exists in the database.
	 * @param item The item to insert
	 * @param connection Implicit DB connection
	 * @return The inserted item
	 */
	def unique(item: V)(implicit connection: Connection) = single(item, None)
	/**
	 * Inserts 0-n items to the database. Assumes that no matching entries exist in the database.
	 * @param items The items to insert
	 * @param connection Implicit DB connection
	 * @return The inserted items (not necessarily in the same order as specified).
	 */
	def unique(items: IterableOnce[V])(implicit connection: Connection) =
		keyMapped(items.iterator.map { item => item -> item }, Map.empty[V, S]).values
	
	/**
	 * Stores an individual item to the database.
	 * If the specified item was a duplicate entry, no insertion is performed.
	 * @param item The item to store
	 * @param existingMatch An existing matching entry from the database.
	 * @param connection Implicit DB connection
	 * @return Result of this store operation, either containing the inserted entry, or 'existingMatch'.
	 */
	def single(item: V, existingMatch: S)(implicit connection: Connection): StoreResult[S] =
		single(item, Some(existingMatch))
	
	/**
	 * Stores 0-n items to the database. Checks against existing data and won't insert any duplicate entries.
	 * @param itemsToStore Items to store
	 * @param existingItems Matching items from the database.
	 * @param itemToKey A function that accepts an item to store and maps it to a unique key
	 * @param existingToKey A function that accepts an existing database entry and maps it to a unique key similar
	 *                      to what 'itemToKey' would produce.
	 * @param connection Implicit DB connection
	 * @tparam K Type of keys used
	 * @return A map where keys are the specified keys and values are store results,
	 *         either containing a newly inserted item, or one of the existing DB entries.
	 */
	def keyMap[K](itemsToStore: IterableOnce[V], existingItems: IterableOnce[S])
	             (itemToKey: V => K)(existingToKey: S => K)(implicit connection: Connection) =
		keyMapped[K](itemsToStore.iterator.map { item => itemToKey(item) -> item },
			existingItems.iterator.map { existing => existingToKey(existing) -> existing }.toMap)
}
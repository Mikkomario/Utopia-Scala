package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.template.MapAccess
import utopia.flow.operator.Identity
import utopia.flow.operator.MaybeEmpty.collectionMayBeEmpty
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.Extender
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
	def apply[D, S <: HasId[Int] with Extender[D]](model: Inserter[D, S]) = new StoreFactory[D, S](model)
	
	/**
	 * @param model Interface used for inserting new items to the database
	 * @param toData A function which converts the accepted input into insertable data
	 * @tparam In Type of accepted input
	 * @tparam D Type of stored data
	 * @tparam S Type of the items that have already been stored to the DB
	 * @return An interface for storing items to the DB
	 */
	def using[In, D, S <: HasId[Int]](model: Inserter[D, S])(toData: In => D) =
		new PreparedStore[In, D, S](model)(toData)
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
	
	/**
	 * Prepares an advanced store factory
	 * @param model DB model to use in data inserts
	 * @param toData A function which converts input to insertable data
	 * @tparam In Type of accepted input / items-to-store
	 * @tparam D Type of inserted data
	 * @tparam S Type of stored items (after insert)
	 * @return A factory for creating the store interface
	 */
	def advancedUsing[In, D, S](model: Inserter[D, S])(toData: In => D) =
		new AdvancedStoreFactory[In, D, S](model)(toData)
	
	
	// NESTED   ---------------------------
	
	class StoreFactory[D, S <: HasId[Int] with Extender[D]](model: Inserter[D, S])
	{
		// COMPUTED ---------------------------
		
		/**
		 * @return An interface for storing prepared data to the DB
		 */
		def data = new PreparedStoreData[D, S](model)
		
		
		// OTHER    ---------------------------
		
		/**
		 * @param toData A function which converts the accepted input into insertable data
		 * @tparam In Type of accepted input
		 * @return An interface for storing items to the DB
		 */
		def apply[In](toData: In => D) = new PreparedStore[In, D, S](model)(toData)
		/**
		 * @param toData A function which converts the accepted input (key + value) into insertable data
		 * @tparam K Type of keys attached to the stored values
		 * @tparam V Type of stored values
		 * @return An interface for storing items to the DB
		 */
		def keyMapped[K, V](toData: (K, V) => D) = new PreparedKeyMappedStore[K, V, D, S](model, None)(toData)
		
		/**
		 * @param toData A function which converts accepted input into insertable data
		 * @tparam In Type of accepted input / items-to-store
		 * @return A factory for constructing more complex store interfaces
		 */
		def advanced[In](toData: In => D) = new AdvancedStoreFactory[In, D, S](model)(toData)
		
		/**
		 * @param toData A function for converting values into data.
		 *               Accepts:
		 *                  1. Value to convert
		 *                  1. A store construction parameter
		 * @tparam In Type of the converted values
		 * @tparam GN Type of the additional construction parameter
		 * @return A new factory for creating store interfaces, using construction parameters
		 */
		def generic[In, GN](toData: (In, GN) => D) = new GenericStoreFactory[In, D, S, GN](model, None)(toData)
		/**
		 * @param toData A function for converting key-value pairs into data.
		 *               Accepts:
		 *                  1. A unique key
		 *                  1. Value linked to that key
		 *                  1. A store construction parameter
		 * @tparam K Type of the unique keys used
		 * @tparam V Type of the converted values
		 * @tparam GN Type of the additional construction parameter
		 * @return A new factory for creating store interfaces, using construction parameters
		 */
		def genericKeyMapped[K, V, GN](toData: (K, V, GN) => D) =
			new GenericKeyMapStoreFactory[K, V, D, S, GN](model, None)(toData)
	}
	
	class AdvancedStoreFactory[In, -D, S](model: Inserter[D, S])(toData: In => D)
	{
		/**
		 * Creates a store interface, which wraps inserted values
		 * @param wrapInserted A function which converts an inserted item
		 *                     to a type comparable with that of the existing items
		 * @tparam E Type of the accepted existing items
		 * @return A new store interface
		 */
		//noinspection ConvertibleToMethodValue
		def mapInserted[E <: HasId[Int]](wrapInserted: (In, S) => E) =
			apply[E, StoreResult[E]] { (input, inserted) => StoreResult.inserted(wrapInserted(input, inserted)) } {
				StoreResult.existed(_) }
		/**
		 * Creates a store interface, which wraps existing values to another type
		 * @param wrapExisting A function which converts a pre-existing item to a stored item
		 * @tparam E Type of the accepted existing items
		 * @return A new store interface
		 */
		def mapExisting[E](wrapExisting: E => S)(implicit ev: S <:< HasId[Int]) =
			apply[E, StoreResult[S]] { (_, i) => StoreResult(i, i.id, isNew = true) } { e =>
				val wrapped = wrapExisting(e)
				StoreResult(wrapped, wrapped.id, isNew = false)
			}
		
		/**
		 * Creates a new custom-mapping store
		 * @param insertedToResult A function which converts an inserted item into a result.
		 *                         Receives both the original input, and the inserted item.
		 * @param existingToResult A function which converts a pre-existing item into a result
		 * @tparam E Type of the accepted existing items
		 * @tparam R Type of generated store results
		 * @return A new store interface
		 */
		def apply[E, R](insertedToResult: (In, S) => R)(existingToResult: E => R) =
			new AdvancedPreparedStore[In, E, D, S, R](model)(toData)(insertedToResult)(existingToResult)
	}
	
	trait GenericStoreFactoryLike[+In, E, +S, -GN, +ST, +Repr] extends CanUseReplaceHandler[In, E, S, Repr]
	{
		// ABSTRACT    ----------------------------
		
		/**
		 * @param param A store construction parameter
		 * @return A store interface, based on that parameter
		 */
		def apply(param: GN): ST
		
		
		// COMPUTED -------------------------------
		
		/**
		 * @param param A store construction parameter (implicit)
		 * @return A store interface, based on the implicit parameter
		 */
		def contextual(implicit param: GN) = apply(param)
	}
	class GenericStoreFactory[In, -D, S <: HasId[Int], -GN](model: Inserter[D, S],
	                                                        replaceHandler: Option[ReplaceHandler[In, S, S]])
	                                                       (toData: (In, GN) => D)
		extends GenericStoreFactoryLike[In, S, S, GN, PreparedStore[In, D, S], GenericStoreFactory[In, D, S, GN]]
	{
		// IMPLEMENTED  ---------------------------
		
		override def apply(param: GN) =
			new PreparedStore[In, D, S](model, replaceHandler)({ value => toData(value, param) })
		
		override def using(handler: ReplaceHandler[In, S, S]): GenericStoreFactory[In, D, S, GN] =
			new GenericStoreFactory[In, D, S, GN](model, Some(handler))(toData)
	}
	class GenericKeyMapStoreFactory[K, V, -D, S <: HasId[Int], -GN](model: Inserter[D, S],
	                                                                replaceHandler: Option[ReplaceHandler[(K, V), S, S]])
	                                                               (toData: (K, V, GN) => D)
		extends GenericStoreFactoryLike[(K, V), S, S, GN, PreparedKeyMappedStore[K, V, D, S], GenericKeyMapStoreFactory[K, V, D, S, GN]]
	{
		// IMPLEMENTED  ----------------------------
		
		override def apply(param: GN): PreparedKeyMappedStore[K, V, D, S] =
			new PreparedKeyMappedStore[K, V, D, S](model, replaceHandler)({ (key, value) => toData(key, value, param) })
		
		override def using(handler: ReplaceHandler[(K, V), S, S]): GenericKeyMapStoreFactory[K, V, D, S, GN] =
			new GenericKeyMapStoreFactory[K, V, D, S, GN](model, Some(handler))(toData)
	}
	
	abstract class AbstractStore[In, E, -D, S, +R, +Repr](model: Inserter[D, S],
	                                                      replaceHandler: Option[ReplaceHandler[In, E, S]] = None)
	                                                     (toData: In => D)
		extends Store[In, E, S, R, Repr]
	{
		// ABSTRACT -------------------------------
		
		/**
		 * Wraps an inserted item as a store result
		 * @param inserted The inserted item
		 * @return A store result wrapping the inserted item
		 */
		protected def insertedResult(input: In, inserted: S): R
		/**
		 * Wraps an existing item as a store result
		 * @param existing The existing item
		 * @return A store result wrapping the specified item
		 */
		protected def existingResult(existing: E): R
		
		
		// IMPLEMENTED  ---------------------------
		
		override def single(item: In, existingMatch: Option[E])(implicit connection: Connection) =
			existingMatch match {
				// Case: There already existed a new item => Performs the replacement check, if appropriate
				case Some(existing) =>
					replaceHandler match {
						// Case: Replacement may be utilized => Checks whether the item is a new version or a duplicate
						case Some(replacer) =>
							replacer.handleMatch(item, item, existing) match {
								case Left(isNewVersion) =>
									// Case: The item is a new version => Replaces the old version with it
									if (isNewVersion) {
										val lazyInserted = Lazy { model.insert(toData(item)) }
										replacer.replace(MapAccess { _ => lazyInserted.value })
										insertedResult(item, lazyInserted.value)
									}
									// Case: Duplicate => Yields the existing item
									else
										existingResult(existing)
								
								// Case: An update was performed => Yields the modified version of the existing item
								case Right(updated) => existingResult(updated)
							}
						// Case: Replacement is not used
						//       => Considers the new item a duplicate and yields the existing item
						case None => existingResult(existing)
					}
				// Case: No existing match => Inserts the item
				case None => insertedResult(item, model.insert(toData(item)))
			}
		
		//noinspection ConvertibleToMethodValue
		override def keyMapped[K](itemsToStore: IterableOnce[(K, In)], existingItems: => Map[K, E])
		                         (implicit connection: Connection) =
			itemsToStore.nonEmptyIterator match {
				case Some(itemsIterator) =>
					val cachedExisting = existingItems
					// Checks against the existing items,
					// preparing the non-duplicates as new inserts and handling possible replacements & updates
					val updatedBuilder = OptimizedIndexedSeq.newBuilder[(K, E)]
					itemsIterator
						.filter { case (key, item) =>
							// Checks whether this item matches an existing version
							cachedExisting.get(key).forall { existing =>
								// Case: Matches an existing item
								//       => Handles it using the replace-handler, if appropriate.
								//          If no handler has been specified,
								//          treats the item as a duplicate and won't insert it.
								replaceHandler match {
									case Some(replaceHandler) =>
										replaceHandler.handleMatch(key, item, existing).leftOrMap { modified =>
											updatedBuilder += (key -> modified)
											false
										}
									case None => true
								}
							}
						}
						.toOptimizedSeq.notEmpty match
					{
						// Case: At least one insert is necessary
						case Some(inserts) =>
							// Performs the inserts before or after the replacement
							val lazyInserted = Lazy {
								model.insertFrom(inserts) { case (_, item) => toData(item) } {
									case (inserted, (key, item)) => (key, item, inserted) }
							}
							// Performs the replacement, if appropriate
							replaceHandler.foreach { replacer =>
								val lazyInsertedMap = lazyInserted
									.map { _.iterator.map { case (key, _, inserted) => key -> inserted }.toMap[Any, S] }
								replacer.replace(MapAccess.wrap[Any, S](lazyInsertedMap))
							}
							// Applies the performed updates to the result map, if appropriate
							val existingResultView = (updatedBuilder.result().notEmpty match {
								case Some(updated) => (cachedExisting ++ updated).view
								case None => cachedExisting.view
							}).mapValues(existingResult)
							// Converts the insertion results into store-results
							// If the insert was not performed yet, it is performed here
							// Combines the existing and inserted results into a map
							View
								.concat(
									existingResultView,
									lazyInserted.value.view.map { case (key, item, inserted) =>
										key -> insertedResult(item, inserted)
									}
								)
								.toMap
						
						// Case: None of the specified items were new => Returns the existing items
						case None => cachedExisting.view.mapValues(existingResult).toMap
					}
				
				// Case: There were no items to store => Yields an empty map
				case None => Map[K, R]()
			}
	}
	
	abstract class AbstractSimpleStore[In, -D, S <: HasId[Int], +Repr](model: Inserter[D, S],
	                                                                   replaceHandler: Option[ReplaceHandler[In, S, S]] = None)
	                                                                  (toData: In => D)
		extends AbstractStore[In, S, D, S, StoreResult[S], Repr](model, replaceHandler)(toData)
	{
		override protected def insertedResult(input: In, inserted: S) = StoreResult.inserted(inserted)
		override protected def existingResult(existing: S) = StoreResult.existed(existing)
	}
	
	class PreparedStore[In, -D, S <: HasId[Int]](model: Inserter[D, S],
	                                             replaceHandler: Option[ReplaceHandler[In, S, S]] = None)
	                                            (toData: In => D)
		extends AbstractSimpleStore[In, D, S, PreparedStore[In, D, S]](model, replaceHandler)(toData)
	{
		// IMPLEMENTED  ---------------------------
		
		override def using(handler: ReplaceHandler[In, S, S]): PreparedStore[In, D, S] =
			new PreparedStore[In, D, S](model, Some(handler))(toData)
	}
	
	class PreparedStoreData[D, S <: Extender[D] with HasId[Int]](model: Inserter[D, S],
	                                                             replaceHandler: Option[ReplaceHandler[D, S, S]] = None)
		extends AbstractSimpleStore[D, D, S, PreparedStoreData[D, S]](model, replaceHandler)(Identity)
	{
		// IMPLEMENTED  ------------------------
		
		override def using(handler: ReplaceHandler[D, S, S]): PreparedStoreData[D, S] =
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
		def keyMap[K](itemsToStore: IterableOnce[D], existingItems: => IterableOnce[S])(dataToKey: D => K)
		             (implicit connection: Connection) =
			super.keyMap[K](itemsToStore, existingItems) { dataToKey(_) } { e => dataToKey(e.wrapped) }
	}

	class PreparedKeyMappedStore[K, V, -D, S <: HasId[Int]](model: Inserter[D, S],
	                                                        replaceHandler: Option[ReplaceHandler[(K, V), S, S]])
	                                                       (toData: (K, V) => D)
		extends AbstractSimpleStore[(K, V), D, S, PreparedKeyMappedStore[K, V, D, S]](model, replaceHandler)(
			{ case (key, value) => toData(key, value) })
	{
		// IMPLEMENTED  ---------------------
		
		override def using(handler: ReplaceHandler[(K, V), S, S]): PreparedKeyMappedStore[K, V, D, S] =
			new PreparedKeyMappedStore[K, V, D, S](model, Some(handler))(toData)
			
		
		// OTHER    -------------------------
		
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
		def keyValueMap[A](itemsToStore: IterableOnce[A], existingItems: => IterableOnce[S])
		                  (itemToKey: A => K)(itemToValue: A => V)(existingToKey: S => K)
		                  (implicit connection: Connection) =
			apply(itemsToStore.iterator.map { a => itemToKey(a) -> itemToValue(a) }, existingItems)(existingToKey)
		/**
		 * @param itemsToStore Items to store, including unique keys
		 * @param existingItems Existing DB entries
		 * @param existingToKey A function which converts an existing item into a unique key
		 * @param connection Implicit DB connection
		 * @return A map where each encountered key is mapped to the stored item (either existing or inserted)
		 */
		def apply(itemsToStore: IterableOnce[(K, V)], existingItems: => IterableOnce[S])(existingToKey: S => K)
		         (implicit connection: Connection) =
			keyMap(itemsToStore, existingItems) { _._1 }(existingToKey)
	}
	
	class AdvancedPreparedStore[In, E, -D, S, +R](model: Inserter[D, S],
	                                              replaceHandler: Option[ReplaceHandler[In, E, S]] = None)
	                                             (toData: In => D)(wrapInsert: (In, S) => R)(wrapExisting: E => R)
		extends AbstractStore[In, E, D, S, R, AdvancedPreparedStore[In, E, D, S, R]](model, replaceHandler)(toData)
	{
		override protected def insertedResult(input: In, inserted: S): R = wrapInsert(input, inserted)
		override protected def existingResult(existing: E): R = wrapExisting(existing)
		
		override def using(handler: ReplaceHandler[In, E, S]): AdvancedPreparedStore[In, E, D, S, R] =
			new AdvancedPreparedStore[In, E, D, S, R](model, Some(handler))(toData)(wrapInsert)(wrapExisting)
	}
}

/**
 * A prepared interface for performing various store functions
 * @tparam In Type of accepted input / items to store
 * @tparam E Type of existing items
 * @tparam S Type of inserted items
 * @tparam R Type of store results
 * @tparam Repr Type of this store interface
 */
trait Store[In, E, +S, +R, +Repr] extends CanUseReplaceHandler[In, E, S, Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * Stores an individual item to the database.
	 * If the specified item was a duplicate entry, no insertion is performed.
	 * @param item The item to store
	 * @param existingMatch An existing matching entry from the database. None if there was no matching entry.
	 * @param connection Implicit DB connection
	 * @return Result of this store operation, either containing the inserted entry, or 'existingMatch'.
	 */
	def single(item: In, existingMatch: Option[E])(implicit connection: Connection): R
	
	/**
	 * Stores 0-n items to the database. Checks against existing data and won't insert any duplicate entries.
	 * @param itemsToStore The items that should be stored to the DB.
	 *                     Each item is mapped to a unique key,
	 *                     which is used for matching it with a possible existing DB entry.
	 * @param existingItems Matching items from the database. These, also, are mapped to unique (matching) keys.
	 *                      Call-by-name: Not called if there are no items to store.
	 * @param connection Implicit DB connection
	 * @tparam K type of keys used
	 * @return A map where keys are the specified keys and values are store results,
	 *         either containing a newly inserted item, or one of the existing DB entries.
	 */
	def keyMapped[K](itemsToStore: IterableOnce[(K, In)], existingItems: => Map[K, E])
	                (implicit connection: Connection): Map[K, R]
	
	
	// OTHER    ------------------------
	
	/**
	 * Stores an individual item to the database.
	 * If the specified item was a duplicate entry, no insertion is performed.
	 * @param item The item to store
	 * @param existingMatch An existing matching entry from the database.
	 * @param connection Implicit DB connection
	 * @return Result of this store operation, either containing the inserted entry, or 'existingMatch'.
	 */
	def single(item: In, existingMatch: E)(implicit connection: Connection): R =
		single(item, Some(existingMatch))
	
	/**
	 * Stores 0-n items to the database. Checks against existing data and won't insert any duplicate entries.
	 * @param itemsToStore Items to store
	 * @param existingItems Matching items from the database. Call-by-name: Not called if there are no items to store.
	 * @param itemToKey A function that accepts an item to store and maps it to a unique key
	 * @param existingToKey A function that accepts an existing database entry and maps it to a unique key similar
	 *                      to what 'itemToKey' would produce.
	 * @param connection Implicit DB connection
	 * @tparam K Type of keys used
	 * @return A map where keys are the specified keys and values are store results,
	 *         either containing a newly inserted item, or one of the existing DB entries.
	 */
	def keyMap[K](itemsToStore: IterableOnce[In], existingItems: => IterableOnce[E])
	             (itemToKey: In => K)(existingToKey: E => K)(implicit connection: Connection) =
		keyMapped[K](itemsToStore.iterator.map { item => itemToKey(item) -> item },
			existingItems.iterator.map { existing => existingToKey(existing) -> existing }.toMap)
	
	/**
	 * Inserts a new item to the database. Assumes that no matching entry exists in the database.
	 * @param item The item to insert
	 * @param connection Implicit DB connection
	 * @return The inserted item
	 */
	def unique(item: In)(implicit connection: Connection) = single(item, None)
	/**
	 * Inserts 0-n items to the database. Assumes that no matching entries exist in the database.
	 * @param items The items to insert
	 * @param connection Implicit DB connection
	 * @return The inserted items (not necessarily in the same order as specified).
	 */
	def unique(items: IterableOnce[In])(implicit connection: Connection) =
		keyMapUnique(items)(Identity).values
	
	/**
	 * Inserts 0-n items to the database. Assumes that no matching entries exist in the database.
	 * Performs key-mapping in order to get the store results as a map.
	 * @param items The items to insert
	 * @param toKey A function that converts an item into a unique map key
	 * @param connection Implicit DB connection
	 * @return A map containing the inserted items, each mapped to a key produced with 'toKey'
	 */
	def keyMapUnique[K](items: IterableOnce[In])(toKey: In => K)(implicit connection: Connection) =
		keyMappedUnique(items.iterator.map { i => toKey(i) -> i })
	/**
	 * Inserts 0-n key-mapped items to the database. Assumes that no matching entries exist in the database.
	 * @param items The items to insert as key-value pairs.
	 * @param connection Implicit DB connection
	 * @return A map containing the inserted items, each mapped to their respective key.
	 */
	def keyMappedUnique[K](items: IterableOnce[(K, In)])(implicit connection: Connection) =
		keyMapped(items, Map.empty[K, E])
	
	/**
	 * @param itemsToStore Items to store, including unique keys
	 * @param existingItems Existing DB entries. Call-by-name: Not called if there are not items to store.
	 * @param existingToKey A function which converts an existing item into a unique key
	 * @param connection Implicit DB connection
	 * @return A map where each encountered key is mapped to the stored item (either existing or inserted)
	 */
	def keyMapExisting[K, V](itemsToStore: IterableOnce[In], existingItems: => IterableOnce[E])(existingToKey: E => K)
	                        (implicit connection: Connection, ev: In <:< (K, V)) =
		keyMap(itemsToStore, existingItems) { _._1 }(existingToKey)
}
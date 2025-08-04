package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{IntSet, OptimizedIndexedSeq}
import utopia.flow.operator.Identity
import utopia.flow.operator.MaybeEmpty.collectionMayBeEmpty
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.view.{NullDeprecatableView, ViewManyByIntIds}
import utopia.vault.store.{HasId, StoreResult, Stored}

import scala.collection.View

/**
  * Provides simple interfaces for facilitating duplicate-aware insertion logic, a.k.a. storing.
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
object Store
{
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * The specified items are expected to be in prepared data format, ready to be inserted after duplicate-exclusion.
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates)
	 * @param existingItems Related items that already existed in the DB
	 * @param dataToKey A function that extracts a unique key from a data entry
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def keyMapData[K, D, S <: Stored[D, Int]](model: DataInserter[_, S, D], itemsToStore: Iterable[D],
	                                          existingItems: Iterable[S])
	                                         (dataToKey: D => K)(implicit connection: Connection) =
		keyMap[K, D, D, S](model, itemsToStore, existingItems)(dataToKey) { e => dataToKey(e.data) }(Identity)
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates)
	 * @param existingItems Related items that already existed in the DB
	 * @param itemToKey     A function that extracts a unique key from a proposed item
	 * @param existingToKey A function that extracts a unique key from an existing item
	 * @param toData A function that converts an item into a data-set ready for DB insertion
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam V Type of stored values
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def keyMap[K, V, D, S <: HasId[Int]](model: DataInserter[_, S, D], itemsToStore: Iterable[V],
	                                     existingItems: Iterable[S])
	                                    (itemToKey: V => K)(existingToKey: S => K)
	                                    (toData: V => D)
	                                    (implicit connection: Connection) =
		keyMapped[K, V, D, S](model, itemsToStore.view.map { a => itemToKey(a) -> a },
			existingItems.view.map { e => existingToKey(e) -> e }.toMap)(toData)
	
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * The specified items are expected to be in prepared data format, ready to be inserted after duplicate-exclusion.
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates). Each entry is mapped to a unique key.
	 * @param existingItems Related items that already existed in the DB. Each entry is mapped to a unique key.
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def keyMappedData[K, D, S <: HasId[Int]](model: DataInserter[_, S, D], itemsToStore: Iterable[(K, D)],
	                                         existingItems: Map[K, S])
	                                        (implicit connection: Connection) =
		keyMapped[K, D, D, S](model, itemsToStore, existingItems)(Identity)
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates). Each entry is mapped to a unique key.
	 * @param existingItems Related items that already existed in the DB. Each entry is mapped to a unique key.
	 * @param toData A function that converts an item into a data-set ready for DB insertion
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam V Type of stored values
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def keyMapped[K, V, D, S <: HasId[Int]](model: DataInserter[_, S, D], itemsToStore: Iterable[(K, V)],
	                                        existingItems: Map[K, S])
	                                       (toData: V => D)
	                                       (implicit connection: Connection) =
	{
		// Checks for duplicates
		val dataToInsert = itemsToStore.view
			.flatMap { case (key, item) =>
				if (existingItems.contains(key))
					None
				// Case: Not a duplicate => Prepares data to insert
				else
					Some(key -> toData(item))
			}
			.toOptimizedSeq
		
		// Case: No data to insert => Returns the existing entries
		if (dataToInsert.isEmpty)
			existingItems.view.mapValues(StoreResult.existed).toMap
		else {
			// Inserts the prepared data
			val inserted = model.insertFrom(dataToInsert) { _._2 } { case (inserted, (key, _)) =>
				key -> StoreResult.inserted(inserted) }
			
			// Combines the existing and the inserted data
			View.concat(existingItems.view.mapValues(StoreResult.existed), inserted).toMap
		}
	}
	
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * New data may replace existing data, in which case the existing database entry is deprecated.
	 * The specified items are expected to be in prepared data format, ready to be inserted after duplicate-exclusion.
	 * @param access Root access point for querying existing data
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates)
	 * @param existingItems Related items that already existed in the DB
	 * @param dataToKey A function that extracts a unique key from a data entry
	 * @param shouldReplace A function that compares a new item (1) and an existing item's data (2),
	 *                      where their unique keys matched, and yields true in
	 *                      situations where the new item should replace the existing DB entry.
	 *                      If this function yields false, the new item is considered a duplicate and ignored.
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def keyMapDataReplacing[K, D, S <: Stored[D, Int]](access: ViewManyByIntIds[NullDeprecatableView[_]],
	                                                   model: DataInserter[_, S, D],
	                                                   itemsToStore: Iterable[D], existingItems: Iterable[S])
	                                                  (dataToKey: D => K)(shouldReplace: (D, D) => Boolean)
	                                                  (implicit connection: Connection) =
		keyMapReplacing[K, D, D, S](access, model, itemsToStore, existingItems)(dataToKey) { e => dataToKey(e.data) } {
			(data, existing) => shouldReplace(data, existing.data) }(Identity)
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * New data may replace existing data, in which case the existing database entry is deprecated.
	 * @param access Root access point for querying existing data
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates)
	 * @param existingItems Related items that already existed in the DB
	 * @param itemToKey A function that extracts a unique key from a proposed item
	 * @param existingToKey A function that extracts a unique key from an existing item
	 * @param shouldReplace A function that compares a new item (1) and an existing item (2),
	 *                      where their unique keys matched, and yields true in
	 *                      situations where the new item should replace the existing DB entry.
	 *                      If this function yields false, the new item is considered a duplicate and ignored.
	 * @param toData A function that converts an item into a data-set ready for DB insertion
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam V Type of stored values
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def keyMapReplacing[K, V, D, S <: HasId[Int]](access: ViewManyByIntIds[NullDeprecatableView[_]],
	                                              model: DataInserter[_, S, D],
	                                              itemsToStore: Iterable[V], existingItems: Iterable[S])
	                                             (itemToKey: V => K)(existingToKey: S => K)
	                                             (shouldReplace: (V, S) => Boolean)(toData: V => D)
	                                             (implicit connection: Connection) =
		replaceKeyMapped[K, V, D, S](access, model, itemsToStore.view.map { a => itemToKey(a) -> a },
			existingItems.view.map { e => existingToKey(e) -> e }.toMap)(shouldReplace)(toData)
	
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * New data may replace existing data, in which case the existing database entry is deprecated.
	 * The specified items are expected to be in prepared data format, ready to be inserted after duplicate-exclusion.
	 * @param access Root access point for querying existing data
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates). Each entry is mapped to a unique key.
	 * @param existingItems Related items that already existed in the DB. Each entry is mapped to a unique key.
	 * @param shouldReplace A function that compares a new item (1) and an existing item (2) and yields true in
	 *                      situations where the new item should replace the existing DB entry.
	 *                      If this function yields false, the new item is considered a duplicate and ignored.
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam D Type of prepared data entries / items to store
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def replaceKeyMappedData[K, D, S <: HasId[Int]](access: ViewManyByIntIds[NullDeprecatableView[_]],
	                                                model: DataInserter[_, S, D],
	                                                itemsToStore: Iterable[(K, D)], existingItems: Map[K, S])
	                                               (shouldReplace: (D, S) => Boolean)
	                                               (implicit connection: Connection) =
		replaceKeyMapped[K, D, D, S](access, model, itemsToStore, existingItems)(shouldReplace)(Identity)
	/**
	 * Stores 0-n items, avoiding inserting duplicate.
	 * New data may replace existing data, in which case the existing database entry is deprecated.
	 * @param access Root access point for querying existing data
	 * @param model A DB model for inserting new data to the DB
	 * @param itemsToStore Items to store to the DB (excluding duplicates). Each entry is mapped to a unique key.
	 * @param existingItems Related items that already existed in the DB. Each entry is mapped to a unique key.
	 * @param shouldReplace A function that compares a new item (1) and an existing item (2) and yields true in
	 *                      situations where the new item should replace the existing DB entry.
	 *                      If this function yields false, the new item is considered a duplicate and ignored.
	 * @param toData A function that converts an item into a data-set ready for DB insertion
	 * @param connection Implicit DB connection
	 * @tparam K Type of used unique keys
	 * @tparam V Type of stored values
	 * @tparam D Type of prepared data entries
	 * @tparam S Type of existing database entries
	 * @return A map containing both the existing and the inserted items, mapped to their unique keys
	 */
	def replaceKeyMapped[K, V, D, S <: HasId[Int]](access: ViewManyByIntIds[NullDeprecatableView[_]],
	                                               model: DataInserter[_, S, D],
	                                               itemsToStore: Iterable[(K, V)], existingItems: Map[K, S])
	                                              (shouldReplace: (V, S) => Boolean)(toData: V => D)
	                                              (implicit connection: Connection) =
	{
		// Reviews the items, collecting inserts, deprecations and existing matches
		val idsToReplaceBuilder = IntSet.newBuilder
		val insertsBuilder = OptimizedIndexedSeq.newBuilder[(K, V)]
		itemsToStore.foreach { case (key, item) =>
			existingItems.get(key) match {
				case Some(existing) =>
					if (shouldReplace(item, existing)) {
						idsToReplaceBuilder += existing.id
						insertsBuilder += (key -> item)
					}
				case None => insertsBuilder += (key -> item)
			}
		}
		
		insertsBuilder.result().notEmpty match {
			case Some(inserts) =>
				// Performs the deprecations and the inserts
				idsToReplaceBuilder.result().notEmpty.foreach { access(_).deprecate() }
				val inserted = model.insertFrom(inserts) { case (_, v) => toData(v) } { case (inserted, (key, _)) =>
					key -> StoreResult.inserted(inserted) }
				
				// Combines the results
				View.concat(existingItems.view.mapValues(StoreResult.existed), inserted).toMap
			
			// Case: No data to insert => Just returns the existing items
			case None => existingItems.view.mapValues(StoreResult.existed).toMap
		}
	}
}

package utopia.logos.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.WeakList
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.vault.database.Connection

import scala.collection.{MapView, View}

/**
 * An abstract thread-safe implementation of a storing interface that weakly caches generated mappings.
 * @tparam I Type of values accepted for storing
 * @tparam K Type of stored mapping keys
 * @tparam V Type of database values attached to the stored values
 * @author Mikko Hilpinen
 * @since 27.02.2025, v0.5
 */
abstract class CachingVolatileMapStore[I, K, V]
{
	// ATTRIBUTES   -------------------------
	
	private val cachedMapsP: Volatile[WeakList[Map[K, V]]] = Volatile(WeakList.empty)
	private val storeLock = new AnyRef
	
	
	// ABSTRACT -----------------------------
	
	/**
	 * Standardizes a value, so that it may be used as a map key.
	 * Note: Only used when mapping externally provided values,
	 * not called for [[diff]], [[pullMatchMap]] or [[insertAndMap]].
	 * @param value Value to standardize
	 * @return Standardized version of the specified value
	 */
	protected def standardize(value: I): K
	/**
	 * Finds the values that are different between the two sets of values.
	 * May perform some sort of standardization, or just use `proposed -- existing`
	 * @param proposed Proposed new values
	 * @param existing Existing set of values
	 * @return Proposed new values that don't match any of the existing values
	 */
	protected def diff(proposed: Set[I], existing: Set[K]): Set[I]
	/**
	 * Pulls matching values from the database
	 * @param values Values for which DB matches are searched
	 * @param connection Implicit DB connection
	 * @return A map where keys are the specified values and values are their DB matches
	 */
	protected def pullMatchMap(values: Set[I])(implicit connection: Connection): Map[K, V]
	/**
	 * Inserts new values to the DB
	 * @param values Values to insert
	 * @param connection Implicit DB connection
	 * @return A map where keys are the inserted values and values are their DB matches
	 */
	protected def insertAndMap(values: Seq[I])(implicit connection: Connection): Map[K, V]
	
	
	// COMPUTED -----------------------------
	
	private def cached = cachedMapsP.mutate { cached =>
		val iter = cached.iterator
		if (iter.hasNext) {
			val first = iter.next()
			// Case: Combining 2 or more cached maps
			//       => Weakly caches the result, replacing the multiple separate entries
			if (iter.hasNext) {
				val combined = iter.foldLeft(first) { _ ++ _ }
				combined -> WeakList(combined)
			}
			// Case: Cache contains only a single map
			else
				first -> cached
		}
		// Case: Cache is empty
		else
			Map.empty[K, V] -> WeakList.empty
	}
	
	
	// OTHER    --------------------------
	
	/**
	 * Stores the specified values in the DB. Avoids inserting duplicates.
	 * @param values Values that will be transformed and stored
	 * @param extract A function that extracts an insertable part from a value
	 * @param merge A function that merges 3 values:
	 *                  1. The inserted value
	 *                  1. The original value
	 *                  1. Whether the value was inserted
	 * @param connection Implicit DB connection
	 * @tparam A Type of original values
	 * @tparam R Type of merge results
	 * @return Merge results
	 */
	def storeFrom[A, R](values: Iterable[A])(extract: A => I)(merge: (V, A, Boolean) => R)
	                   (implicit connection: Connection, log: Logger) =
	{
		// Extracts the information to store
		val inputWithValues = values.view.map { v => v -> extract(v) }.toOptimizedSeq
		// Stores the distinct values
		val valueMap = store(inputWithValues.view.map { _._2 }.toSet)
		// Merges the inserted data with the original data
		inputWithValues.flatMap { case (original, extracted) =>
			val matchingValue = valueMap.get(standardize(extracted))
			if (matchingValue.isEmpty)
				log(s"Warning: Stored value $original => $extracted didn't match any of the resulting map values")
			matchingValue.map { case (value, wasInserted) => merge(value, original, wasInserted) }
		}
	}
	/**
	 * Stores the specified values to the database. Avoids inserting duplicates.
	 * @param values Values to store
	 * @param connection Implicit DB connection
	 * @return A Map (view) where keys are the specified values and values are their DB matches.
	 *         Each value is attached to a boolean indicating whether the entry was just inserted.
	 */
	def store(values: Set[I])(implicit connection: Connection) = {
		// Case: No values to pull
		if (values.isEmpty)
			MapView.empty[K, (V, Boolean)]
		else {
			// Looks up cached information, and which values require database access
			val cached = this.cached
			val valuesToPull = diff(values, cached.keySet)
			
			// Case: Cache contains all requested values => Uses cached information
			if (valuesToPull.isEmpty)
				cached.view.mapValues { _ -> true }
			else {
				// DB interactions are synchronized
				storeLock.synchronized {
					val existing = pullMatchMap(valuesToPull)
					cache(existing)
					val newValues = diff(valuesToPull, existing.keySet)
					// Case: No inserts are needed => returns
					if (newValues.isEmpty)
						(cached ++ existing).view.mapValues { _ -> true }
					else {
						val inserted = insertAndMap(newValues.toOptimizedSeq)
						cache(inserted)
						(View.concat(cached, existing).map { case (k, v) => k -> (v -> true) }.toMap ++
							inserted.view.mapValues { _ -> false })
							.view
					}
				}
			}
		}
	}
	
	private def cache(mapping: Map[K, V]) = cachedMapsP.update { _ :+ mapping }
}

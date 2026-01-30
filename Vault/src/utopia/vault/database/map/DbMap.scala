package utopia.vault.database.map

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.{Duration, TimeUnit}
import utopia.flow.view.mutable.Resettable
import utopia.vault.database.map.DbMap.{DbMapValue, GenericDbMap}
import utopia.vault.database.value.{LazyDbValue, LazyLookUpDbValue}
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.columns.AccessColumnValue
import utopia.vault.nosql.targeting.columns.AccessColumnValue.AccessColumnValueFactory
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.Condition

import java.nio.file.Path
import scala.util.Try

object DbMap
{
	// OTHER    ------------------------
	
	/**
	 * Creates a new database map factory
	 * @param access Wrapped column access interface
	 * @param valueColumn Column which stores the map values
	 * @param cPool Implicit connection pool
	 * @tparam A Type of the access point used
	 * @return A factory for constructing database maps
	 */
	def apply[A <: AccessColumn with Filterable[A]](access: A, valueColumn: Column)
	                                               (implicit cPool: ConnectionPool): DbMapFactory[A, Nothing] =
		DbMapFactory[A, Nothing](access, valueColumn)
	
	
	// NESTED   ------------------------
	
	/**
	 * Common trait for value access interfaces used with DbMaps
	 * @tparam A Type of the accessed value
	 * @tparam In Type of the input value (used in value-assignment functions)
	 */
	trait DbMapValue[+A, -In] extends LazyDbValue[A] with Resettable
	{
		// ABSTRACT --------------------
		
		/**
		 * @param newValue A new value to assign for this key
		 * @param connection Implicit DB connection
		 */
		def set(newValue: In)(implicit connection: Connection): Unit
		/**
		 * Clears the value of this key, setting it to NULL.
		 * @param connection Implicit DB connection.
		 */
		def clear()(implicit connection: Connection): Unit
	}
	
	case class DbMapFactory[+A <: AccessColumn with Filterable[A], C](access: A, valueColumn: Column,
	                                                                  context: Option[C] = None,
	                                                                  refreshInterval: Duration = Duration.infinite,
	                                                                  extraUseDuration: Duration = Duration.zero)
	                                                                 (implicit cPool: ConnectionPool)
	{
		// COMPUTED ----------------------
		
		/**
		 * Changes this factory's accepted context type.
		 * @tparam C2 Type of the new accepted context
		 * @return Copy of this factory accepting the specified type of context
		 */
		def acceptingContextOf[C2 >: C] = copy[A, C2](context = context)
		
		
		// OTHER    ----------------------
		
		/**
		 * @param context New (starting) context to assign
		 * @tparam C2 Type of the accepted context values
		 * @return Copy of this factory wrapping the specified initial context
		 */
		def withContext[C2](context: C2) = copy(context = Some(context))
		
		/**
		 * @param refreshInterval A value refresh interval to apply. Cached values are cleared or reset after they're
		 *                        queried past this store duration.
		 * @param extraUseDuration Duration past 'refreshInterval',
		 *                         during which a cached value may still be returned (once).
		 *                         Setting this to a positive value may reduce the number of value look-ups.
		 *                         Default = 0 = After 'refreshInterval', a new value is always acquired.
		 * @return Copy of this factory caching the values for the specified duration
		 */
		def refreshingAfter(refreshInterval: Duration, extraUseDuration: Duration = Duration.zero) =
			copy(refreshInterval = refreshInterval, extraUseDuration = extraUseDuration)
		
		/**
		 * Creates a new DB map
		 * @param contextToCondition A function which converts a context instance into a filter condition
		 * @param keyToCondition A function which converts a map key into a filter condition
		 * @param store A function which may store a new key-value pair to the database.
		 *              Used in situations where a targeted update didn't affect any rows.
		 *
		 *              Receives 4 values:
		 *                  1. Applicable context value (if specified)
		 *                  1. Targeted key
		 *                  1. Assigned value as a [[Value]]
		 *                  1. Database connection
		 *
		 * @tparam K Type of the keys accepted in this map
		 * @return A new DB map
		 */
		def apply[K](contextToCondition: C => Condition)(keyToCondition: K => Condition)
		            (store: (Option[C], K, Value, Connection) => Unit): DbMap[K, C] =
		{
			implicit val params: DbMapParams[K, C] = new DbMapParams(valueColumn, refreshInterval, extraUseDuration,
				contextToCondition, keyToCondition, store)
			new _DbMap[K, C, A](access, context)
		}
		/**
		 * Creates a new DB map, which doesn't utilize context values
		 * @param keyToCondition A function which converts a map key into a filter condition
		 * @param store A function which may store a new key-value pair to the database.
		 *              Used in situations where a targeted update didn't affect any rows.
		 *
		 *              Receives 3 values:
		 *                  1. Targeted key
		 *                  1. Assigned value as a [[Value]]
		 *                  1. Database connection
		 *
		 * @tparam K Type of the keys accepted in this map
		 * @return A new DB map
		 */
		def withoutContext[K](keyToCondition: K => Condition)(store: (K, Value, Connection) => Unit): DbMap[K, Nothing] = {
			implicit val params: DbMapParams[K, Any] = new DbMapParams(valueColumn, refreshInterval, extraUseDuration,
				_ => Condition.alwaysTrue, keyToCondition, (_, k, v, c) => store(k, v, c))
			new _DbMap[K, Nothing, A](access)
		}
	}
	
	private class GenericDbMap[-K, -C](wrapped: DbMap[K, C])(implicit jsonParser: JsonParser) extends DbMap[K, C]
	{
		override def parsingJson(implicit jsonParser: JsonParser): DbMap[K, C] = this
		
		override def accessWith[A, In](key: K)(makeValue: AccessColumnValueFactory => AccessColumnValue[A, _, In]): DbMapValue[A, In] =
			wrapped.accessWith(key) { factory => makeValue(factory.parsingGenericValues) }
		
		override def withContext(newContext: C): DbMap[K, C] = new GenericDbMap(wrapped.withContext(newContext))
	}
	
	private class _DbMap[-K, -C, +V <: AccessColumn with Filterable[V]](access: V, context: Option[C] = None)
	                                                                   (implicit cPool: ConnectionPool,
	                                                                    params: DbMapParams[K, C])
		extends DbMap[K, C]
	{
		// ATTRIBUTES   ---------------------
		
		// Applies context-based filtering, if appropriate
		private lazy val filteredAccess = context match {
			case Some(context) => access.filter(params.contextToCondition(context))
			case None => access
		}
		
		
		// IMPLEMENTED  ---------------------
		
		override def accessWith[A, In](key: K)(makeValue: AccessColumnValueFactory => AccessColumnValue[A, _, In]): DbMapValue[A, In] =
			new _DbMapValue(
				makeValue(AccessColumnValue(filteredAccess.filter(params.keyToCondition(key)), params.valueColumn)),
				params.refreshInterval, params.extraUseDuration)({ (value, connection) =>
				params.insertRow(key, value, context)(connection)
			})
		
		override def withContext(newContext: C): DbMap[K, C] = new _DbMap(access, Some(newContext))
	}
	
	private class _DbMapValue[+A, -In](access: AccessColumnValue[A, _, In], refreshInterval: Duration,
	                                   extraUseDuration: Duration)(insert: (Value, Connection) => Unit)
	                                  (implicit cPool: ConnectionPool)
		extends DbMapValue[A, In]
	{
		// ATTRIBUTES   ----------------------
		
		// Wraps a LazyDbValue
		private val _value: LazyDbValue[A] with Resettable = {
			if (refreshInterval.isFinite)
				LazyLookUpDbValue.refreshing(refreshInterval, extraUseDuration) { implicit c => access.pull }
			else
				LazyLookUpDbValue { implicit c => access.pull }
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def value: A = _value.value
		override def current: Option[A] = _value.current
		
		override def isSet: Boolean = _value.isSet
		
		override def connectedValue(implicit connection: Connection): A = _value.connectedValue
		
		override def reset(): Boolean = _value.reset()
		
		override def set(newValue: In)(implicit connection: Connection): Unit = {
			// First attempts an update
			if (!access.set(newValue)) {
				// If no row was updated, may insert a new row
				insert(access.valueOf(newValue), connection)
			}
			reset()
		}
		override def clear()(implicit connection: Connection): Unit = {
			access.clear()
			reset()
		}
	}
	
	private class DbMapParams[-K, -C](val valueColumn: Column, val refreshInterval: Duration,
	                                  val extraUseDuration: Duration, val contextToCondition: C => Condition,
	                                  val keyToCondition: K => Condition,
	                                  inserter: (Option[C], K, Value, Connection) => Unit)
	{
		def insertRow(key: K, value: Value, context: Option[C])
		             (implicit connection: Connection): Unit =
			inserter(context, key, value, connection)
	}
}

/**
 * An interface for accessing and storing (generic) values from/to a database table
 * @tparam K Type of map keys used
 * @tparam C Type of accepted context
 * @author Mikko Hilpinen
 * @since 20.11.2025, v2.1
 */
trait DbMap[-K, -C]
{
	// ABSTRACT ----------------------
	
	/**
	 * Provides access to a map value
	 * @param key Targeted key
	 * @param makeValue A function which finalizes column access, specifying accessed value type, etc.
	 * @tparam A Type of the accessed column values
	 * @tparam In Type of the settable column values
	 * @return Map value access
	 */
	def accessWith[A, In](key: K)(makeValue: AccessColumnValueFactory => AccessColumnValue[A, _, In]): DbMapValue[A, In]
	
	/**
	 * @param newContext New context parameter to assign
	 * @return A copy of this map using the specified context instead
	 */
	def withContext(newContext: C): DbMap[K, C]
	
	
	// COMPUTED ---------------------
	
	/**
	 * @param jsonParser JSON parser used when interpreting column values
	 * @return A copy of this map which expects column values to contain JSON strings.
	 */
	def parsingJson(implicit jsonParser: JsonParser): DbMap[K, C] = new GenericDbMap[K, C](this)
	
	
	// OTHER    ---------------------
	
	/**
	 * @param key Targeted key
	 * @param parse A function which parses column values
	 * @param valueOf An implicit function for converting a parsed value into a [[Value]] for storing it into the DB
	 * @tparam A Type of parsed values
	 * @return Access to the specified key, applying the specified parsing logic
	 */
	def apply[A](key: K)(parse: Value => A)(implicit valueOf: A => Value) =
		accessWith(key) { _(parse) }
	/**
	 * @param key Targeted key
	 * @param parse A function which parses column values. May yield None.
	 * @param valueOf An implicit function for converting a parsed value into a [[Value]] for storing it into the DB
	 * @tparam A Type of parsed values
	 * @return Access to the specified key, applying the specified parsing logic
	 */
	def get[A](key: K)(parse: Value => Option[A])(implicit valueOf: A => Value) =
		accessWith(key) { _.optional(parse) }
	/**
	 * @param key Targeted key
	 * @param parse A function which parses column values. Yields a [[Try]].
	 * @param valueOf An implicit function for converting a parsed value into a [[Value]] for storing it into the DB
	 * @tparam A Type of parsed values
	 * @return Access to the specified key, applying the specified parsing logic
	 */
	def required[A](key: K)(parse: Value => Try[A])(implicit valueOf: A => Value) =
		accessWith(key) { _.tryParse(parse) }
	
	/**
	 * @param key Targeted key
	 * @param parse A function which parses individual values
	 * @param valueOf An implicit function for converting a parsed value into a [[Value]] for storing it into the DB
	 * @tparam A Type of individually processed values
	 * @return Access to the specified key, applying parsing logic which supports Vector values
	 */
	def seq[A](key: K)(parse: Value => A)(implicit valueOf: A => Value) =
		accessWith(key) { _ { _.getVector.view.map(parse).toOptimizedSeq } }
	
	/**
	 * @param key Targeted key
	 * @return Access to the specified key, parsing the values as [[Path]] options
	 */
	def getPath(key: K) =
		getCustom(key) { _.string.map { p => p: Path } } { p: Path => p.toJson }
	/**
	 * @param key Targeted key
	 * @param default Default setting value (call-by-name)
	 * @return Access to the specified key, parsing the values as [[Path]].
	 *         Replacing empty values with the specified default.
	 */
	def pathOr(key: K, default: => Path) =
		customOr(key, default) { _.string.map { p => p: Path } } { p: Path => p.toJson }
	
	/**
	 * @param key Targeted key
	 * @return Access to the specified key, parsing the values as [[Duration]].
	 */
	def getDuration(key: K, unit: TimeUnit) =
		getCustom(key) { _.long.map { Duration(_, unit) } } { d: Duration => d.to(unit) }
	/**
	 * @param key Targeted key
	 * @param default Default setting value (call-by-name)
	 * @return Access to the specified key, parsing the values as [[Duration]].
	 *         Replacing empty values with the specified default.
	 */
	def durationOr(key: K, unit: TimeUnit, default: => Duration) =
		customOr(key, default) { _.long.map { Duration(_, unit) } } { d: Duration => d.to(unit) }
	
	/**
	 * @param key Targeted key
	 * @param parse A function which parses column values.
	 * @param toValue A function for converting a parsed value (of some type) into a [[Value]] for storing it into the DB
	 * @tparam A Type of the parsed values
	 * @tparam In Type of input values in set functions
	 * @return Access to the specified key, applying the specified parsing logic
	 */
	def custom[A, In](key: K)(parse: Value => A)(toValue: In => Value) =
		accessWith(key) { _.custom(parse)(toValue).concrete }
	/**
	 * @param key Targeted key
	 * @param parse A function which parses column values. May yield None.
	 * @param toValue A function for converting a parsed value (of some type) into a [[Value]] for storing it into the DB
	 * @tparam A Type of the parsed values
	 * @tparam In Type of input values in set functions
	 * @return Access to the specified key, applying the specified parsing logic
	 */
	def getCustom[A, In](key: K)(parse: Value => Option[A])(toValue: In => Value) =
		accessWith(key) { _.custom(parse)(toValue).iterable }
	/**
	 * @param key Targeted key
	 * @param default The default value to use, if parsing yields None.
	 * @param parse A function which parses column values. May yield None.
	 * @param toValue A function for converting a parsed value (of some type) into a [[Value]] for storing it into the DB
	 * @tparam A Type of the parsed values
	 * @tparam In Type of input values in set functions
	 * @return Access to the specified key, applying the specified parsing logic
	 */
	def customOr[A, In](key: K, default: => A)(parse: Value => Option[A])(toValue: In => Value) =
		custom(key) { parse(_).getOrElse(default) }(toValue)
}
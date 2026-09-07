package utopia.vault.database.value

import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Resettable
import utopia.flow.view.mutable.async.Volatile
import utopia.vault.database.value.DbValue.MappedDbValue
import utopia.vault.database.{Connection, ConnectionPool}

import scala.util.Try

object DbValue
{
	// COMPUTED ------------------------
	
	/**
	 * @return Access to caching DB value constructors
	 */
	def caching = LazyDbValue
	
	
	// OTHER    ------------------------
	
	/**
	 * @param f A function used for pulling the targeted value. May yield a failure.
	 * @param cPool Implicit DB connection pool to use, if necessary
	 * @tparam A Type of the accessed value, when successful
	 * @return An interface for accessing a DB value using the specified function
	 */
	def tryPull[A](f: Connection => Try[A])(implicit cPool: ConnectionPool): DbValue[Try[A]] = new TryDbValue[A](f)
	
	/**
	 * @param f A function used for pulling the targeted value
	 * @param cPool Implicit connection pool to use, if necessary
	 * @tparam A Type of the pulled values
	 * @return An interface for accessing a DB value using the specified function
	 */
	def apply[A](f: Connection => A)(implicit cPool: ConnectionPool): DbValue[A] = new _DbValue[A](f)
	
	
	// NESTED   ------------------------
	
	private class MappedDbValue[A, B](source: DbValue[A], f: (A, Connection) => B)(implicit cPool: ConnectionPool)
		extends DbValue[B]
	{
		// ATTRIBUTES   ----------------
		
		private val cache = Volatile.empty[(A, B)]
		
		
		// IMPLEMENTED  ----------------
		
		override def value: B = cPool { implicit c => connectedValue }
		override def connectedValue(implicit connection: Connection): B = {
			val origin = source.connectedValue
			cache.value.filter { _._1 == origin } match {
				case Some((_, value)) => value
				case None =>
					val value = f(origin, connection)
					cache.setOne((origin, value))
					value
			}
		}
		
		override def isSet: Boolean = cache.nonEmpty && source.isSet
		
		override def reset(): Boolean = {
			val cacheWasReset = cache.pop().isDefined
			source.reset() || cacheWasReset
		}
	}
	
	private class TryDbValue[+A](f: Connection => Try[A])(implicit cPool: ConnectionPool) extends DbValue[Try[A]]
	{
		override def value: Try[A] = cPool.tryWith(f).flatten
		override def connectedValue(implicit connection: Connection): Try[A] = f(connection)
		
		override def reset(): Boolean = false
		override def isSet: Boolean = false
	}
	
	private class _DbValue[+A](f: Connection => A)(implicit cPool: ConnectionPool) extends DbValue[A]
	{
		override def value: A = cPool(f)
		override def connectedValue(implicit connection: Connection): A = f(connection)
		
		override def reset(): Boolean = false
		override def isSet: Boolean = false
	}
}

/**
 * Common trait for views into database-originated values
 * @author Mikko Hilpinen
 * @since 24.05.2026, v2.2
 */
trait DbValue[+A] extends View[A] with Resettable
{
	// ABSTRACT ------------------------
	
	/**
	 * @param connection Implicit DB connection to utilize, if necessary
	 * @return Wrapped value
	 */
	def connectedValue(implicit connection: Connection): A
	
	
	// OTHER    ------------------------
	
	/**
	 * Maps the value of this container
	 * @param f A mapping function that modifies this value. Also receives an open DB connection.
	 *          Assumed to be deterministic, with no side effects.
	 * @param cPool Implicit connection pool to use.
	 * @tparam B Type of mapping results.
	 * @return A view into the mapped value.
	 */
	def mapConnected[B](f: (A, Connection) => B)(implicit cPool: ConnectionPool): DbValue[B] =
		new MappedDbValue[A, B](this, f)
}

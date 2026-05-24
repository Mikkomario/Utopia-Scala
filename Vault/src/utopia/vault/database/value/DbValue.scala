package utopia.vault.database.value

import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Resettable
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
}

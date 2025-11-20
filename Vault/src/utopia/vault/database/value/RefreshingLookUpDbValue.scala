package utopia.vault.database.value

import utopia.flow.time.{Duration, Now}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.{Pointer, Resettable}
import utopia.vault.database.{Connection, ConnectionPool}

import java.time.Instant
import scala.annotation.unchecked.uncheckedVariance

object RefreshingLookUpDbValue
{
	// OTHER    -------------------------
	
	/**
	 * @param refreshInterval Interval between automated refreshes
	 * @param extraUseDuration Duration after 'refreshInterval', after which a cached value may still be used (once).
	 *
	 *                         If set to a positive value, the cache is cleared after 'refreshInterval', but a
	 *                         new value is not immediately acquired, unless this duration has passed since
	 *                         the scheduled refresh.
	 *
	 *                         Default = 0 = A new value is always acquired immediately.
	 * @param lookUp A function which acquires the value to cache
	 * @param cPool Implicit connection pool used when necessary
	 * @tparam A Type of the cached values
	 * @return A new interface which caches and looks up values, as necessary
	 */
	def apply[A](refreshInterval: Duration, extraUseDuration: Duration = Duration.zero)(lookUp: Connection => A)
	            (implicit cPool: ConnectionPool): RefreshingLookUpDbValue[A] =
		new _RefreshingLookUpDbValue[A](refreshInterval, extraUseDuration)(lookUp)
	
	
	// NESTED   -------------------------
	
	private class _RefreshingLookUpDbValue[+A](refreshInterval: Duration, extraUseDuration: Duration)
	                                          (f: Connection => A)
	                                          (implicit cPool: ConnectionPool)
		extends RefreshingLookUpDbValue[A](refreshInterval, extraUseDuration)
	{
		override protected def lookUp(implicit connection: Connection): A = f(connection)
	}
}

/**
 * An abstract implementation of [[LazyDbValue]] trait, which caches the acquired value for some time, refreshing it
 * periodically.
 * @author Mikko Hilpinen
 * @since 20.11.2025, v2.1
 */
abstract class RefreshingLookUpDbValue[+A](refreshInterval: Duration, extraUseDuration: Duration = Duration.zero)
                                          (implicit cPool: ConnectionPool)
	extends LazyDbValue[A] with Resettable
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * Contains 3 values when set:
	 *      1. A cached value
	 *      1. Time after which the cache should be cleared
	 *      1. Time after which the cached value may not be used anymore
	 */
	private val currentP: Pointer[Option[(A @uncheckedVariance, Instant, Instant)]] = Pointer.empty
	
	
	// ABSTRACT --------------------------
	
	protected def lookUp(implicit connection: Connection): A
	
	
	// IMPLEMENTED  ----------------------
	
	override def value: A = current.getOrElse { cPool { implicit c => newValue } }
	override def connectedValue(implicit connection: Connection): A = current.getOrElse(newValue)
	
	override def current: Option[A] = {
		lazy val now = Now.toInstant
		currentP.getAndUpdate { _.filter { _._2 > now } }.filter { _._3 > now }.map { _._1 }
	}
	
	override def isSet: Boolean = currentP.value.exists { _._3.isFuture }
	
	override def reset(): Boolean = currentP.getAndSet(None).exists { _._3.isFuture }
	
	
	// OTHER    -------------------------
	
	private def newValue(implicit connection: Connection) = {
		val value = lookUp
		val refreshTime = Now + refreshInterval
		currentP.setOne((value, refreshTime, refreshTime + extraUseDuration))
		value
	}
}

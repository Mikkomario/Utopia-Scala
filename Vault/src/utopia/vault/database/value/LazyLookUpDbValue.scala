package utopia.vault.database.value

import utopia.vault.database.{Connection, ConnectionPool}

import scala.annotation.unchecked.uncheckedVariance

object LazyLookUpDbValue
{
	// OTHER    ---------------------
	
	/**
	 * @param f A function that looks up the targeted value using a DB connection
	 * @param cPool Connection pool to use (if necessary)
	 * @tparam A Type of the lazily acquired value
	 * @return A new lazily initialized DB value
	 */
	def apply[A](f: Connection => A)(implicit cPool: ConnectionPool): LazyLookUpDbValue[A] =
		new _LazyLookUpDbValue[A](f)
	
	
	// NESTED   ---------------------
	
	private class _LazyLookUpDbValue[+A](f: Connection => A)(implicit cPool: ConnectionPool)
		extends LazyLookUpDbValue[A]
	{
		override protected def lookUp(implicit connection: Connection): A = f(connection)
	}
}

/**
 * An abstract implementation of the [[LazyDbValue]] trait, based on a database look-up
 * @author Mikko Hilpinen
 * @since 17.11.2025, v2.0.1
 */
abstract class LazyLookUpDbValue[+A](implicit cPool: ConnectionPool) extends LazyDbValue[A]
{
	// ATTRIBUTES   ----------------
	
	// No variance check, since the interface is fully covariant
	private var _current: Option[A @uncheckedVariance] = None
	
	
	// ABSTRACT --------------------
	
	/**
	 * Looks up this data source ID
	 * @param connection Implicit DB connection
	 * @return The targeted data source ID
	 */
	protected def lookUp(implicit connection: Connection): A
	
	
	// IMPLEMENTED  ----------------
	
	override def current: Option[A] = _current
	
	override def value: A = _current.getOrElse {
		val value = cPool { implicit c => lookUp }
		_current = Some(value)
		value
	}
	override def connectedValue(implicit connection: Connection): A = _current.getOrElse {
		val value = lookUp
		_current = Some(value)
		value
	}
}

package utopia.vault.database.value

import utopia.vault.database.Connection

object InitializedDbValue
{
	// OTHER    -----------------------
	
	/**
	 * @param value The value to wrap
	 * @tparam A Type of the specified value
	 * @return A DB value that wraps the specified value
	 */
	def apply[A](value: A): InitializedDbValue[A] = _InitializedDbValue(value)
	
	
	// NESTED   -----------------------
	
	private case class _InitializedDbValue[+A](value: A) extends InitializedDbValue[A]
}

/**
 * Common trait for implementations of [[LazyDbValue]], which simply wrap a value
 * @author Mikko Hilpinen
 * @since 17.11.2025, v2.1
 */
trait InitializedDbValue[+A] extends LazyDbValue[A]
{
	override def current: Option[A] = Some(value)
	override def connectedValue(implicit connection: Connection): A = value
}

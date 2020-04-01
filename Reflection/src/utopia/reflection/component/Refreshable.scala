package utopia.reflection.component

import scala.collection.SpecificIterableFactory

object Refreshable
{
	implicit class MultiRefreshable[A, C <: Iterable[A]](val r: Refreshable[C]) extends AnyVal
	{
		/**
		  * Removes all items from this pool
		  * @param factory Implicit specific iterable factory
		  */
		def clear()(implicit factory: SpecificIterableFactory[A, C]) = r.content = factory.empty
	}
}

/**
  * Refreshable components are pools that can be refreshed from the program side
  * @author Mikko Hilpinen
  * @since 23.4.2019, v1+
  * @tparam A The type of content in this pool
  */
trait Refreshable[A] extends Pool[A]
{
	/**
	  * Updates the contents of this pool
	  * @param newContent New contents
	  */
	def content_=(newContent: A): Unit
}
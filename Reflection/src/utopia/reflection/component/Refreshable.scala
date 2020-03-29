package utopia.reflection.component

import scala.collection.generic.CanBuildFrom

object Refreshable
{
	implicit class MultiRefreshable[A, C <: Traversable[A]](val r: Refreshable[C]) extends AnyVal
	{
		/**
		  * Removes all items from this pool
		  * @param cbf Implicit canbuildfrom
		  * @tparam C2 Arbitrary collection type
		  */
		def clear[C2]()(implicit cbf: CanBuildFrom[C2, A, C]) =
		{
			val builder = cbf()
			r.content = builder.result()
		}
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
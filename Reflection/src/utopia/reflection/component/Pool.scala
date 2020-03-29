package utopia.reflection.component

object Pool
{
	implicit class MultiPool(val p: Pool[Traversable[_]]) extends AnyVal
	{
		/**
		  * @return The current item count in this pool
		  */
		def count = p.content.size
		
		/**
		  * @return Whether there is currently no content in this pool
		  */
		def isEmpty = p.content.isEmpty
		
		/**
		  * @return Whether there is currently content in this pool
		  */
		def nonEmpty = p.content.nonEmpty
	}
}

/**
  * Pools are used for presenting one or more items
  * @author Mikko Hilpinen
  * @since 23.4.2019, v1+
  * @tparam A The type of content in this pool
  */
trait Pool[+A]
{
	/**
	  * @return The contents of this pool
	  */
	def content: A
}

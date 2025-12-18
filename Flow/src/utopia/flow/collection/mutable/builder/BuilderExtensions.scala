package utopia.flow.collection.mutable.builder

import scala.collection.mutable

/**
 * Provides additional functions for builders
 * @author Mikko Hilpinen
 * @since 25.11.2025, v2.8
 */
object BuilderExtensions
{
	implicit class RichBuilder[A, +To](val b: mutable.Builder[A, To]) extends AnyVal
	{
		/**
		 * @return A copy of this builder which flattens the input
		 */
		def flatten = new FlatteningBuilder(b)
		
		/**
		 * @param f A mapping function applied before adding elements to this builder
		 * @tparam B Type of elements accepted by 'f'
		 * @return A copy of this builder applying the specified mapping function for all input items
		 */
		def mapInput[B](f: B => A) = new MapInputBuilder[B, A, To](b)(f)
	}
}

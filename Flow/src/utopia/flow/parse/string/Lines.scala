package utopia.flow.parse.string

import scala.io.Source

/**
 * Provides interfaces for parsing and for iterating over lines read from data sources
 * @author Mikko Hilpinen
 * @since 28.09.2025, v2.7
 */
object Lines
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * @return An interface for reading (buffered) lines from various sources
	 */
	val from = iterate.buffered
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return An interface for iterating lines from various sources
	 */
	def iterate = IterateLinesFrom
	
	
	// NESTED   ----------------------------
	
	object IterateLinesFrom extends OpenSource[Iterator[String]]
	{
		override protected def presentSource[A](source: Source, processor: Iterator[String] => A): A =
			processor(source.getLines())
	}
}

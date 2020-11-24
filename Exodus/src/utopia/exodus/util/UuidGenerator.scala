package utopia.exodus.util

object UuidGenerator
{
	// OTHER	-----------------------------
	
	/**
	  * @param iterator An <b>infinite</b> iterator that returns unique strings
	  * @return A new uuid generator based on the specified iterator
	  */
	def fromIterator(iterator: Iterator[String]): UuidGenerator = new GeneratorFromIterator(iterator)
	
	/**
	  * @param next A function for generating new unique strings
	  * @return A new uuid generator that uses the specified function to generate new ids
	  */
	def apply(next: => String) = fromIterator(Iterator.continually(next))
	
	
	// NESTED	-----------------------------
	
	private class GeneratorFromIterator(iterator: Iterator[String]) extends UuidGenerator
	{
		override def next() = iterator.next()
	}
}

/**
  * A common trait for unique id generator implementations. The generators should produce keys which cannot simply be
  * guessed by individuals with possible malign intents
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
trait UuidGenerator
{
	/**
	  * @return Creates a new unique id
	  */
	def next(): String
}

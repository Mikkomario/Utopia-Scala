package utopia.flow.parse.json

import scala.collection.mutable

object JsonAppendix
{
	/**
	 * Wraps an item that's already value-convertible
	 * @param convertible A value-convertible item
	 * @return The specified item wrapped as a JsonAppendix
	 */
	def wrap(convertible: JsonConvertible) =
		apply { (builder, depth) => convertible.appendToJson(builder, depth) }
	
	/**
	 * @param f A function that accepts two parameters:
	 *          1. A JSON string builder
	 *          1. Recursive call depth
	 *
	 *          And appends something to the string builder.
	 *
	 * @return A new JsonAppendix wrapper for the specified function
	 */
	def apply(f: (mutable.Growable[Char], Int) => Unit) = new JsonAppendix(f)
}

/**
 * Common trait for prepared JSON conversions.
 * Intended (exclusively) to be used in Value-to-JSON conversion.
 * @author Mikko Hilpinen
 * @since 14.10.2026, v2.9
 */
class JsonAppendix(f: (mutable.Growable[Char], Int) => Unit) extends JsonConvertible
{
	override def appendToJson(jsonBuilder: mutable.Growable[Char], depth: Int): Unit = f(jsonBuilder, depth)
}

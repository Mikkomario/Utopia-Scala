package utopia.access.model.header

import utopia.access.model.header.HeaderValues._HeaderValues
import utopia.flow.generic.model.immutable.Value
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.util.Mutate

object HeaderValues
{
	// ATTRIBUTES   -----------------------
	
	private val comma = ","
	
	
	// OTHER    ---------------------------
	
	def apply[A](values: Seq[A], separator: String = comma)(toText: A => String = _.toString)
	            (implicit toValue: A => ValueConvertible): HeaderValues[A] =
		new _HeaderValues[A](values, separator, toText, v => toValue(v).toValue)
	
	
	// NESTED   ---------------------------
	
	private class _HeaderValues[A](override val parsed: Seq[A], override val separator: String,
	                               toText: A => String, conversion: A => Value)
		extends HeaderValues[A]
	{
		override protected def itemToText(item: A): String = toText(item)
		override protected def itemToValue(item: A): Value = conversion(item)
	}
}

/**
 * Common trait for collection-based header values
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait HeaderValues[A] extends HeaderValue[Seq[A]]
{
	// ABSTRACT --------------------------
	
	// TODO: Remove these
	/**
	 * @return A separator placed between individual header items
	 */
	def separator: String
	
	/**
	 * @param item An individual header item
	 * @return A text representation of that item
	 */
	protected def itemToText(item: A): String
	/**
	 * @param item An individual item
	 * @return A value based on that item
	 */
	protected def itemToValue(item: A): Value
	
	
	// IMPLEMENTED  ----------------------
	
	override def text: String = parsed.iterator.map(itemToText).mkString(separator)
	override def toValue: Value = parsed.view.map(itemToValue).toOptimizedSeq
	
	
	// OTHER    --------------------------
	
	def mapEach(f: Mutate[A]): HeaderValues[A] = new _HeaderValues[A](parsed.map(f), separator, itemToText, itemToValue)
}

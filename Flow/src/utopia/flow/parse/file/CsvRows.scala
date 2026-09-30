package utopia.flow.parse.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.mutable.builder.BuilderExtensions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.{FromSource, OpenSource, Regex}
import utopia.flow.util.StringExtensions._

import scala.io.Source
import scala.language.implicitConversions

/**
 * An interface for parsing CSV rows from files, streams, etc.
 * @author Mikko Hilpinen
 * @since 29.09.2026, v2.9
 */
object CsvRows
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * Separator used by default (comma).
	 */
	val defaultSeparator = ','
	
	private val doubleQuoteR = Regex.escape('"') * 2
	
	val factory = CsvRowsFactory(defaultSeparator, ignoreEmptyStringValues = false)
	
	
	// IMPLICIT  --------------------------
	
	// Implicitly treats this object as a factory
	implicit def objectAsFactory(o: CsvRows.type): CsvRowsFactory = o.factory
	
	
	// OTHER    ---------------------------
	
	private def cleanValue(original: String) = {
		val trimmed = original.trim
		val unquoted = {
			// Case: Escaped value => Removes the leading '
			if (trimmed.startsWith("'"))
				trimmed.drop(1)
			// Case: Quoted value => Removes the quotes, except if they're double quotes
			else if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
				val len = trimmed.length
				if (len >= 4 && trimmed(1) == '"' && trimmed(len - 2) == '"')
					trimmed
				else
					trimmed.slice(1, len - 1)
			}
			// Case: Not quoted => Keeps as is
			else
				trimmed
		}
		// Replaces double quotes with single quotes
		unquoted.replaceEachMatchOf(doubleQuoteR, "\"")
	}
	
	
	// NESTED   ---------------------------
	
	case class CsvRowsFactory(separator: Char, ignoreEmptyStringValues: Boolean)
	{
		// COMPUTED -----------------------
		
		/**
		 * @return A copy of this factory that expects semicolon-separated (;) input.
		 */
		def semicolonSeparated = separatedBy(';')
		
		/**
		 * @return A copy of this factory that doesn't include empty values in the returned models
		 */
		def withoutEmptyValues = copy(ignoreEmptyStringValues = true)
		
		/**
		 * @return An interface for iterating through the raw row string values instead of models.
		 */
		def iterateRaw = new IterateRawCsvRowsFrom(separator)
		
		/**
		 * @param jsonParser Implicit JSON parser used for parsing individual column values
		 * @return An interface for buffering all rows
		 */
		def from(implicit jsonParser: JsonParser) = new CsvRowsFrom(separator, ignoreEmptyStringValues)
		/**
		 * @param jsonParser Implicit JSON parser used for parsing individual column values
		 * @return An interface for iterating through rows
		 */
		def iterate(implicit jsonParser: JsonParser) = new IterateCsvRowsFrom(separator, ignoreEmptyStringValues)
		
		
		// OTHER    -----------------------
		
		/**
		 * @param separator Column-separator to apply
		 * @return A copy of this factory that uses the specified column separator
		 */
		def separatedBy(separator: Char) = copy(separator = separator)
		
		/**
		 * @param skipEmpty Whether empty values should be ignored / omitted from the resulting models
		 * @return A copy of this factory applying the specified setting
		 */
		def withoutEmptyValuesIf(skipEmpty: Boolean) = copy(ignoreEmptyStringValues = skipEmpty)
	}
	
	class IterateRawCsvRowsFrom(separator: Char) extends OpenSource[Iterator[IndexedSeq[String]]]
	{
		override protected def presentSource[A](source: Source, processor: Iterator[IndexedSeq[String]] => A): A =
			processor(new CsvRowsIterator(source, separator))
	}
	
	class IterateCsvRowsFrom(separator: Char, ignoreEmptyStringValues: Boolean)(implicit jsonParser: JsonParser)
		extends OpenSource[Iterator[Model]]
	{
		override protected def presentSource[A](source: Source, processor: Iterator[Model] => A): A = {
			// Splits and cleans the line entries
			val rowsIter = new CsvRowsIterator(source, separator)
			// Looks for the header row
			rowsIter.nextOption() match {
				case Some(headers) =>
					// Converts the remaining rows into models using the discovered headers
					processor(rowsIter.map { line =>
						val namedValuesIter = {
							if (ignoreEmptyStringValues)
								headers.iterator.zip(line).filter { _._2.nonEmpty }
							else
								headers.iterator.zip(line.view.padTo(headers.size, ""))
						}
						Model.withConstants(
							namedValuesIter
								.map { case (header, value) => Constant(header, jsonParser.valueOf(value)) }
								.toOptimizedSeq)
					})
				// Case: Empty document
				case None => processor(Iterator.empty)
			}
		}
	}
	
	class CsvRowsFrom(separator: Char, ignoreEmptyStringValues: Boolean)(implicit jsonParser: JsonParser)
		extends FromSource[Iterator[Model], IndexedSeq[Model]]
	{
		// ATTRIBUTES   --------------------
		
		override protected val open: OpenSource[Iterator[Model]] =
			new IterateCsvRowsFrom(separator, ignoreEmptyStringValues)
		
		
		// IMPLEMENTED  -------------------
		
		override protected def buffer(input: Iterator[Model]): IndexedSeq[Model] = input.toOptimizedSeq
	}
	
	private class CsvRowsIterator(source: Iterator[Char], separator: Char) extends Iterator[IndexedSeq[String]]
	{
		// IMPLEMENTED  -------------------
		
		override def hasNext: Boolean = source.hasNext
		
		override def next(): IndexedSeq[String] = {
			// Starts building the row
			val rowBuilder = OptimizedIndexedSeq.newBuilder[String].mapInput(cleanValue)
			val fieldBuilder = new StringBuilder()
			var insideQuote = false
			var quoteStarted = false // Marks whether the last character was a starting quote
			var completed = false
			
			// Iterates over the source characters until the row is completed
			while (!completed && source.hasNext) {
				source.next() match {
					// Case: Quotation
					case '"' =>
						// Case: Two quotation characters back-to-back => Interprets as an escaped quotation
						if (quoteStarted) {
							fieldBuilder += '"'
							insideQuote = false
							quoteStarted = false
						}
						// Case: Ending quote => Returns to normal mode
						else if (insideQuote)
							insideQuote = false
						// Case: Starting quote => Enters quote mode
						else {
							insideQuote = true
							quoteStarted = true
						}
					// Case: Non-escaped line-break => Finishes this row
					case '\r' | '\n' if !insideQuote => completed = true
					// Case: Another character
					case c =>
						quoteStarted = false
						// Case: Column separator outside quotes => Finishes this field
						if (!insideQuote && c == separator) {
							rowBuilder += fieldBuilder.result()
							fieldBuilder.clear()
						}
						// Case: Another character or a quoted separator => Adds to the current field
						else
							fieldBuilder += c
				}
			}
			// Adds the last field
			if (fieldBuilder.nonEmpty)
				rowBuilder += fieldBuilder.result()
			
			rowBuilder.result()
		}
	}
}

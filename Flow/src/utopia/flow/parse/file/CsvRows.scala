package utopia.flow.parse.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, Single}
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.{FromSource, OpenSource, Regex}
import utopia.flow.util.NotEmpty
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
	 * Separator used by default, a quotation-aware comma.
	 */
	val defaultSeparator = Regex.comma.ignoringQuotations
	
	private val quoteR = Regex.escape('"')
	private val doubleQuoteR = quoteR * 2
	
	val factory = CsvRowsFactory(defaultSeparator, enableMultiLine = false, ignoreEmptyStringValues = false)
	
	
	// IMPLICIT  --------------------------
	
	// Implicitly treats this object as a factory
	implicit def objectAsFactory(o: CsvRows.type): CsvRowsFactory = o.factory
	
	
	// OTHER    ---------------------------
	
	private def splitLines(linesIter: Iterator[String], separator: Regex, enableMultiLine: Boolean) =
	{
		if (enableMultiLine)
			new MultiLineRowIterator(linesIter.dropWhile { _.isEmpty }, separator)
		else
			linesIter.filter { _.nonEmpty }.map { _.splitIterator(separator).map(cleanValue).toOptimizedSeq }
	}
	
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
	
	case class CsvRowsFactory(separator: Regex, enableMultiLine: Boolean, ignoreEmptyStringValues: Boolean)
	{
		// COMPUTED -----------------------
		
		/**
		 * @return A copy of this factory that expects semicolon-separated (;) input.
		 */
		def semicolonSeparated = separatedBy(Regex.semicolon.ignoringQuotations)
		
		/**
		 * @return A copy of this factory that supports multiline input
		 */
		def multiline = copy(enableMultiLine = true)
		/**
		 * @return A copy of this factory that doesn't include empty values in the returned models
		 */
		def withoutEmptyValues = copy(ignoreEmptyStringValues = true)
		
		/**
		 * @return An interface for iterating through the raw row string values instead of models.
		 */
		def iterateRaw = new IterateRawCsvRowsFrom(separator, enableMultiLine)
		
		/**
		 * @param jsonParser Implicit JSON parser used for parsing individual column values
		 * @return An interface for buffering all rows
		 */
		def from(implicit jsonParser: JsonParser) = new CsvRowsFrom(separator, enableMultiLine, ignoreEmptyStringValues)
		/**
		 * @param jsonParser Implicit JSON parser used for parsing individual column values
		 * @return An interface for iterating through rows
		 */
		def iterate(implicit jsonParser: JsonParser) =
			new IterateCsvRowsFrom(separator, enableMultiLine, ignoreEmptyStringValues)
		
		
		// OTHER    -----------------------
		
		/**
		 * @param separator Column-separator to apply
		 * @return A copy of this factory that uses the specified column separator
		 */
		def separatedBy(separator: Regex) = copy(separator = separator)
		
		/**
		 * @param skipEmpty Whether empty values should be ignored / omitted from the resulting models
		 * @return A copy of this factory applying the specified setting
		 */
		def withoutEmptyValuesIf(skipEmpty: Boolean) = copy(ignoreEmptyStringValues = skipEmpty)
	}
	
	class IterateRawCsvRowsFrom(separator: Regex, enableMultiLine: Boolean)
		extends OpenSource[Iterator[IndexedSeq[String]]]
	{
		override protected def presentSource[A](source: Source, processor: Iterator[IndexedSeq[String]] => A): A =
			processor(splitLines(source.getLines(), separator, enableMultiLine))
	}
	
	class IterateCsvRowsFrom(separator: Regex, enableMultiLine: Boolean, ignoreEmptyStringValues: Boolean)
	                        (implicit jsonParser: JsonParser)
		extends OpenSource[Iterator[Model]]
	{
		override protected def presentSource[A](source: Source, processor: Iterator[Model] => A): A = {
			// Splits and cleans the line entries
			val rowsIter = splitLines(source.getLines(), separator, enableMultiLine)
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
	
	class CsvRowsFrom(separator: Regex, enableMultiLine: Boolean, ignoreEmptyStringValues: Boolean)
	                 (implicit jsonParser: JsonParser)
		extends FromSource[Iterator[Model], IndexedSeq[Model]]
	{
		// ATTRIBUTES   --------------------
		
		override protected val open: OpenSource[Iterator[Model]] =
			new IterateCsvRowsFrom(separator, enableMultiLine, ignoreEmptyStringValues)
		
		
		// IMPLEMENTED  -------------------
		
		override protected def buffer(input: Iterator[Model]): IndexedSeq[Model] = input.toOptimizedSeq
	}
	
	private class MultiLineRowIterator(source: Iterator[String], separator: Regex) extends Iterator[IndexedSeq[String]]
	{
		// IMPLEMENTED  -------------------
		
		override def hasNext: Boolean = source.hasNext
		
		override def next(): IndexedSeq[String] = {
			// Builder that collects additional line entries
			val multiLineBuilder = OptimizedIndexedSeq.newBuilder[IndexedSeq[String]]
			var lastLine = source.next().split(separator)
			// Continues adding multiline entries as long as the last line is incomplete / broken
			while (startsMultiLine(lastLine) && source.hasNext) {
				multiLineBuilder += lastLine
				
				// Finds a row that contains at least one non-escaped quote => This might end the multiline sequence
				var foundQuotes = false
				while (!foundQuotes && source.hasNext) {
					val line = source.next()
					quoteR.startIndexIteratorIn(line)
						.find { i =>
							// Makes sure the quote in question is not escaped
							Pair(1, -1).count { step => line.lift(i + step).contains('"') } % 2 == 0
						} match
					{
						// Case: Found a line with 1 or more quotes
						//       => Checks whether the multiline sequence breaks or continues
						case Some(quoteStartIndex) =>
							foundQuotes = true
							// Finds where the first actual column-separator is located
							// (doesn't include separators up to the quote)
							separator.rangesIteratorIn(line).find { _.start > quoteStartIndex } match {
								case Some(firstSeparatorRange) =>
									lastLine = line.take(firstSeparatorRange.start) +:
										line.drop(firstSeparatorRange.end).split(separator)
								
								case None => multiLineBuilder += Single(line)
							}
						// Case: This line didn't contain any quotes => Adds it as a single entry
						case None => multiLineBuilder += Single(line)
					}
				}
			}
			
			// Checks whether this entry spanned one or multiple lines
			val allLineParts = NotEmpty(multiLineBuilder.result()) match {
				// Case: Multi-liner => Merges the lines into one
				case Some(precedingLines) =>
					val mergeBuilder = OptimizedIndexedSeq.newBuilder[String]
					// Adds elements before the first broken entry
					mergeBuilder ++= precedingLines.head.view.dropRight(1)
					// Prepares to continue the first broken entry
					var incomplete = precedingLines.head.last
					
					// Processes the remaining lines
					(precedingLines.view.tail.iterator ++ Single(lastLine)).foreach { line =>
						line.only match {
							// Case: Only contains one (joining) element => Appends it to the current incomplete entry
							case Some(onlyElement) => incomplete = s"$incomplete\n$onlyElement"
							// Case: Contains multiple elements => Finishes the incomplete entry and starts a new one
							case None =>
								mergeBuilder += s"$incomplete\n${ line.head }"
								// Adds the elements in-between as they are
								mergeBuilder ++= line.view.slice(1, line.length - 1)
								incomplete = line.last
						}
					}
					// Adds the last line entry
					mergeBuilder += incomplete
					
					mergeBuilder.result()
					
				// Case: One-liner => Ready
				case None => lastLine
			}
			
			// Trims and cleans the line parts
			allLineParts.map(cleanValue)
		}
		
		
		// OTHER    ------------------------
		
		/**
		 * Check whether the specified line starts a multi-line sequence
		 * @param line A raw CSV row
		 * @return Whether the specified row is incomplete / continues on another line.
		 */
		private def startsMultiLine(line: IndexedSeq[String]) = {
			// Multiline if the last value starts with a non-escaped quote and does not end that quote.
			val lastValue = line.last.trim
			lastValue.startsWith("\"") && !lastValue.lift(1).contains('"') &&
				(!lastValue.endsWith("\"") || lastValue.lift(lastValue.length - 2).contains('"'))
		}
	}
}

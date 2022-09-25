package utopia.flow.parse.json

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.{DoubleType, IntType, LongType}
import utopia.flow.parse.string.StringFrom
import utopia.flow.collection.CollectionExtensions._

import java.io.{File, InputStream}
import scala.collection.immutable.VectorBuilder
import scala.io.Codec
import scala.util.Try

/**
  * Used for parsing JSON strings into objects
  * @author Mikko Hilpinen
  * @since 22.7.2019, v1.6+
  */
object JSONReader extends JsonParser
{
	val defaultEncoding = Codec.UTF8
	
	/**
	  * Parses the contents of a stream
	  * @param inputStream Stream from which data is read
	  * @param encoding Encoding used in stream (default = UTF-8)
	  * @return Value parsed from stream data
	  */
	def apply(inputStream: InputStream, encoding: String): Try[Value] = StringFrom.stream(inputStream, encoding).flatMap(apply)
	
	/**
	 * Parses the contents of a stream
	 * @param inputStream Stream from which data is read
	 * @param encoding Encoding used in stream (default = UTF-8)
	 * @return Value parsed from stream data
	 */
	def apply(inputStream: InputStream, encoding: Codec): Try[Value] = StringFrom.stream(inputStream)(encoding).flatMap(apply)
	
	/**
	 * Parses the contents of a stream using UTF-8 encoding
	 * @param inputStream Stream from which data is read
	 * @return Value parsed from stream data
	 */
	def apply(inputStream: InputStream): Try[Value] = apply(inputStream, defaultEncoding)
	
	/**
	  * Parses the contents of a file. Expects file to be json-formatted.
	  * @param jsonFile A file that contains json data
	  * @return Value parsed from the file. May fail if the file couldn't be found / read or if file contents were malformed
	  */
	def apply(jsonFile: File): Try[Value] = StringFrom.file(jsonFile).flatMap(apply)
	
	/**
	  * Parses a model out of JSON data. The parsing will start at the first object start ('{') and
	  * end at the end of that object. Only a single model will be parsed, even if there are multiple
	  * siblings available
	  * @param json The JSON string
	  * @return The parsed model. Fails if the json was malformed
	  */
	@deprecated("Please use apply(String) instead", "v1.6")
	def parseSingle(json: String) = apply(json).map { _.getModel }
	
	/**
	  * Parses models out of JSON data. The parsing will start at the first object start ('{') and
	  * continue until each of the objects have been parsed. If the data is malformed, the parsing
	  * will be stopped.
	  * @param json The JSON string
	  * @return The parsed models
	  */
	@deprecated("Please use apply(String) instead", "v1.6")
	def parseMany(json: String) = apply(json).map { _.getVector.map { _.getModel } }
	
	/**
	  * Parses a single value from the provided JSON. Only the first value will be read, whether it
	  * is an array, object or a simple value. Fails if the provided json is malformed.
	  */
	@deprecated("Please use apply(String) instead", "v1.6")
	def parseValue(json: String) = apply(json)
	
	/**
	  * Parses the json and wraps it in a value
	  * @param json Json data
	  * @return Parsed value or failure if json was invalid
	  */
	def apply(json: String) =
	{
		Try
		{
			// First finds the indices of each meaningful json marker
			val indices = allIndicesOf(Set(Quote.marker, Separator.marker, ArrayStart.marker, ArrayEnd.marker,
				ObjectStart.marker, ObjectEnd.marker), json)
			
			// Finds quoted areas and filters other markers into non-quoted areas
			val (quotes, nonQuotes) = separateQuotes(indices(Quote.marker), json.length)
			val arrayRanges = openCloseRangesFrom(onlyIndicesInRanges(indices(ArrayStart.marker), nonQuotes),
				onlyIndicesInRanges(indices(ArrayEnd.marker), nonQuotes))
			val objectRanges = openCloseRangesFrom(onlyIndicesInRanges(indices(ObjectStart.marker), nonQuotes),
				onlyIndicesInRanges(indices(ObjectEnd.marker), nonQuotes))
			
			val eventRanges = (quotes.map { _ -> Quote } ++ arrayRanges.map { _ -> ArrayStart } ++
				objectRanges.map { _ -> ObjectStart }).sortBy { _._1.start }
			
			// Finds the first event, which determines the type of parsed item
			if (eventRanges.isEmpty)
			{
				// If there are no events, parses a simple value
				parseNonStringValueFrom(json)
			}
			else
			{
				val (firstRange, firstEvent) = eventRanges.head
				parse(firstEvent, firstRange, Data(json, eventRanges,
					onlyIndicesInRanges(indices(Separator.marker), nonQuotes)), State(0, 1)).parsed
			}
		}
	}
	
	// Event count should be increased for this event already when calling this method
	private def parse(eventType: JSONReadEvent, range: Range, data: Data, startState: State): Result =
	{
		eventType match
		{
			case Quote => Result(parseQuote(range, data.json), startState)
			case ArrayStart => parseArray(range, data, startState)
			case ObjectStart => parseObject(range, data, startState)
			case _ => throw new InvalidFormatException(s"Unexpected marker ${eventType.marker} at index ${range.start}")
		}
	}
	
	// Remember to increase usedEventCount after calling
	private def parseQuote(range: Range, json: String) = json.substring(range.start + 1, range.end)
	
	private def parseObject(range: Range, data: Data, startState: State) =
	{
		val buffer = new VectorBuilder[Constant]()
		
		var isCompleted = false
		var usedEventCount = startState.usedEventCount
		var usedSeparatorCount = startState.usedSeparatorCount
		
		while (!isCompleted)
		{
			// If there are no more events left, completes the object
			val nextEvent = nextEventInRange(data, usedEventCount, range)
			
			if (nextEvent.isEmpty)
				isCompleted = true
			else
			{
				// the next event should be a quote
				val (quoteRange, quoteEvent) = nextEvent.get
				if (quoteEvent != Quote)
					throw new InvalidFormatException(s"Expected property name at index ${
						quoteRange.start
					}, instead found ${quoteEvent.marker}")
				
				// Parses quote as property name
				val propertyName = parseQuote(quoteRange, data.json)
				usedEventCount += 1
				
				// Quote should be followed by either a) a new marker (quote, object, array) or b) some string
				// followed by a separator or c) Neither if approaching the end of json string or end of this object
				val separatorBeforeEvent = nextSeparatorBeforeNextMarker(data, usedSeparatorCount, usedEventCount)
				
				if (separatorBeforeEvent.exists { _ > range.end })
				{
					buffer += Constant(propertyName, parseStringAssignment(data.json, quoteRange.end, range.end))
					isCompleted = true
				}
				else if (separatorBeforeEvent.isDefined)
				{
					// In case of a simple string, parses it and assigns it to a constant
					// Expects the string to contain an assignment portion, which is not parsed of course
					buffer +=Constant(propertyName, parseStringAssignment(data.json, quoteRange.end,
						separatorBeforeEvent.get))
					
					// Moves to the next separator afterwards
					usedSeparatorCount += 1
				}
				else if (usedEventCount < data.eventCount)
				{
					// In case of an event, parses it as a value and assigns to a constant
					val (nextEventRange, nextEventType) = data.eventRanges(usedEventCount)
					val eventParseResult = parse(nextEventType, nextEventRange, data,
						State(usedSeparatorCount, usedEventCount + 1))
					
					usedSeparatorCount = eventParseResult.newState.usedSeparatorCount
					usedEventCount = eventParseResult.newState.usedEventCount
					buffer += Constant(propertyName, eventParseResult.parsed)
					
					// Continues by moving to the next separator, if there is one, or object end if there are no separators left
					val nextSeparator = nextSeparatorInRange(data, usedSeparatorCount, range)
					
					if (nextSeparator.isDefined)
						usedSeparatorCount += 1
					else
						isCompleted = true
				}
				else
				{
					buffer +=Constant(propertyName, parseStringAssignment(data.json, quoteRange.end, range.end))
					isCompleted = true
				}
			}
		}
		
		// Returns the resulting model
		Result(Model.withConstants(buffer.result()), State(usedSeparatorCount, usedEventCount))
	}
	
	private def parseStringAssignment(json: String, quoteEndIndex: Int, endIndex: Int) =
	{
		val stringAfterQuote = json.substring(quoteEndIndex + 1, endIndex)
		val assignmentIndex = stringAfterQuote.indexOf(Assignment.marker)
		if (assignmentIndex < 0)
			throw new InvalidFormatException(
				s"Expected assigment after index $quoteEndIndex. Instead found $stringAfterQuote")
		
		parseNonStringValueFrom(stringAfterQuote.substring(assignmentIndex + 1).trim)
	}
	
	private def parseArray(range: Range, data: Data, startState: State) =
	{
		val buffer = new VectorBuilder[Value]()
		
		var isCompleted = false
		var usedEventCount = startState.usedEventCount
		var usedSeparatorCount = startState.usedSeparatorCount
		var lastSeparatorIndex = range.start
		
		while (!isCompleted)
		{
			// Finds the separators before the next marker
			// NB: If this array ends before the next marker, only searches those
			val nextEvent = nextEventInRange(data, usedEventCount, range)
			if (nextEvent.isEmpty)
			{
				val separatorsBeforeEnd = separatorsBefore(data, usedSeparatorCount, range.end)
				
				// If there are no more separators, that means that there is only 0-1 value(s) left
				if (separatorsBeforeEnd.isEmpty)
				{
					val remainingString = data.json.substring(lastSeparatorIndex + 1, range.end).trim
					// Only time a string is left unparsed is in case of an empty array filled with whitespaces
					if (remainingString.nonEmpty || usedSeparatorCount != startState.usedSeparatorCount)
						buffer += parseNonStringValueFrom(remainingString)
				}
				else
				{
					// If there are separators, simply parses each range separately
					separatorsBeforeEnd.foreach { separatorIndex =>
						
						buffer += parseNonStringValueFrom(data.json.substring(lastSeparatorIndex + 1, separatorIndex).trim)
						lastSeparatorIndex = separatorIndex
						usedSeparatorCount += 1
					}
					
					// Handles the last portion as well
					buffer += parseNonStringValueFrom(data.json.substring(lastSeparatorIndex + 1, range.end).trim)
				}
				
				isCompleted = true
			}
			else
			{
				// In case there are still some markers left, finds the separators before the next marker
				val (nextEventRange, nextMarker) = nextEvent.get
				val separatorsBeforeNextMarker = separatorsBefore(data, usedSeparatorCount, nextEventRange.start)
				
				// If there are no separators before the next marker, there are no values either
				// If there are, however, parses strings between markers
				separatorsBeforeNextMarker.foreach { separatorIndex =>
					
					buffer += parseNonStringValueFrom(data.json.substring(lastSeparatorIndex + 1, separatorIndex).trim)
					lastSeparatorIndex = separatorIndex
					usedSeparatorCount += 1
				}
				
				// Finally handles the marked area
				val readResult = parse(nextMarker, nextEventRange, data,
					State(usedSeparatorCount, usedEventCount + 1))
				
				usedEventCount = readResult.newState.usedEventCount
				usedSeparatorCount = readResult.newState.usedSeparatorCount
				buffer += readResult.parsed
				
				// Moves to the next separator or to the end of array, whichever comes first
				if (nextSeparatorInRange(data, usedSeparatorCount, range).isDefined)
					usedSeparatorCount += 1
				else
					isCompleted = true
			}
		}
		
		// Returns the resulting array
		Result(buffer.result(), State(usedSeparatorCount, usedEventCount))
	}
	
	private def nextEventInRange(data: Data, usedEventCount: Int, range: Range) =
	{
		if (usedEventCount >= data.eventCount)
			None
		else
		{
			val next = data.eventRanges(usedEventCount)
			if (next._1.start > range.end)
				None
			else
				Some(next)
		}
	}
	
	private def nextSeparatorInRange(data: Data, usedSeparatorCount: Int, range: Range) =
	{
		if (usedSeparatorCount >= data.separatorCount)
			None
		else
		{
			val nextSeparator = data.separatorIndices(usedSeparatorCount)
			if (nextSeparator < range.end)
				Some(nextSeparator)
			else
				None
		}
	}
	
	private def nextSeparatorBeforeNextMarker(data: Data, usedSeparatorCount: Int, usedEventCount: Int) =
	{
		if (usedSeparatorCount >= data.separatorCount)
			None
		else if (usedEventCount >= data.eventCount)
			Some(data.separatorIndices(usedSeparatorCount))
		else
		{
			val nextSeparator = data.separatorIndices(usedSeparatorCount)
			if (nextSeparator < data.eventRanges(usedEventCount)._1.start)
				Some(nextSeparator)
			else
				None
		}
	}
	
	private def parseNonStringValueFrom(string: String): Value =
	{
		// 'null' (without quotations) is a synonym for empty value
		if (string.isEmpty || string.equalsIgnoreCase("null"))
			Value.empty
		// 'true' / 'false' are considered to be boolean
		else if (string.equalsIgnoreCase("true"))
			true
		else if (string.equalsIgnoreCase("false"))
			false
		else
		{
			val parsedNumber =
			{
				// Double is the only number format that contains a '.'
				if (string.contains('.'))
					string withType DoubleType
				// Very long numbers are considered to be of type long
				else if (string.length() >= 10)
					string withType LongType
				else
					string withType IntType
			}
			
			// If the number couldn't be parsed, returns the value as a string instead
			parsedNumber.orElse(string)
		}
	}
	
	private def separatorsBefore(data: Data, usedCount: Int, beforeStringIndex: Int) =
	{
		if (data.separatorCount <= usedCount)
			Vector()
		else
		{
			val result = new VectorBuilder[Int]()
			var i = usedCount
			while (i < data.separatorCount && data.separatorIndices(i) < beforeStringIndex)
			{
				result += data.separatorIndices(i)
				i += 1
			}
			
			result.result()
		}
	}
	
	private def openCloseRangesFrom(openIndices: Seq[Int], closeIndices: Seq[Int]) =
	{
		// Sorts markers and then finds object pairs
		val allMarkers = (openIndices.map { _ -> true } ++ closeIndices.map { _ -> false }).sortBy { _._1 }
		
		var opened = List[Int]()
		val openCloseRanges = new VectorBuilder[Range]()
		allMarkers.foreach { case (index, isStart) =>
			if (isStart)
				opened = index +: opened
			else
			{
				if (opened.isEmpty)
					throw new InvalidFormatException(s"Array of object close before open at index $index")
				val lastObjectStart = opened.head
				opened = opened.tail
				openCloseRanges += (lastObjectStart to index)
			}
		}
		
		openCloseRanges.result()
	}
	
	private def separateQuotes(quoteIndices: Seq[Int], totalLength: Int) =
	{
		// If there are no quotes, treats all as non-quoted
		if (quoteIndices.isEmpty)
			Vector() -> Vector(0 until totalLength)
		// Groups the indices to pairs
		else if (quoteIndices.size % 2 != 0)
			throw new InvalidFormatException(s"JSON contained an uneven number of quotation markers")
		else
		{
			// Separates into quote- and non-quote sections (starts and ends are inclusive, but contain quote markers in quotes)
			val quotes = (quoteIndices.indices by 2).map { i => quoteIndices(i) to quoteIndices(i + 1) }
			if (quotes.nonEmpty)
			{
				val nonQuoteBuilder = new VectorBuilder[Range]()
				if (quotes.head.start != 0)
					nonQuoteBuilder += (0 to quotes.head.start)
				quotes.foreachWith(quotes.drop(1)) { (first, second) => if (second.start > first.end + 1)
					nonQuoteBuilder += (first.end + 1 until second.start) }
				if (quotes.last.end < totalLength - 1)
					nonQuoteBuilder += (quotes.last.end + 1 until totalLength)
				
				quotes -> nonQuoteBuilder.result()
			}
			else
				quotes -> Vector()
		}
	}
	
	private def onlyIndicesInRanges(indices: Vector[Int], ranges: Seq[Range]) =
	{
		indices.filter { index =>
		
			// Expects ranges to be ordered, so finds the first range that could contain the target index
			ranges.findMap { range =>
				
				if (range.end < index)
					None
				else if (range.contains(index))
					Some(true)
				else
					Some(false)
			
			}.getOrElse(false)
		}
	}
	
	private def allIndicesOf(targets: Set[Char], source: String) =
	{
		val results = targets.map { _ -> new VectorBuilder[Int]() }.toMap
		source.indices.foreach { i =>
			val c = source(i)
			targets.find { _ == c }.foreach { results(_) += i }
		}
		results.map { case (k, v) => k -> v.result() }
	}
}

private case class Result(parsed: Value, newState: State)

private case class State(usedSeparatorCount: Int, usedEventCount: Int)

private case class Data(json: String, eventRanges: Seq[(Range, JSONReadEvent)], separatorIndices: Seq[Int])
{
	val eventCount = eventRanges.size
	val separatorCount = separatorIndices.size
}

// These exceptions are thrown to interrupt the program flow when the provided JSON has invalid
// format
private class InvalidFormatException(message: String) extends Exception(message)

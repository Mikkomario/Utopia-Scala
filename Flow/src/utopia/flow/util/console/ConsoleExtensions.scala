package utopia.flow.util.console

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.time.{DateRange, Today, WeekDay}
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WeekDay.{Monday, Sunday}
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._

import java.time.{LocalDate, YearMonth}
import scala.io.StdIn
import scala.util.Try

/**
 * Introduces extensions targeted for command line / console use cases
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 */
object ConsoleExtensions
{
	// OTHER    -----------------------------
	
	/**
	 * A more advanced function for parsing a date from an (input) string.
	 * Allows use of relative and/or impartial dates, such as "today", "last tuesday", or "3.6".
	 * Expects dates, months and years to be separated with ".", and to be in ascending order.
	 * @param str String to convert into a date
	 * @param defaultMonth Month and year to supply when missing from the input.
	 *                     Default = current month & year.
	 * @return Parsed date. None if the input couldn't be parsed.
	 */
	def parseDate(str: String, defaultMonth: YearMonth = Today.yearMonth) = {
		str.toLowerCase match {
			case "today" => Some(Today.toLocalDate)
			case "yesterday" => Some(Today.yesterday)
			case "tomorrow" => Some(Today.tomorrow)
			case str =>
				// Case: Targeting some past week day
				if (str.startsWith("last"))
					WeekDay.matching(str.drop(4).trim).map { Today.previous(_) }
				// Case: Targeting some future week day
				else if (str.startsWith("next"))
					WeekDay.matching(str.drop(4).trim).map { Today.next(_) }
				// Case: Targets a specific date
				else {
					val parts = str.split('.').toVector
						.flatMap { _.trim.filter { _.isDigit }.dropWhile { _ == '0' }.toIntOption }
					NotEmpty(parts).flatMap { parts =>
						Try {
							LocalDate.of(
								parts.getOrElse(2, defaultMonth.year.value),
								parts.getOrElse(1, defaultMonth.month.value),
								parts.head
							)
						}.toOption
					}
				}
		}
	}
	
	
	// EXTENSIONS   -------------------------
	
	implicit class RichStdIn(val in: StdIn.type) extends AnyVal
	{
		/**
		 * Reads a line of text from this input. May display a prompt before requesting input.
		 * @param prompt Prompt to display before requesting for input. If empty, no prompt is displayed (default)
		 * @return Read line as a string
		 */
		def printAndReadLine(prompt: String) = {
			NotEmpty(prompt).foreach(println)
			in.readLine()
		}
		/**
		 * Reads a line of text from this input
		 * @param prompt Prompt to display before requesting input (default = empty = no prompt)
		 * @param retryPrompt Prompt used for asking the user for another input in case they specify an empty input -
		 *                    Will only be asked once (default = empty = no retry prompting)
		 * @return Read line. None if user wrote an empty line.
		 */
		def readNonEmptyLine(prompt: String = "", retryPrompt: String = ""): Option[String] = {
			val firstResult = NotEmpty(printAndReadLine(prompt))
			if (firstResult.isDefined || retryPrompt.isEmpty)
				firstResult
			else
				readNonEmptyLine(retryPrompt)
		}
		
		/**
		 * Reads a value from this input
		 * @param prompt Prompt to display before requesting input (default = empty = no prompt)
		 * @return Value read from the input. Empty if user specified an empty string.
		 */
		def read(prompt: String = ""): Value =
			readNonEmptyLine(prompt) match {
				case Some(line) => line
				case None => Value.empty
			}
		/**
		 * Reads a value from this input, supports json parsing
		 * @param prompt Prompt to display before requesting input (default = empty = no prompt)
		 * @return Value read from the input. Empty if user specified an empty string.
		 */
		def readJson(prompt: String = "")(implicit parser: JsonParser): Value =
			readNonEmptyLine(prompt) match {
				case Some(line) => parser.valueOf(line)
				case None => Value.empty
			}
		
		/**
		 * Asks the user a yes or no -question
		 * @param question Question to ask from the user (" (y/n)" is added after the question)
		 * @param default The default value to use when answer can't be interpreted (default = false)
		 * @return Answer given by the user (or the default)
		 */
		def ask(question: String, default: Boolean = false) = {
			val yesNo = if (default) "Y/n" else "y/N"
			readNonEmptyLine(s"$question $yesNo") match {
				case Some(answer) =>
					answer.head.toLower match {
						case 'y' => true
						case 'n' => false
						case _ => default
					}
				case None => default
			}
		}
		
		/**
		 * @return An iterator that reads new lines
		 */
		def readLineIterator = Iterator.continually { in.readLine() }
		/**
		 * @return An iterator that reads values
		 */
		def readIterator = Iterator.continually { read() }
		
		/**
		 * @param prompt Prompt shown before each new line
		 * @return An iterator that reads new lines
		 */
		def readLineIteratorWithPrompt(prompt: String) =
			Iterator.continually { printAndReadLine(prompt) }
		
		/**
		 * @return Reads lines until a non-empty response is given
		 */
		def readLineUntilNotEmpty(prompt: String = "") = {
			NotEmpty(prompt).foreach(println)
			readLineIterator.find { _.nonEmpty }.get
		}
		/**
		 * @return Reads lines while a non-empty response is given
		 */
		def readLineWhileNotEmpty(prompt: String = "") = {
			NotEmpty(prompt).foreach(println)
			readLineIterator.takeWhile { _.nonEmpty }.toVector
		}
		/**
		 * Reads lines until a non-empty value is given
		 */
		def readUntilNotEmpty(prompt: String = "") = {
			NotEmpty(prompt).foreach(println)
			readIterator.find { _.isDefined }.get
		}
		
		/**
		 * Reads either a valid input or an empty input
		 * @param initialPrompt Prompt shown before initial read (default = empty = no prompt)
		 * @param validate A function that validates the input. Returns Left[String] error message /
		 *                 instructions on failure and Right[A] valid input on success.
		 * @tparam A Type of valid item
		 * @return Read valid item or None if user provided an empty input
		 */
		def readValidOrEmpty[A](initialPrompt: String = "")(validate: Value => Either[String, A]) = {
			NotEmpty(initialPrompt).foreach(println)
			readIterator.findMap { input =>
				if (input.isEmpty)
					Some(None)
				else
					validate(input) match
					{
						case Right(valid) => Some(Some(valid))
						case Left(message) =>
							println(message)
							None
					}
			}.get
		}
		
		/**
		 * Requests the user to input a date. Allows various forms of input,
		 * defaulting current month and year where omitted.
		 * @param prompt Prompt to display to the user before requesting the date.
		 *               Additional input instructions are added after this prompt.
		 * @return Date specified by the user. None if the user didn't specify a date or if date-parsing failed.
		 */
		def readDate(prompt: String = "") = {
			NotEmpty(prompt).foreach(println)
			println("\tInstruction: Supported format is dd.mm.yyyy")
			println("\tIf some parts are left empty, current month and year are substituted")
			println("\tAlso supports options: today, yesterday, last Monday, next Tuesday, etc.")
			readNonEmptyLine().flatMap { parseDate(_) }
		}
		/**
		 * Reads a date range from user input.
		 * Allows for various input styles relative to the current date, such as "next week".
		 * @param prompt Prompt to display before asking for a date range.
		 *               Date format instructions will be added to the end of the prompt.
		 * @return Parsed input.
		 *         None if the user didn't specify input or if input couldn't be parsed.
		 */
		def readDateRange(prompt: String = "") = {
			NotEmpty(prompt).foreach(println)
			println("\tInstruction: Supported format is dd.mm.yyyy, you may specify a range with '-'")
			println("\tIf some parts are left empty, current month and year are substituted")
			println("\tYou may also omit month and/or year in the first date if they are the same as in the following date. ")
			println("\tAlso supports options: today, yesterday, this month, last week, next Monday, etc.")
			readNonEmptyLine().flatMap { input =>
				if (input.containsIgnoreCase("year"))
					thisLastOrNext(input, Today.year) { _.next } { _.previous }
						.orElse { input.drop(4).trim.int.map { _.year } }
						.map { _.dates }
				else if (input.containsIgnoreCase("month"))
					thisLastOrNext(input, Today.yearMonth) { _.next } { _.previous }.map { _.dates }
				else if (input.containsIgnoreCase("week")) {
					lazy val t = Today.toLocalDate
					thisLastOrNext(input,
						Span(t.previous(Monday, includeSelf = true), t.next(Sunday, includeSelf = true))) {
						_.mapEnds { _ + 7 } } { _.mapEnds { _ - 7 } }.map { DateRange(_) }
				}
				else {
					val (startPart, endPart) = input.splitAtFirst("-").map { _.trim }.toTuple
					val endDate = parseDate(endPart)
					val startDate = parseDate(startPart, endDate.getOrElse(Today.toLocalDate).yearMonth)
					startDate match {
						case Some(start) =>
							endDate match {
								// Case: Both start and end specified
								case Some(end) => Some(DateRange.inclusive(start, end))
								// Case: Only start date specified
								case None =>
									// Case: Start date ends with - => Continues to today
									if (input.contains('-'))
										Some(DateRange.inclusive(start, Today))
									// Case: Only start date
									else
										Some(DateRange.single(start))
							}
						case None => endDate.map(DateRange.single)
					}
				}
			}
		}
		
		/**
		 * Requests the user to select one of the specified options
		 * @param options Options to select from, where the second values are descriptions to list to the user
		 * @param target A string representing the options in plural form. Default = "items".
		 * @param verb Verb used for the action of selecting an option. Default = "select".
		 * @param maxListCount Maximum number of items that may be listed on the console.
		 * @tparam A Type of items selected from
		 * @return The item selected by the user.
		 *         None if no options were available, or if the user chose not to select an item.
		 */
		def selectFrom[A](options: Seq[(A, String)], target: String = "items", verb: String = "select",
		                  maxListCount: Int = 20) =
			_selectFrom(options, None, target, verb, maxListCount)
		/**
		 * Requests the user to select one of the specified options.
		 * Allows the user to add a new value, if they so desire.
		 * @param options Options to select from, where the second values are descriptions to list to the user
		 * @param target A string representing the options in plural form. Default = "items".
		 * @param verb Verb used for the action of selecting an option. Default = "select".
		 * @param maxListCount Maximum number of items that may be listed on the console.
		 * @param addNew A function that, when called, prompts the user for the data needed in order to add a new item.
		 *               Returns None if the user cancelled the process.
		 * @tparam A Type of items selected or added.
		 * @return The selected or added item.
		 */
		def selectFromOrAdd[A](options: Seq[(A, String)], target: String = "items", verb: String = "select",
		                       maxListCount: Int = 20)(addNew: => Option[A]) =
			_selectFrom(options, Some({ () => addNew }), target, verb, maxListCount)
		
		// Allows the user to select from the specified options.
		// Option description is the second item in 'options'
		// If 'addNew' is defined, the user is allowed to add new items
		// Please specify 'target' in plural form
		private def _selectFrom[A](options: Seq[(A, String)], addNew: Option[() => Option[A]],
		                           target: String, verb: String, maxListCount: Int): Option[A] =
		{
			options.emptyOneOrMany match {
				// Case: No options available
				case None =>
					addNew match {
						// Case: Insert possible => Asks the user if they want to insert a new item
						case Some(addNew) =>
							if (ask(s"No $target were found. Would you like to add a new one?"))
								addNew()
							else
								None
						// Case: Insert not possible => Couldn't find any item
						case None =>
							println(s"No $target were found.")
							None
					}
					
				// Case: Only one option available => Asks the user whether they want to select it
				case Some(Left(only)) =>
					val addNote = {
						if (addNew.isDefined)
							"\nPlease note that if you select \"no\", you may still add a new item."
						else
							""
					}
					if (ask(s"Do you want to $verb ${ only._2 }?$addNote", default = addNew.isEmpty))
						Some(only._1)
					// Also allows insertion as a secondary option, if possible
					else
						addNew.filter { _ => ask("Do you want to add a new one instead?") }.flatMap { _() }
						
				// Case: Multiple options to choose from => Asks the user to filter or to select one
				case Some(Right(options)) =>
					val tooMany = options.hasSize > maxListCount
					if (tooMany) {
						println(s"There are ${options.size} $target to $verb from")
						println("Please narrow the selection by specifying an additional filter (empty cancels)")
						if (addNew.isDefined)
							println("Hint: you can also add a new item by typing \"new\"")
					}
					else {
						options.indices.foreach { index => println(s"\t${index + 1}: ${options(index)._2}") }
						if (addNew.isDefined)
							println("0: Create new")
						println("Please select the correct index or narrow the selection by typing text (empty cancels)")
					}
					readNonEmptyLine().flatMap { filter =>
						addNew.filter { _ => filter ~== "new" } match {
							// Case: Selected "new" => Adds a new item
							case Some(addNew) => addNew()
							case None =>
								val targetIndex = if (tooMany) None else filter.int
								targetIndex match {
									// Case: Selected an index
									case Some(index) =>
										// Case: Selected 0 => Adds a new item
										if (index == 0 && addNew.isDefined)
											addNew.flatMap { _() }
										// Case: Selected another index => Picks that item, if the index is valid
										else
											options.lift(index - 1) match {
												case Some(item) => Some(item._1)
												case None =>
													println(s"Specified index $index didn't match listed option. Please try again.")
													_selectFrom(options, addNew, target, verb, maxListCount)
											}
									// Case: Specified text => Filters options
									case None =>
										val narrowed = options.filter { _._2.containsIgnoreCase(filter) }
										// Case: No matches => Lists the same options again
										if (narrowed.isEmpty) {
											println(s"\"$filter\" didn't match any $target, please try again")
											_selectFrom(options, addNew, target, verb, maxListCount)
										}
										else
											narrowed.find { _._2 ~== filter } match {
												// Case: Specified an exact match => Selects that one
												case Some(matchingResult) => Some(matchingResult._1)
												case None =>
													narrowed.only match {
														// Case: Specified a unique match => Selects that one (with notice)
														case Some(only) =>
															println(s"Selected ${ only._2 }")
															Some(only._1)
														// Case: Still multiple options => Lists them again
														case None =>
															_selectFrom(options, addNew, target, verb, maxListCount)
													}
											}
								}
						}
					}
			}
		}
		
		private def thisLastOrNext[A](input: String, current: => A)(next: A => A)(prev: A => A) = {
			input.take(4).toLowerCase match {
				case "this" => Some(current)
				case "last" | "prev" => Some(prev(current))
				case "next" => Some(next(current))
				case _ => None
			}
		}
	}
}

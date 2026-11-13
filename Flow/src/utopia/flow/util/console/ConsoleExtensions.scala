package utopia.flow.util.console

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.FileUtils
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WeekDay.{Monday, Sunday}
import utopia.flow.time.{DateRange, Today, WeekDay, YearMonth}
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.mutable.Pointer

import java.nio.file.Path
import java.time.LocalDate
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
	 * Allows use of relative and/or impartial dates, such as "today", "last Tuesday", or "3.6".
	 * Expects dates, months and years to be separated with ".", and to be in ascending order.
	 * @param str String to convert into a date
	 * @param defaultMonth Month and year to supply when missing from the input.
	 *                     Default = current month & year.
	 * @return Parsed date. None if the input couldn't be parsed.
	 */
	def parseDate(str: String, defaultMonth: YearMonth = Today.yearMonth) = str.toLowerCase match {
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
				val parts = str.splitIterator(Regex.dot)
					.flatMap { _.trim.filter { _.isDigit }.dropWhile { _ == '0' }.toIntOption }
					.toOptimizedSeq
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
	
	/**
	 * Resolves a path together with the user
	 * @param arg Received file (command) argument
	 * @param root The top searched directory. Default = current working directory.
	 * @param prompt Prompt to display before asking the user to select a file or a directory
	 * @param listDirectoriesCount Number of directories to search and display for selection at once. Default = 5.
	 * @param targetDirectories Whether searching for directories.
	 *                          False if regular files are searched (default)
	 * @param fileFilter Filter that only accepts valid files.
	 *
	 *                   Note: If 'targetDirectories' is set to true and 'filterBasedOnContent' is left as false,
	 *                   this filter is applied to directories instead of regular files.
	 *
	 * @param filterBasedOnContent Whether 'fileFilter' should be applied to directory contents.
	 *                             Ignored if 'targetDirectories' is false (default).
	 *                             Default = false = 'fileFilter' will be applied to the targeted directories,
	 *                             if applicable.
	 * @param log Implicit logger interface
	 * @return Selected (existing) regular file path. None if not found or canceled by the user.
	 */
	def resolvePath(arg: Value = Value.empty, root: Path = FileUtils.workingDirectory,
	                prompt: String = "Please specify the targeted file:", listDirectoriesCount: Int = 5,
	                targetDirectories: Boolean = false, filterBasedOnContent: Boolean = false)
	               (fileFilter: Path => Boolean)(implicit log: Logger) =
	{
		// Finds all directories that may be targeted, including targetable files (if applicable)
		val allDirectoriesIter = {
			val base = root.toTree.topDownNodesIterator.filter { _.value.isDirectory }
			// Case: Targeting directories => May apply 'fileFilter' to directories
			if (targetDirectories) {
				if (filterBasedOnContent)
					base.filter { dir => dir.children.exists { p => p.value.isRegularFile && fileFilter(p.value) } }
						.map { _.value -> Empty }
				else
					base.map { _.value }.filter(fileFilter).map { _ -> Empty }
			}
			// Case: Targeting files
			//       => Applies 'fileFilter' to files and includes only directories with 1+ targetable files
			else
				base
					.map { p =>
						p.value -> p.children.iterator.map { _.value }.filter { p => p.isRegularFile && fileFilter(p) }
							.toOptimizedSeq
					}
					.filter { _._2.nonEmpty }
		}
		arg.string match {
			// Case: Custom target => Checks whether it's an existing file or directory
			case Some(target) =>
				Some(target: Path).filter { _.exists } match {
					// Case: Exists => Checks whether it's a regular file
					case Some(targetPath) =>
						// Case: Directory (when not targeted) => Asks the user to select a file from it
						if (targetPath.isDirectory && !targetDirectories)
							selectFromDirectoryFiles(targetPath,
								targetPath
									.iterateChildren {
										_.filter { p => p.isRegularFile && fileFilter(p) }.toOptimizedSeq
									}
									.logWithMessage("File system reading failed").getOrElse(Empty))
						// Case: Regular file or targeted directory => Selects it
						else
							Some(targetPath)
						
						// Case: (Likely) not a file reference
					case None =>
						// Case: Looks like an absolute path => Treats it as a non-existing file and fails
						if (target.headOption.contains('/')) {
							println(s"`$target` doesn't exist")
							None
						}
						// Case: Other input string => Looks whether a file or a directory with that name can be found
						else {
							val directoriesIter = allDirectoriesIter.flatMap { case (dir, files) =>
								val hasMatchingName = dir.fileName.containsIgnoreCase(target)
								// Case: Targets directories => Directory name must match the input
								if (targetDirectories) {
									if (hasMatchingName)
										Some(dir -> files)
									else
										None
								}
								// Case: Directory name contains specified input & targeting regular files
								//       => Accepts all files from that directory
								else if (hasMatchingName)
									Some(dir -> files)
								// Case: Directory name is not a match & targeting regular files
								//       => Only targets matching files
								else {
									val matchingFiles = files.filter { _.fileName.containsIgnoreCase(target) }
									// Case: No targetable files => Skips this directory
									if (matchingFiles.isEmpty)
										None
									else
										Some(dir -> matchingFiles)
								}
							}
							// Allows the user to select from the results (or to specify another file)
							selectFromDirectories(directoriesIter, prompt, listDirectoriesCount, targetDirectories)
						}
				}
				// Case: No custom target => Scans the local directories for possible options
			case None =>
				// Allows the user to select from the results (or to specify another file)
				selectFromDirectories(allDirectoriesIter, prompt, listDirectoriesCount, targetDirectories)
		}
	}
	private def selectFromDirectories(directoriesIter: Iterator[(Path, Seq[Path])], prompt: String, displayCount: Int,
	                                  targetDirectories: Boolean)
	                                 (implicit log: Logger): Option[Path] =
	{
		// Finds the next X directories to display
		val nextOptions = directoriesIter.collectNext(displayCount)
		// Case: No results => Asks the user to specify a path
		if (nextOptions.isEmpty)
			askForPath(prompt)
		// Case: Directories found => Asks the user to select one of them
		else {
			println(prompt)
			val baseOptions = nextOptions.map { case (dir, files) =>
				val target = {
					if (targetDirectories)
						dir -> files
					else
						files.only match {
							case Some(file) => file -> Empty
							case None => dir -> files
						}
				}
				Some(target) -> target._1.toAbsolutePath.toJson
			}
			// May also include an option "Look further" for another search iteration
			val options = if (directoriesIter.hasNext) baseOptions :+ (None -> "Look further") else baseOptions
			StdIn
				.selectFromOrAdd(options, "directories") {
					Some(askForPath(s"Please specify the targeted ${ if (targetDirectories) "directory" else "file" }")
						.map { selected =>
							if (targetDirectories || selected.isRegularFile)
								selected -> Empty
							else
								selected -> selected.iterateChildren { _.filter { _.isRegularFile }.toOptimizedSeq }
									.logWithMessage("File system reading failed").getOrElse(Empty)
						})
				}
				.flatMap {
					// Case: Selected a directory or a file
					case Some((selectedPath, files)) =>
						// Case: Regular file found => Search completes
						if (targetDirectories || selectedPath.isRegularFile)
							Some(selectedPath)
						// Case: Selected a directory => Accepts it or asks the user to select a file from it
						else
							selectFromDirectoryFiles(selectedPath, files)
						
						// Case: Selected "Look further" => Uses recursion
					case None => selectFromDirectories(directoriesIter, prompt, displayCount, targetDirectories)
				}
		}
	}
	private def selectFromDirectoryFiles(directory: Path, files: Seq[Path]) =
		files.emptyOneOrMany match {
			// Case: No files found => Fails
			case None =>
				println(s"No files were found under ${ directory.toAbsolutePath }")
				None
				// Case: Only one file found => Selects it
			case Some(Left(only)) => Some(only)
				// Case: Multiple files found => Asks the user to select one
			case Some(Right(options)) =>
				println("Select from the following options:")
				StdIn.selectFrom(options.map { p => p -> p.fileName }, "files")
		}
	private def askForPath(prompt: String) = StdIn.readNonEmptyLine(prompt) match {
		case Some(input) =>
			val path = input: Path
			if (path.exists)
				Some(path)
			else {
				println(s"`$input` doesn't exist")
				None
			}
		case None =>
			println("Canceled by user")
			None
	}
	
	
	// EXTENSIONS   -------------------------
	
	implicit class RichStdIn(val in: StdIn.type) extends AnyVal
	{
		/**
		 * Reads a line of text from this input. May display a prompt before requesting input.
		 * @param prompt Prompt to display before requesting for input. If empty, no prompt is displayed (default).
		 *               If the prompt ends with ":", requests input on the same line.
		 * @return Read line as a string
		 */
		def printAndReadLine(prompt: String) = {
			if (prompt.isEmpty)
				in.readLine()
			else if (prompt.endsWith(":"))
				in.readLine(s"$prompt ")
			else {
				println(prompt)
				in.readLine()
			}
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
		def read(prompt: String = ""): Value = readNonEmptyLine(prompt) match {
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
			val suffix = if (question.contains('\n') || question.length > 80) "" else ":"
			readNonEmptyLine(s"$question $yesNo$suffix") match {
				case Some(answer) =>
					answer.head.toLower match {
						case 'y' | 't' => true
						case 'n' | 'f' => false
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
			readLineIterator.takeWhile { _.nonEmpty }.toOptimizedSeq
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
		 * @param maxListCount Maximum number of items that may be listed on the console (default = 25).
		 * @tparam A Type of items selected from
		 * @return The item selected by the user.
		 *         None if no options were available, or if the user chose not to select an item.
		 */
		def selectFrom[A](options: Seq[(A, String)], target: String = "items", verb: String = "select",
		                  maxListCount: Int = 25) =
			_selectFrom(options, None, target, verb, maxListCount).headOption
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
			_selectFrom(options, Some({ () => addNew.emptyOrSingle }), target, verb, maxListCount).headOption
		
		/**
		  * Allows the user to select 0-n items from the specified options, using an interactive console
		  * @param options Options available for selection
		  * @param target A string representing the options in plural form. Default = "items".
		  * @param verb Verb used for the action of selecting an option. Default = "select".
		  * @param maxListCount Maximum number of items that may be listed on the console.
		  * @tparam A Type of selected items
		  * @return Selected items
		  */
		def selectMany[A](options: Seq[(A, String)], target: String = "items", verb: String = "select",
		                  maxListCount: Int = 20) =
			_selectFrom(options, None, target, verb, maxListCount, multiSelect = true)
		
		// Allows the user to select from the specified options.
		// Option description is the second item in 'options'
		// If 'addNew' is defined, the user is allowed to add new items
		// Please specify 'target' in plural form
		private def _selectFrom[A](options: Seq[(A, String)], addNew: Option[() => Iterable[A]],
		                           target: String, verb: String, maxListCount: Int, multiSelect: Boolean = false): Iterable[A] =
			options.emptyOneOrMany match {
				// Case: No options available
				case None =>
					addNew match {
						// Case: Insert possible => Asks the user if they want to insert a new item
						case Some(addNew) =>
							if (ask(s"No $target were found. \nAdd a new one?"))
								addNew()
							else
								Empty
						// Case: Insert not possible => Couldn't find any item
						case None =>
							println(s"No $target were found.")
							Empty
					}
				// Case: Only one option available => Asks the user whether they want to select it
				case Some(Left(only)) =>
					if (addNew.isDefined)
						_selectFromMany(Single(only), addNew, target, verb, maxListCount, multiSelect = false)
					else if (ask(s"$verb ${ only._2 }?", default = true))
						Single(only._1)
					else
						Empty
				
				// Case: Multiple options to choose from => Asks the user to filter or to select one
				case Some(Right(options)) => _selectFromMany(options, addNew, target, verb, maxListCount, multiSelect)
			}
		private def _selectFromMany[A](options: Seq[(A, String)], addNew: Option[() => Iterable[A]],
		                               targetsStr: String, verb: String, maxListCount: Int, multiSelect: Boolean): Iterable[A] =
		{
			val tooManyOptions = options.hasSize > maxListCount
			
			// Displays the prompt
			println(s"${ verb.capitalize } from the list below:")
			options.iterator.take(maxListCount).zipWithIndex.foreach { case ((_, desc), i) =>
				println(s"\t${i + 1}: $desc")
			}
			if (tooManyOptions)
				println(s"s\t${ maxListCount + 1 }: Show more $targetsStr (${ options.size - maxListCount } more)")
			if (addNew.isDefined)
				println("\t0: Create new")
			if (multiSelect)
				println(s"${ verb.capitalize } multiple $targetsStr by specifying a comma-separated list. To $verb all $targetsStr, type \"all\".")
			
			// Asks the user to select an option
			// May repeat, depending on the selection
			val moreIndexP = Pointer(if (tooManyOptions) Some(maxListCount + 1) else None)
			val optionsP = Pointer(options)
			val resultsIter = Iterator
				.continually {
					readNonEmptyLine("Selected index or item:") match {
						case None => Empty -> false
						case Some(target) =>
							resolveTarget(target, optionsP, moreIndexP, addNew, targetsStr, maxListCount, multiSelect)
					}
				}
				// Repeats as long as needed, then returns the selected options
				.takeTo { _._2 }.flatMap { _._1 }
			
			if (multiSelect)
				resultsIter.toOptimizedSeq
			else
				resultsIter.nextOption()
		}
		// Assumes that target is not empty
		private def resolveTarget[A](target: String, optionsP: Pointer[Seq[(A, String)]],
		                             showMoreIndexP: Pointer[Option[Int]], addNew: Option[() => Iterable[A]],
		                             targetsStr: String, maxListCount: Int, multiSelect: Boolean) = {
			// Checks whether "new" was selected
			addNew.filter { _ => target == "0" || (target ~== "new") } match {
				// Case: Selected "new" => Adds a new item
				case Some(addNew) => addNew() -> true
				// Case: Targeted specific item(s), added filter(s) or requested to display more options
				case None =>
					val currentOptions = optionsP.value
					// Case: Targeted all items
					if (multiSelect && (target ~== "all")) {
						println(s"Selected all ${ currentOptions.size } $target")
						currentOptions.map { _._1 } -> true
					}
					else {
						// Splits the input into name-based and index-based targets
						val targets = {
							if (multiSelect)
								target.splitIterator(Regex.comma).map { _.trim }.toOptimizedSeq
							else
								Single(target)
						}
						val (stringTargets, targetIndices) = targets.divideWith { t => t.int.toRight(t) }
						// Limits the integer-based targets to the displayed range
						val (shouldShowMore, appliedTargetIndices) = {
							showMoreIndexP.value match {
								case Some(moreIndex) =>
									targetIndices.contains(moreIndex) -> targetIndices.filter { _ < moreIndex }
								case None => false -> targetIndices
							}
						}
						
						// Matches based on targeted indices
						val (unmatchedIndices, indexTargeted) = appliedTargetIndices.divideWith { index =>
							// Case: Selected 0 => Adds a new item
							if (index == 0 && addNew.isDefined)
								addNew match {
									case Some(addNew) => Right(addNew())
									case None => Right(Empty)
								}
							// Case: Selected another index => Picks that item, if the index is valid
							else
								currentOptions.lift(index - 1) match {
									case Some((item, _)) => Right(Single(item))
									case None => Left(index)
								}
						}
						// Matches based on targeted names / filters
						val (unmatchedTargets, nameMatches) = stringTargets.divideWith { target =>
							currentOptions.filter { _._2.containsIgnoreCase(target) }.emptyOneOrMany match {
								// Case: No matches => Adds the target to the unmatched list
								case None => Left(target)
								// Case: Targeted a specific item => Selects that item
								case Some(Left((only, name))) =>
									if (name !~== target)
										println(s"Selected $name")
									Right(Right(only))
								
								// Case: The target is ambiguous => Adds the targets to the next iteration's options
								case Some(Right(many)) => Right(Left(many))
							}
						}
						val (filtered, namedTargets) = nameMatches.divided
						val selected = OptimizedIndexedSeq.concat(indexTargeted.view.flatten, namedTargets)
						
						val readyToFinish = {
							// Case: Some targets were not matched => Asks the user to select again
							if (unmatchedTargets.nonEmpty || unmatchedIndices.nonEmpty) {
								println(s"Could not resolve: ${
									(unmatchedTargets.iterator ++ unmatchedIndices.iterator.map { _.toString })
										.map { _.quoted }.mkString(", ") }")
								
								false
							}
							// Case: Selected "show more" => Displays more options and allows another selection
							else if (shouldShowMore) {
								showMoreIndexP.update {
									case Some(nextIndex) =>
										currentOptions.view.slice(nextIndex, nextIndex + maxListCount).iterator.zipWithIndex
											.foreach { case ((_, desc), index) =>
												println(s"\t${nextIndex + index}: $desc")
											}
										Some(nextIndex + maxListCount).filter { _ <= currentOptions.size }
									
									case None => None
								}
								showMoreIndexP.value.foreach { i =>
									println(s"s\t$i: Show more $targetsStr (${ currentOptions.size - i + 1 } more)")
								}
								
								false
							}
							// Case: Applied filters
							//       => Updates the options with the filtered results and moves to the next iteration
							else if (filtered.nonEmpty) {
								val newOptions = filtered.flatten
								optionsP.value = newOptions
								println(s"\nFiltered $targetsStr:")
								newOptions.iterator.zipWithIndex.foreach { case ((_, desc), i) =>
									println(s"\t${ i + 1 }: $desc")
								}
								
								false
							}
							else
								true
						}
						
						// If another iteration is needed and items were selected, lists the selected items
						if (!readyToFinish)
							selected.emptyOneOrMany.foreach {
								case Left(only) => println(s"Currently selected: $only")
								case Right(many) =>
									println("Currently selected:")
									many.foreach(println)
							}
						
						selected -> readyToFinish
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

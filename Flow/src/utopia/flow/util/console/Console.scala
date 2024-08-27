package utopia.flow.util.console

import utopia.flow.async.process.Breakable
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.caching.ResettableLazy

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.StdIn
import scala.util.Try

object Console
{
	// OTHER    ------------------------------
	
	/**
	 * Creates a new console
	 * @param commandsPointer A pointer to the available commands
	 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name, default = empty)
	 * @param terminatorPointer A pointer that contains true when this console should be closed (default = always false)
	 * @param closeCommandName Name of the command that closes this console (default = empty = no close command is used)
	 * @param jsonParser Implicit json parser for command argument handling
	 * @return A new console
	 */
	def apply(commandsPointer: View[Iterable[Command]], prompt: => String = "",
	          terminatorPointer: View[Boolean] = Fixed(false), closeCommandName: String = "")
	         (implicit jsonParser: JsonParser) =
		new Console(commandsPointer, prompt, terminatorPointer, closeCommandName)
	
	/**
	 * Creates a new console that doesn't change its commands (or state unless stopped or directed by the user)
	 * @param commands Commands served by this console
	 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name, default = empty)
	 * @param closeCommandName Name of the command that closes this console
	 *                         (default = empty = no close command is used).
	 *                         Please note that if empty, the only way to close this console
	 *                         is through the stop() method
	 * @param jsonParser jsonParser Implicit json parser for command argument handling
	 * @return A new console
	 */
	def static(commands: Iterable[Command], prompt: => String = "", closeCommandName: String = "")
	          (implicit jsonParser: JsonParser) =
		apply(View(commands), prompt, closeCommandName = closeCommandName)
	/**
	 * Creates a new console with fixed commands that terminates when a condition is met
	 * @param commands Commands served by this console
	 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name, default = empty)
	 * @param closeCommandName Name of the command that closes this console
	 *                         (default = empty = no close command is used)
	 * @param testTermination A function called between commands to see whether this console should close.
	 *                        Returns true when its time for this console to close.
	 * @param jsonParser jsonParser jsonParser Implicit json parser for command argument handling
	 * @return A new console
	 */
	def terminating(commands: Iterable[Command], prompt: => String = "", closeCommandName: String = "")
	               (testTermination: => Boolean)
	               (implicit jsonParser: JsonParser) =
		apply(View(commands), prompt, View(testTermination), closeCommandName)
}

/**
 * Provides an interactive (command line) console for the user, with which they can fire specific commands
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 * @param commandsPointer A pointer to the available commands
 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name, default = empty)
 * @param terminatorPointer A pointer that contains true when this console should be closed (default = always false)
 * @param closeCommandName Name of the command that closes this console (default = empty = no close command is used)
 * @param jsonParser Implicit json parser for command argument handling
 */
class Console(commandsPointer: View[Iterable[Command]], prompt: => String = "",
              terminatorPointer: View[Boolean] = View(false), closeCommandName: String = "")
             (implicit jsonParser: JsonParser)
	extends Runnable with Breakable
{
	// ATTRIBUTES   ---------------------------------
	
	private implicit val log: Logger = SysErrLogger
	
	private val currentRunsPointer = Volatile.eventful(0)
	private val stopFlag = Volatile.switch
	private val stopPromiseCache = ResettableLazy { Promise[Unit]() }
	
	private lazy val helpCommand = Command("help", "man",
		"Lists available commands or describes a command if one is specified")(
		ArgumentSchema("command", "cmd", help = "Name of the described command")) { args =>
		args("command").string match
		{
			// Case: Requests information about a specific command
			case Some(commandName) =>
				val availableCommands = commandsPointer.value
				// Finds the targeted command
				availableCommands.find { _.matchesName(commandName) } match
				{
					// Case: Targeted command found => Describes it and its arguments
					case Some(command) =>
						command.helpOption.foreach(println)
						if (command.takesArguments)
						{
							println("Arguments:")
							command.argumentsSchema.arguments.foreach { arg => println(s"- $arg") }
						}
						else
							println(s"${command.nameAndAlias} doesn't take any arguments")
					// Case: Command not found => Informs the user
					case None =>
						if (commandName ~== closeCommandName)
							println("Closes this console. Takes no arguments.")
						else
						{
							println(s"'$commandName' doesn't match any command")
							findClosestMatch(commandName, availableCommands)
								.foreach { command => println(s"Did you mean ${command.nameAndAlias}?") }
						}
				}
			// Case: Requests a list of commands => Prints the list
			case None =>
				println("Available commands:")
				commandsPointer.value.foreach { command => println(s"- $command") }
				closeCommandName.notEmpty.foreach { close => println(s"- $close: Closes this console") }
				println("For more information about a specific command, add that command's name or alias as the first argument of this command")
		}
	}
	
	
	// COMPUTED -------------------------------------
	
	/**
	 * @return Whether this console is currently running in at least one thread
	 */
	def isRunning = currentRunsPointer.value > 0
	
	/**
	 * @param exc Implicit execution context
	 * @return A future that completes as soon as this console is running - completed future if already running
	 */
	def runningFuture(implicit exc: ExecutionContext) = currentRunsPointer.futureWhere { _ > 0 }
	/**
	 * @param exc Implicit execution context
	 * @return A future that completes as soon as this console is no longer running - completed future if not running
	 */
	def notRunningFuture(implicit exc: ExecutionContext) = currentRunsPointer.futureWhere { _ <= 0 }
	
	
	// IMPLEMENTED  ---------------------------------
	
	override def stop() =
	{
		// Case: Currently running => requests the run to stop
		if (isRunning)
		{
			stopFlag.set()
			stopPromiseCache.value.future
		}
		// Case: Not running => succeeds immediately
		else
			Future.successful(())
	}
	
	override def run() = {
		// Records that the run started
		currentRunsPointer.update { _ + 1 }
		
		// Catches all thrown exceptions in order to make sure console state is updated correctly
		val result = Try {
			// Repeats the process until stopped, closed or terminated
			var closed = false
			val closeCommand = closeCommandName.notEmpty.map { commandName =>
				Command.withoutArguments(commandName, help = "Closes this console") { closed = true }
			}
			
			while (!stopFlag.getAndReset() && !closed && !terminatorPointer.value) {
				val baseCommands = commandsPointer.value.toVector
				// If there are no commands available, automatically closes
				if (baseCommands.isEmpty)
					closed = true
				else {
					// Asks the user for input
					prompt.notEmpty.foreach(println)
					val input = StdIn.readLine()
					
					if (input.nonEmpty) {
						val availableCommands = (baseCommands :+ helpCommand) ++ closeCommand
						// Splits the input into command name and argument list parts
						val (commandPart, argsPart) = input.splitAtFirst(" ").toTuple
						// Finds the targeted command
						availableCommands.find { _.matchesName(commandPart) } match {
							// Case: Targeted command found => executes that command using the specified arguments
							case Some(command) =>
								// Catches thrown exceptions
								Try { command.parseAndExecute(argsPart) }.failure.foreach { error =>
									error.printStackTrace()
									println(s"Command ${command.name} execution failed with exception (${
										error.getMessage}). See the stack trace above for more details.")
								}
							// Case: No command found => informs the user
							case None =>
								println(s"'$commandPart' doesn't match any available command")
								findClosestMatch(commandPart, availableCommands)
									.foreach { command => println(s"Did you mean ${command.nameAndAlias}?") }
								println("Use the 'help' command to see a list of available commands")
						}
					}
				}
			}
		}
		
		// Records that run ended and informs possible stop listeners
		currentRunsPointer.update { _ - 1 }
		stopPromiseCache.popCurrent().foreach { _.success(()) }
		
		// If the process failed, propagates that failure further
		result.failure.foreach { throw _ }
	}
	
	
	// OTHER    ----------------------------
	
	private def findClosestMatch(input: String, options: Iterable[Command]) = {
		options.bestMatch { c => c.name.isSimilarTo(input, 2) || c.alias.notEmpty.exists { _.isSimilarTo(input, 1) } }
			.oneOrMany match
		{
			case Left(only) => Some(only)
			case Right(options) =>
				// Prefers items with shorter names
				val orderedOptions = options.toSeq.sortBy { _.name.length }
				val optionsWithAlias = orderedOptions.filter { _.hasAlias }.sortBy { _.alias.length }
				val lowerIn = input.toLowerCase
				
				// Tests for a case where alias contains input
				optionsWithAlias.find { _.alias.toLowerCase.contains(lowerIn) }
					// Tests for a case where name contains input
					.orElse { orderedOptions.find { _.name.toLowerCase.contains(lowerIn) } }
					.orElse {
						val inputChars = lowerIn.toSet
						// Tests which alias contains most input characters
						optionsWithAlias.map { command =>
								val chars = command.alias.toLowerCase.toSet
								command -> (chars & inputChars).size
							}.maxByOption { _._2 }.filter { _._2 > 0 }.map { _._1 }
							.orElse {
								// Tests which name contains most input characters
								orderedOptions.map { command =>
									val chars = command.name.toLowerCase.toSet
									command -> (chars & inputChars).size
								}.maxByOption { _._2 }.filter { _._2 > 0 }.map { _._1 }
							}
					}
		}
	}
}

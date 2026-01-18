package utopia.flow.util.console

import utopia.flow.async.process.Breakable
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single, Tree}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.StringExtensions._
import utopia.flow.util.StringUtils
import utopia.flow.util.console.Console.Commands
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.StdIn
import scala.util.Try

object Console
{
	// TYPES    ------------------------------
	
	private type Commands = Tree[(Seq[String], Iterable[Command])]
	
	
	// OTHER    ------------------------------
	
	/**
	 * Creates a new console with fixed commands that terminates when a condition is met
	 * @param commands Commands served by this console
	 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name, default = empty)
	 * @param closeCommandName Name of the command that closes this console
	 *                         (default = empty = no close command is used)
	 * @param testTermination A function called between commands to see whether this console should close.
	 *                        Returns true when it's time for this console to close.
	 * @param jsonParser jsonParser jsonParser Implicit json parser for command argument handling
	 * @return A new console
	 */
	@deprecated("Deprecated for removal", "v2.8")
	def terminating(commands: Iterable[Command], prompt: => String = "", closeCommandName: String = "")
	               (testTermination: => Boolean)
	               (implicit jsonParser: JsonParser) =
		apply(Fixed(commands), prompt, View(testTermination), closeCommandName)
	
	/**
	 * Creates a new console
	 * @param commands Commands served by this console
	 * @param prompt Prompt displayed before requesting user to type the next command
	 * @param closeCommandName Name of the command that closes this console
	 * @param listAvailableCommands Whether available commands should be listed
	 *                              when asking the user to specify the next command.
	 * @param jsonParser jsonParser Implicit JSON parser for command argument handling
	 * @return A new console
	 */
	def static(commands: Iterable[Command], prompt: String, closeCommandName: String,
	           listAvailableCommands: Boolean = false)
	          (implicit jsonParser: JsonParser) =
		staticNamespaced(Map("" -> commands), prompt, closeCommandName, listAvailableCommands)
	/**
	 * Creates a new console
	 * @param commandsPointer A pointer to the available commands
	 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name)
	 * @param terminatorPointer A pointer that contains true when this console should be closed (default = always false)
	 * @param closeCommandName Name of the command that closes this console (default = empty = no close command is used)
	 * @param listAvailableCommands Whether available commands should be listed
	 *                              when asking the user to specify the next command.
	 * @param jsonParser Implicit JSON parser for command argument handling
	 * @return A new console
	 */
	def apply(commandsPointer: Changing[Iterable[Command]], prompt: => String,
	          terminatorPointer: View[Boolean] = Fixed(false), closeCommandName: String = "",
	          listAvailableCommands: Boolean = false)
	         (implicit jsonParser: JsonParser) =
		namespaced(commandsPointer.map { commands => Map("" -> commands) }, prompt, terminatorPointer,
			closeCommandName, listAvailableCommands)
	
	/**
	 * Creates a new console
	 * @param commands List of available commands
	 * @param prompt Prompt displayed before requesting user to type the next command
	 * @param closeCommandName Name of the command that closes this console
	 * @param listAvailableCommands Whether available commands should be listed
	 *                              when asking the user to specify the next command.
	 * @param jsonParser Implicit JSON parser for command argument handling
	 * @return A new console
	 */
	def staticNamespaced(commands: Map[String, Iterable[Command]], prompt: String,
	                     closeCommandName: String, listAvailableCommands: Boolean = false)
	                    (implicit jsonParser: JsonParser) =
		namespaced(Fixed(commands), prompt, AlwaysFalse, closeCommandName, listAvailableCommands)
	/**
	 * Creates a new console
	 * @param commandsPointer A pointer to the available commands
	 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name)
	 * @param terminatorPointer A pointer that contains true when this console should be closed (default = always false)
	 * @param closeCommandName Name of the command that closes this console (default = empty = no close command is used)
	 * @param listAvailableCommands Whether available commands should be listed
	 *                              when asking the user to specify the next command.
	 * @param jsonParser Implicit JSON parser for command argument handling
	 * @return A new console
	 */
	def namespaced(commandsPointer: Changing[Map[String, Iterable[Command]]], prompt: => String,
	               terminatorPointer: View[Boolean] = AlwaysFalse, closeCommandName: String = "",
	               listAvailableCommands: Boolean = false)
	              (implicit jsonParser: JsonParser) =
		new Console(commandsPointer, prompt, terminatorPointer, closeCommandName, listAvailableCommands)
}

/**
 * Provides an interactive (command line) console for the user, with which they can fire specific commands
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 * @param commandsPointer A pointer to the available commands, grouped by namespace.
 * @param prompt Prompt displayed before requesting user to type the next command (call-by-name)
 * @param terminatorPointer A pointer that contains true when this console should be closed (default = always false)
 * @param closeCommandName Name of the command that closes this console (default = empty = no close command is used)
 * @param listAvailableCommands Whether available commands should be listed
 *                              when asking the user to specify the next command.
 * @param jsonParser Implicit JSON parser for command argument handling
 */
class Console(commandsPointer: Changing[Map[String, Iterable[Command]]], prompt: => String,
              terminatorPointer: View[Boolean] = View(false), closeCommandName: String = "",
              listAvailableCommands: Boolean = false)
             (implicit jsonParser: JsonParser)
	extends Runnable with Breakable
{
	// ATTRIBUTES   ---------------------------------
	
	private implicit val log: Logger = SysErrLogger
	
	private val currentRunsPointer = Volatile.eventful(0)
	private val stopFlag = Volatile.switch
	private val stopPromiseCache = ResettableLazy { Promise[Unit]() }
	
	private var lastNamespace: Seq[String] = Empty
	
	private val helpCommand: Command = {
		val closeCommandRepresentation = closeCommandName.ifNotEmpty
			.map { Command.withoutArguments(_, help = "Closes this console") { () } }
			.emptyOrSingle
		val commandsP = commandsPointer.lazyMap { commandTreeFrom(_, closeCommandRepresentation) }
		
		Command("help", "man",
			"Lists available commands or describes a command if one is specified")(
			ArgumentSchema("command", "cmd", help = "Name of the described command")) { args =>
			args("command").string match {
				// Case: Requests information about a specific command
				case Some(commandName) =>
					findCommand(commandsP.value, commandName).foreach { command =>
						if (command.argumentsSchema.nonEmpty) {
							println(StringUtils.asciiTableFrom[ArgumentSchema](command.argumentsSchema.arguments,
								Vector(
									"Name" -> { _.name },
									"Alias" -> { _.alias },
									"Default" -> { arg =>
										arg.defaultDescription.nonEmptyOrElse(arg.defaultValue.getString)
									},
									"Description" -> { _.help.splitToLinesIterator(40).mkString("\n") }
								),
								(command.nameAndAlias +: command.help.ifNotEmpty.emptyOrSingle).mkString("\n")
							))
						}
						else {
							println(command.nameAndAlias)
							command.help.ifNotEmpty.foreach(println)
							println(s"${command.name} doesn't take any arguments")
						}
					}
				// Case: Requests a list of commands => Prints the list
				case None =>
					val groupedCommands = commandsP.value.allNavsIterator.groupMapReduce { _._1 } { _._2 } { _ ++ _ }
					groupedCommands.oneOrMany match {
						case Left((_, commands)) => listCommands(commands, "Available commands")
						case Right(namespaces) =>
							namespaces.iterator.map { case (ns, commands) => ns.mkString(":") -> commands }
								.toOptimizedSeq.sortBy { _._1 }
								.foreach { case (namespace, commands) => listCommands(commands, namespace) }
					}
					println("\nFor more information about a specific command, specify that command's name or alias as the first argument of this command")
			}
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
	
	override def stop() = {
		// Case: Currently running => requests the run to stop
		if (isRunning) {
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
			val defaultCommands = helpCommand +: closeCommand.emptyOrSingle
			val allCommandsP = commandsPointer.lazyMap { commandTreeFrom(_, defaultCommands) }
			
			while (!stopFlag.getAndReset() && !closed && !terminatorPointer.value) {
				// Asks the user for input
				StdIn.readNonEmptyLine(appliedPrompt(allCommandsP.value)).foreach { input =>
					val (commandPart, argsPart) = input.splitAtFirst(" ").toTuple
					findCommand(allCommandsP.value, commandPart).foreach { command =>
						// Executes the command, catching thrown exceptions
						Try { command.parseAndExecute(argsPart) }.failure.foreach { error =>
							error.printStackTrace()
							println(s"Command ${command.name} execution failed with exception (${
								error.getMessage}). See the stack trace above for more details.")
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
	
	/**
	 * Converts a list of commands into a namespaced tree format
	 * @param namespaced A map of namespaced commands,
	 *                   where keys are namespaces (prefixes) and values are commands under that namespace
	 * @param other Other commands that are not assigned an external namespace
	 * @return A tree that contains the specified commands, grouped by namespace
	 */
	private def commandTreeFrom(namespaced: Map[String, Iterable[Command]], other: Iterable[Command]): Commands = {
		// Converts the specified command-sets into a list of namespace-command pairs
		val commandsWithNamespaces = OptimizedIndexedSeq.concat(
			namespaced.view.flatMap { case (ns, commands) =>
				val nsElements = {
					if (ns.isEmpty)
						Empty
					else
						ns.split(Command.namespaceSplitRegex)
				}
				// Prepends the external namespace to the command's internal namespace
				commands.map { c => (nsElements ++ c.namespace) -> c }
			},
			other.view.map { c => c.namespace -> c })
		
		// Commands without namespace are placed at the root node
		val (nonNamespacedCommands, namespacedCommands) = commandsWithNamespaces.divideBy { _._1.nonEmpty }.toTuple
		// Groups the namespaced commands into trees
		val namespaceNodes = Tree.groupingBranches(namespacedCommands.map { case (namespace, command) =>
			val emptyNodes: Seq[(Seq[String], Option[Command])] =
				(1 until namespace.size).map { len => namespace.take(len) -> None }
			emptyNodes :+ (namespace -> Some(command))
		}) { _._1 } { (ns, commands) => ns -> (commands.flatMap { _._2 }.toOptimizedSeq: Iterable[Command]) }
		
		// Combines the generated trees under a root node
		Tree[(Seq[String], Iterable[Command])](Empty -> nonNamespacedCommands.map { _._2 }, namespaceNodes)
	}
	
	/**
	 * Lists the available commands, if appropriate
	 * @param commands Currently available commands
	 * @return A prompt to show to the user
	 */
	private def appliedPrompt(commands: => Commands) = {
		if (listAvailableCommands) {
			val _commands = commands
			val commandsList = {
				// Case: Namespaces are used => May limit the displayed node-set to the last used namespace
				if (_commands.hasChildren) {
					val root = {
						// Case: Last command had no namespace, or there are not that many commands
						//       => Shows all commands
						if (lastNamespace.isEmpty ||
							_commands.allNavsIterator.map { _._2.size }.foldLeftIterator(0) { _ + _ }.forall { _ <= 8 })
							_commands
						else
							targetNamespace(_commands, lastNamespace).getOrElse(_commands)
					}
					// Checks for duplicate command names and displays those with the namespace included
					val contentsStr = root.allNavsIterator
						.flatMap { case (ns, commands) => commands.iterator.map { c => (ns, c.name) } }
						.groupToSeqsBy { _._2 }.iterator
						.flatMap { case (commandName, versions) =>
							if (versions.hasSize > 1)
								versions.iterator.map { case (ns, name) =>
									s"${ ns.view.drop(lastNamespace.size).mkString(":").appendIfNotEmpty(":") }$name"
								}
							// Unique command names are displayed without the namespace
							else
								Single(commandName)
						}
						.toOptimizedSeq.sorted.mkString(", ")
					
					if ((root eq _commands) || contentsStr.isEmpty)
						contentsStr
					// Case: Some commands were not listed, adds "..."
					else
						s"$contentsStr, ..."
				}
				// Case: There's only one namespace used
				//       => Lists all commands in either their alias or name, depending on how many there are to list
				else {
					val commandToString = {
						if (_commands.allNavsIterator.map { _._2.size }.sum > 8)
							{ c: Command => c.aliasOrName }
						else
							{ c: Command => c.name }
					}
					_commands.allNavsIterator.flatMap { _._2 }.map(commandToString).toOptimizedSeq.sorted.mkString(", ")
				}
			}
			s"$prompt\n[$commandsList]"
		}
		else
			prompt
	}
	
	private def findCommand(commands: Commands, input: String) = {
		// Determines the targeted namespace & command name
		val (namespaceInput, commandName) = if (input.contains(':')) input.splitAtLast(":").toTuple else "" -> input
		val targetedNamespace = {
			if (namespaceInput.isEmpty)
				lastNamespace
			else
				namespaceInput.splitIterator(Command.namespaceSplitRegex).map { _.toLowerCase }.toOptimizedSeq
		}
		val namespaceNode = targetNamespace(commands, targetedNamespace)
		
		// Finds the targeted command
		// Option 1: Finds a command from the targeted namespace
		val result = namespaceNode
			.flatMap { _.nav._2.find { _.matchesName(commandName) }.map { _ -> targetedNamespace } }
			// Option 2: Finds a command from the non-namespaced group
			.orElse {
				if (namespaceInput.isEmpty && lastNamespace.nonEmpty)
					commands.nav._2.find { _.matchesName(commandName) }.map { _ -> Empty }
				else
					None
			}
			// Option 3: Finds a command from within the targeted namespace, including lower namespaces
			.orElse {
				if (namespaceInput.isEmpty)
					None
				else
					namespaceNode.flatMap { _.topDownNodesBelowIterator.findMap { node =>
						node.nav._2.find { _.matchesName(commandName) }.map { _ -> node.nav._1 }
					} }
			}
			// Option 4: Finds all commands matching the specified name.
			//           If there are many options, allows the user to select one.
			.orElse {
				commands.nodesBelowIterator
					.flatMap { node => node.nav._2.filter { _.matchesName(commandName) }.map { _ -> node.nav._1 } }
					.toOptimizedSeq.emptyOneOrMany
					.flatMap {
						case Left(only) => Some(only)
						case Right(many) =>
							println("Which of the following commands did you mean? (empty cancels)")
							StdIn.selectFrom(
								many.map { case p @ (c, namespace) =>
									p -> s"${ namespace.mkString(":").appendIfNotEmpty(":") }${ c.name }"
								},
								"commands")
					}
			}
		
		// May remember the targeted namespace
		result.foreach { case (_, namespace) => lastNamespace = namespace }
		
		if (result.isEmpty) {
			println(s"\"$input\" doesn't match any available command")
			proposeClosestMatch(commandName, targetedNamespace, commands)
			println("Use the \"help\" command to see a list of available commands")
		}
		
		result.map { _._1 }
	}
	
	/**
	 * Finds the targeted namespace (node)
	 * @param commands A tree listing all available commands ana namespaces
	 * @param namespace Targeted namespace
	 * @return Node which matches the targeted namespace. None if no node matched that namespace.
	 */
	private def targetNamespace(commands: Commands, namespace: Seq[String]): Option[Commands] = {
		// Option 1: Checks for an absolute namespace match
		commands.follow(namespace) { (node, namespace) => node._1.last == namespace }.orElse {
			// Option 2: Checks for a partial / relative namespace match
			namespace.headOption.flatMap { firstElem =>
				commands.topDownNodesBelowIterator.find { _.nav._1.headOption.contains(firstElem) }
					.flatMap { targetNamespace(_, namespace) }
			}
		}
	}
	
	private def proposeClosestMatch(input: String, namespace: Seq[String], options: Commands) = {
		val all = options.allNavsIterator
			.flatMap { case (namespace, commands) => commands.iterator.map { namespace -> _ } }.toOptimizedSeq
		val closest = all
			.bestMatch(
				{ c => c._2.name.isSimilarTo(input, 2) || c._2.alias.notEmpty.exists { _.isSimilarTo(input, 1) } },
				{ c => c._1.iterator.zip(namespace).forall { case (a, b) => a == b } },
				{ c => c._1 == namespace })
			.oneOrMany match
		{
			case Left(only) => Some(only)
			case Right(options) =>
				// Prefers items with shorter names
				val orderedOptions = options.sortBy { _._2.name.length }
				val optionsWithAlias = orderedOptions.filter { _._2.hasAlias }.sortBy { _._2.alias.length }
				val lowerIn = input.toLowerCase
				
				// Tests for a case where alias contains input
				optionsWithAlias.find { _._2.alias.toLowerCase.contains(lowerIn) }
					// Tests for a case where name contains input
					.orElse { orderedOptions.find { _._2.name.toLowerCase.contains(lowerIn) } }
					.orElse {
						val inputChars = lowerIn.toSet
						// Tests which alias contains most input characters
						optionsWithAlias.iterator
							.map { case (namespace, command) =>
								val chars = command.alias.toLowerCase.toSet
								(namespace, command, (chars & inputChars).size)
							}
							.maxByOption { _._3 }.filter { _._3 > 0 }
							.orElse {
								// Tests which name contains most input characters
								orderedOptions
									.map { case (namespace, command) =>
										val chars = command.name.toLowerCase.toSet
										(namespace, command, (chars & inputChars).size)
									}
									.maxByOption { _._3 }.filter { _._3 > 0 }
							}
							.map { case (namespace, command, _) => namespace -> command }
					}
		}
		closest.foreach { case (namespace, command) =>
			println(s"Did you mean ${ namespace.mkString(":").appendIfNotEmpty(":") }${command.nameAndAlias}?") }
	}
	
	private def listCommands(commands: Iterable[Command], header: String) = {
		println(StringUtils.asciiTableFrom[Command](
			commands.toOptimizedSeq.sortBy { _.name },
			Vector(
				"Name" -> { _.name },
				"Alias" -> { _.alias },
				"Arguments" -> { _.argumentsSchema.arguments.iterator.map { _.name }.mkString("\n") },
				"Description" -> { _.help.splitToLinesIterator(40).mkString("\n") }
			),
			header
		))
	}
}

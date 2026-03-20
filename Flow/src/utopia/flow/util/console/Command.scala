package utopia.flow.util.console

import utopia.flow.collection.immutable.Empty
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.util.console.Command.whiteSpaceOutsideQuotesR
import utopia.flow.util.StringExtensions._

object Command
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * A regular expression for splitting namespace elements from the command name / input
	 */
	val namespaceSplitRegex = Regex.escape(':')
	
	private val whiteSpaceOutsideQuotesR = Regex.whitespace.ignoringQuotations
	
	
	// OTHER    -------------------------
	
	/**
	 * Creates a new command that doesn't take any arguments
	 * @param name Name of this command
	 * @param alias Alias for this command (default = empty = no alias)
	 * @param help A helpful description of this command (default = empty = no description)
	 * @param execute Function to be called when this command should be executed
	 * @return A new command
	 */
	def withoutArguments(name: String, alias: String = "", help: String = "")(execute: => Unit) =
		_apply(name, alias, CommandArgumentsSchema.empty, help) { _ => execute }
	/**
	 * Creates a new command utilizing an existing command arguments schema
	 * @param schema Arguments schema
	 * @param name Name of this command
	 * @param alias Alias for this command (default = empty = no alias)
	 * @param help A helpful description of this command (default = empty = no description)
	 * @param execute Function to be called when this command should be executed. Accepts processed command arguments.
	 * @return A new command
	 */
	def withSchema(schema: CommandArgumentsSchema, name: String, alias: String = "", help: String = "")
	              (execute: CommandArguments => Unit) =
		_apply(name, alias, schema, help)(execute)
	
	/**
	 * Creates a new command
	 * @param name Name of this command. May include a namespace: e.g. "namespace1:namespace2:command-name"
	 * @param alias Alias for this command (default = empty = no alias)
	 * @param help A helpful description of this command (default = empty = no description)
	 * @param args Schemas for the arguments accepted by this command
	 * @param execute Function to be called when this command should be executed. Accepts processed command arguments.
	 * @return A new command
	 */
	def apply(name: String, alias: String = "", help: String = "")(args: ArgumentSchema*)
	         (execute: CommandArguments => Unit) =
		_apply(name, alias, CommandArgumentsSchema(args), help)(execute)
	
	private def _apply(name: String, alias: String, schema: CommandArgumentsSchema, help: String)
	                  (execute: CommandArguments => Unit) =
	{
		val nameParts = namespaceSplitRegex.split(name)
		new Command(nameParts.last, alias, nameParts.dropRight(1), schema, help)(execute)
	}
}

/**
 * Commands are used for interacting with users in a console. Usually commands accept arguments of some kind.
 * @param name Name of this command
 * @param alias Alias for this command. May be empty.
 * @param namespace Namespace for discerning between commands with the same name. Default = empty.
 * @param argumentsSchema Command argument definitions
 * @param help A description of what this command does. May be empty.
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.12.2
 */
class Command(val name: String, val alias: String = "", val namespace: Seq[String] = Empty,
              val argumentsSchema: CommandArgumentsSchema = CommandArgumentsSchema.empty, val help: String = "")
             (execute: CommandArguments => Unit)
	extends ArgumentMatchable
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * If this command has an alias, returns the alias. Otherwise returns the name.
	 */
	lazy val aliasOrName = alias.nonEmptyOrElse(name)
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return Whether this command receives at least one argument
	 */
	def takesArguments = argumentsSchema.nonEmpty
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = {
		val argsPart = argumentsSchema.arguments.map { arg => s" <${arg.name}>" }.mkString
		val descPart = if (hasHelp) s" // $help" else ""
		s"$nameAndAlias$argsPart$descPart"
	}
	
	
	// OTHER    ------------------------------
	
	/**
	 * Executes this command without arguments
	 * @param jsonParser Implicit json parser
	 */
	def apply()(implicit jsonParser: JsonParser): Unit = apply(Empty)
	/**
	 * Executes this command with the specified arguments
	 * @param firstParam First command argument
	 * @param moreParams More command arguments
	 * @param jsonParser Implicit json parser
	 */
	def apply(firstParam: String, moreParams: String*)(implicit jsonParser: JsonParser): Unit =
		apply(firstParam +: moreParams)
	/**
	 * Executes this command with the specified arguments
	 * @param arguments Arguments for executing this command
	 * @param jsonParser Implicit json parser
	 */
	def apply(arguments: Seq[String])(implicit jsonParser: JsonParser) =
		execute(CommandArguments(argumentsSchema, arguments))
	
	/**
	 * Processes the specified string into an argument list and then executes this command with those arguments
	 * @param argumentsString A string representing an argument list
	 * @param jsonParser Implicit json parser
	 */
	def parseAndExecute(argumentsString: String)(implicit jsonParser: JsonParser) =
		if (argumentsString.isEmpty) apply() else apply(whiteSpaceOutsideQuotesR.split(argumentsString))
}

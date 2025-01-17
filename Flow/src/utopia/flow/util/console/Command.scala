package utopia.flow.util.console

import utopia.flow.collection.immutable.Empty
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.util.console.Command.whiteSpacesOutsideQuotationsRegex
import utopia.flow.util.StringExtensions._

object Command
{
	private val whiteSpacesOutsideQuotationsRegex = Regex.whiteSpace.ignoringQuotations
	
	/**
	 * Creates a new command that doesn't take any arguments
	 * @param name Name of this command
	 * @param alias Alias for this command (default = empty = no alias)
	 * @param help A helpful description of this command (default = empty = no description)
	 * @param execute Function to be called when this command should be executed
	 * @return A new command
	 */
	def withoutArguments(name: String, alias: String = "", help: String = "")(execute: => Unit) =
		new Command(name, alias, help = help)(_ => execute)
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
	              (execute: CommandArguments => Unit) = new Command(name, alias, schema, help)(execute)
	
	/**
	 * Creates a new command
	 * @param name Name of this command
	 * @param alias Alias for this command (default = empty = no alias)
	 * @param help A helpful description of this command (default = empty = no description)
	 * @param args Schemas for the arguments accepted by this command
	 * @param execute Function to be called when this command should be executed. Accepts processed command arguments.
	 * @return A new command
	 */
	def apply(name: String, alias: String = "", help: String = "")
	         (args: ArgumentSchema*)
	         (execute: CommandArguments => Unit) =
		new Command(name, alias, CommandArgumentsSchema(args), help)(execute)
}

/**
 * Commands are used for interacting with users in a console. Usually commands accept arguments of some kind.
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.12.2
 */
class Command(val name: String, val alias: String = "",
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
	 * Executes this command with the specified arguments
	 * @param arguments Arguments for executing this command
	 * @param jsonParser Implicit json parser
	 */
	def apply(arguments: Seq[String])(implicit jsonParser: JsonParser) =
		execute(CommandArguments(argumentsSchema, arguments))
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
	 * Processes the specified string into an argument list and then executes this command with those arguments
	 * @param argumentsString A string representing an argument list
	 * @param jsonParser Implicit json parser
	 */
	def parseAndExecute(argumentsString: String)(implicit jsonParser: JsonParser) =
		if (argumentsString.isEmpty) apply() else apply(whiteSpacesOutsideQuotationsRegex.split(argumentsString))
}

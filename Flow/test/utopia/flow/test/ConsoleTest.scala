package utopia.flow.test

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.util.console.{ArgumentSchema, Command, Console}
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import TestContext._

import scala.io.StdIn

/**
 * Provides a test console
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 */
object ConsoleTest extends App
{
	implicit val jsonParser: JsonParser = JsonReader
	
	val terminatedPointer = Pointer(false)
	val forgottenCommandsPointer = EventfulPointer(Set[String]())
	
	val closeCommand = Command.withoutArguments("quit", "q", "Closes this console") {
		terminatedPointer.value = true }
	val helloCommand = Command("hello", help = "Prints a greeting")(
		ArgumentSchema("name", help = "Name of the person to greet")) { args =>
		args.unrecognized.foreach { arg => println(s"Warning: Unrecognized argument $arg") }
		args("name").string match
		{
			case Some(name) => println(s"Hello $name!")
			case None => println("Hello!")
		}
	}
	val forgetCommand = Command("forget")(
		ArgumentSchema("command", defaultValue = "hello", help = "Name of the command to forget")) { args =>
		val commandName = args("command").getString
		forgottenCommandsPointer.update { _ + commandName }
		println(s"I forgot how to $commandName...")
	}
	val selectManyCommand = Command.withoutArguments("select-many") {
		val selected = StdIn.selectMany(Vector(
			1 -> "Option 1", 2 -> "Second Option", 3 -> "Option numero tres", 4 -> "One more option"))
		println(s"Selected options: ${ selected.mkString(", ") }")
	}
	
	val activeCommandsPointer = forgottenCommandsPointer.map { forgotten =>
		Vector(closeCommand, helloCommand, selectManyCommand, forgetCommand)
			.filterNot { command => forgotten.contains(command.name) }
	}
	
	println("Welcome to test console. Command to quit is 'quit' or 'exit'")
	Console(activeCommandsPointer, "Please type a command", terminatedPointer, "exit").run()
	println("Thank you!")
}

package utopia.flow.test

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.util.console.{ArgumentSchema, Command, Console}

/**
 * Provides a test console
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 */
object ConsoleTest extends App
{
	DataType.setup()
	implicit val jsonParser: JsonParser = JSONReader
	
	val terminatedPointer = Pointer(false)
	val forgottenCommandsPointer = new PointerWithEvents(Set[String]())
	
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
	
	val activeCommandsPointer = forgottenCommandsPointer.map { forgotten =>
		Vector(closeCommand, helloCommand, forgetCommand).filterNot { command => forgotten.contains(command.name) }
	}
	
	println("Welcome to test console. Command to quit is 'quit' or 'exit'")
	Console(activeCommandsPointer, "Please type a command", terminatedPointer, "exit").run()
	println("Thank you!")
}

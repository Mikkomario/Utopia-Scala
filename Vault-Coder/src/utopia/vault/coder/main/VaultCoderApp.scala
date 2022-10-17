package utopia.vault.coder.main

import utopia.flow.util.console.CommandArguments
import utopia.vault.coder.util.Common.jsonParser

/**
  * The command line application for this project, which simply reads data from a json file and outputs it to a certain
  * location
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
object VaultCoderApp extends App
{
	// Determines the program / logic to run
	val options = Vector(MainAppLogic, TableReadAppLogic)
	val logicByCommand = args.headOption.flatMap { cName =>
		val lower = cName.toLowerCase
		options.find { _.name.toLowerCase == lower }
	}
	val logic = logicByCommand.getOrElse(options.head)
	
	// Parses the arguments
	val arguments = CommandArguments(logic.argumentSchema,
		if (logicByCommand.isDefined) args.toVector.drop(1) else args.toVector)
	// Writes hints and warnings
	if (arguments.unrecognized.nonEmpty)
		println(s"Warning! Following arguments were not recognized: ${arguments.unrecognized.mkString(", ")}")
	if (arguments.values.isEmpty) {
		println("Hint: This program supports following command line arguments:")
		arguments.schema.arguments.foreach { arg => println("- " + arg) }
		println()
	}
	
	// Runs the program
	logic(arguments)
}

package utopia.coder.controller.app

import utopia.flow.parse.json.JsonParser
import utopia.flow.util.console.CommandArguments

/**
  * Common trait for the command line applications that use this module
  * @author Mikko Hilpinen
  * @since 4.9.2021, v0.1
  */
trait CoderApp
{
	// ABSTRACT ---------------------
	
	/**
	  * @return The modes or logic options that may be used by this application
	  */
	protected def logicOptions: Iterable[AppLogic]
	
	/**
	  * Runs this application
	  * @param args Command line arguments
	  * @param jsonParser Implicit json parser to use
	  */
	def run(args: Seq[String])(implicit jsonParser: JsonParser) = {
		// Determines the program / logic to run
		val options = logicOptions
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
			println(s"Warning! Following arguments were not recognized: ${ arguments.unrecognized.mkString(", ") }")
		if (arguments.values.isEmpty) {
			println("Hint: This program supports following command line arguments:")
			arguments.schema.arguments.foreach { arg => println(s"- $arg") }
			println()
		}
		
		// Runs the program
		logic(arguments)
	}
}

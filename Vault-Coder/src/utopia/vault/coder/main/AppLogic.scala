package utopia.vault.coder.main

import utopia.flow.util.console.{ArgumentSchema, CommandArguments}

/**
  * A common trait for application logic implementations
  * @author Mikko Hilpinen
  * @since 17.10.2022, v1.7.1
  */
trait AppLogic
{
	/**
	  * @return The name of this program / logic
	  */
	def name: String
	
	/**
	  * @return Schema for the command arguments to parse
	  */
	def argumentSchema: Vector[ArgumentSchema]
	
	/**
	  * Runs this logic
	  * @param args Parsed command arguments
	  */
	def apply(args: CommandArguments): Unit
}

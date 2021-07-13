package utopia.flow.util.console

import utopia.flow.util.StringExtensions._

object CommandArgumentsSchema
{
	/**
	 * An empty schema
	 */
	val empty = CommandArgumentsSchema(Vector())
}

/**
 * Used for determining how command line arguments are supposed to be interpreted
 * @author Mikko Hilpinen
 * @since 26.6.2021, v1.10
 */
case class CommandArgumentsSchema(arguments: Vector[ArgumentSchema])
{
	/**
	 * Finds an argument schema that matches the specified name
	 * @param name Argument name
	 * @return A schema matching that name
	 */
	def apply(name: String) = arguments.find { _.name ~== name }
		.orElse { arguments.find { a => a.alias.nonEmpty && (a.alias ~== name) } }
}

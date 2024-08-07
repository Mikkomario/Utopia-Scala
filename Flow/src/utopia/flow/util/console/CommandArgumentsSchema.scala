package utopia.flow.util.console

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.MaybeEmpty

object CommandArgumentsSchema
{
	/**
	 * An empty schema
	 */
	val empty = CommandArgumentsSchema(Empty)
}

/**
 * Used for determining how command line arguments are supposed to be interpreted
 * @author Mikko Hilpinen
 * @since 26.6.2021, v1.10
 */
case class CommandArgumentsSchema(arguments: Seq[ArgumentSchema]) extends MaybeEmpty[CommandArgumentsSchema]
{
	// IMPLEMENTED  -----------------------
	
	override def self = this
	
	override def isEmpty = arguments.isEmpty
	
	
	// OTHER    --------------------------
	
	/**
	 * Finds an argument schema that matches the specified name
	 * @param name Argument name
	 * @return A schema matching that name
	 */
	def apply(name: String) = arguments.find { _.matchesName(name) }
}

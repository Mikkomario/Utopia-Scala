package utopia.flow.util.console

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value

object ArgumentSchema
{
	/**
	 * Creates a flag argument schema
	 * @param name Parameter / argument name
	 * @param alias Parameter / argument alias (default = empty = no alias)
	 * @param help A (helpful) description for this argument (default = empty)
	 * @param defaultValue Default state (default = false)
	 * @return A new command argument schema for boolean flags
	 */
	def flag(name: String, alias: String = "", help: String = "", defaultValue: Boolean = false) =
		apply(name, alias, defaultValue)
}

/**
 * Determines how a single command line argument should be interpreted
 * @author Mikko Hilpinen
 * @since 26.6.2021, v1.10
 * @param name Name of this parameter (used in code side and can be used by client side also)
 * @param alias Short name / code for this parameter (empty string means no alias)
 * @param defaultValue Value used by default (default = empty)
 * @param help A helpful explanation for this argument (default = empty)
 */
case class ArgumentSchema(name: String, alias: String = "", defaultValue: Value = Value.empty, help: String = "")
	extends ArgumentMatchable
{
	/**
	 * @return Whether this schema has a defined default value
	 */
	def hasDefault = defaultValue.isDefined
	
	override def toString =
	{
		val defaultPart = if (hasDefault) s" default=${defaultValue.toJson}" else ""
		val descriptionPart = if (hasHelp) s" // $help" else ""
		s"<$nameAndAlias>" + defaultPart + descriptionPart
	}
}

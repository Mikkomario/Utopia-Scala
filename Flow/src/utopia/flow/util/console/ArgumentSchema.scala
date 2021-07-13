package utopia.flow.util.console

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._

object ArgumentSchema
{
	/**
	 * Creates a flag argument schema
	 * @param name Parameter / argument name
	 * @param alias Parameter / argument alias (default = empty = no alias)
	 * @param defaultValue Default state (default = false)
	 * @return A new command argument schema for boolean flags
	 */
	def flag(name: String, alias: String = "", defaultValue: Boolean = false) =
		apply(name, alias, defaultValue)
}

/**
 * Determines how a single command line argument should be interpreted
 * @author Mikko Hilpinen
 * @since 26.6.2021, v1.10
 * @param name Name of this parameter (used in code side and can be used by client side also)
 * @param alias Short name / code for this parameter (empty string means no alias)
 * @param defaultValue Value used by default (default = empty)
 */
case class ArgumentSchema(name: String, alias: String = "", defaultValue: Value = Value.empty)
{
	/**
	  * @return argument alias as an option (None if empty)
	  */
	def aliasOption = alias.notEmpty
	
	override def toString = s"$name${aliasOption match {
		case Some(alias) => s" ($alias)"
		case None => ""
	}}${if (defaultValue.isDefined) s" default=${defaultValue.toJson}" else ""}"
}

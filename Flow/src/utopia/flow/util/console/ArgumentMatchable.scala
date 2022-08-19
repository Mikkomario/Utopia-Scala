package utopia.flow.util.console

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.util.StringExtensions._

/**
 * A common trait for items which can be matched to a single part of console input
 * (e.g. command or argument name)
 * @author Mikko Hilpinen
 * @since 10.10.2021, v1.13
 */
trait ArgumentMatchable
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Name of this item
	 */
	def name: String
	/**
	 * @return Alias of this item. Empty if there is no alias.
	 */
	def alias: String
	/**
	 * @return A help description of this item. Empty if there is no description.
	 */
	def help: String
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether this item has an alias
	 */
	def hasAlias = alias.nonEmpty
	/**
	 * @return This item's alias. None if this item doesn't have one.
	 */
	def aliasOption = alias.notEmpty
	
	/**
	 * @return Whether this item contains a helpful usage description
	 */
	def hasHelp = help.nonEmpty
	/**
	 * @return A helpful description of this item. None if there is no description.
	 */
	def helpOption = help.notEmpty
	
	/**
	 * @return Name of this item, including the alias in parentheses (if there is one)
	 */
	def nameAndAlias = if (hasAlias) s"$name ($alias)" else name
	
	
	// OTHER    ---------------------------------
	
	/**
	 * @param name A name
	 * @return Whether this item's name or alias matches that name
	 */
	def matchesName(name: String) = (this.name ~== name) || aliasOption.exists { _ ~== name }
}

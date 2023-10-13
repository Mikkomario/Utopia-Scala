package utopia.courier.model.read

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.string.Regex

object FolderPath
{
	// ATTRIBUTES   ---------------------------
	
	private val delimiterRegex = Regex.escape('/') || Regex.backslash
	
	
	// OTHER    -------------------------------
	
	/**
	 * @param path A folder path as a string
	 * @return The specified path as a folder path
	 */
	def apply(path: String): FolderPath = apply(delimiterRegex.split(path).toVector)
}

/**
 * Represents a path to a specific folder
 * @author Mikko Hilpinen
 * @since 13.10.2023, v1.1
 */
case class FolderPath(parts: Vector[String]) extends MaybeEmpty[FolderPath]
{
	// COMPUTED --------------------------
	
	/**
	 * @return The name of this folder
	 */
	def name = parts.lastOption.getOrElse("")
	
	/**
	 * @return Path to the parent of this folder. This if this is a root folder.
	 */
	def parent = parentOption.getOrElse(this)
	/**
	 * @return Path to the parent of this folder. None if this is a root folder.
	 */
	def parentOption = if (parts.isEmpty) None else Some(copy(parts.dropRight(1)))
	/**
	 * @return An iterator that returns the direct parent of this folder, then the parent of that folder, and so on,
	 *         until it reaches the root folder, which is the last returned element.
	 */
	def parentsIterator = OptionsIterator.iterate(parentOption) { _.parentOption }
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: FolderPath = this
	
	override def isEmpty: Boolean = parts.isEmpty
	
	override def toString = parts.mkString("/")
	
	
	// OTHER    --------------------------
	
	/**
	 * @param childName Name of the targeted sub-folder
	 * @return Path to the specified folder
	 */
	def /(childName: String) = copy(parts :+ childName)
}
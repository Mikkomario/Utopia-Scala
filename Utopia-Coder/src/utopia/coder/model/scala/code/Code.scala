package utopia.coder.model.scala.code

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.MaybeEmpty
import utopia.coder.model.merging.MergeConflict
import utopia.coder.model.scala.datatype.Reference
import utopia.coder.model.scala.template.{CodeConvertible, Referencing}

import scala.language.implicitConversions

object Code
{
	// ATTRIBUTES   ------------------------------
	
	/**
	  * An empty set of code
	  */
	val empty = Code(Vector())
	
	
	// IMPLICIT ----------------------------------
	
	implicit def lineToCode(line: CodeLine): Code = if (line.isEmpty) empty else apply(Vector(line))
	implicit def stringToCode(codeLine: String): Code = if (codeLine.isEmpty) empty else apply(Vector(CodeLine(codeLine)))
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Wraps a single line of code without references
	  * @param line A line of code
	  * @return That line of code wrapped
	  */
	def apply(line: String): Code = apply(Vector(CodeLine(line)))
	
	/**
	  * @param lines Code line strings
	  * @return A code based on those lines
	  */
	def from(lines: Vector[String]) = apply(lines.map { CodeLine(_) })
}

/**
  * Represents one or more lines of scala code
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Code(lines: Vector[CodeLine], references: Set[Reference] = Set())
	extends Referencing with CodeConvertible with MaybeEmpty[Code]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this code contains multiple lines
	  */
	def isMultiLine = lines.size > 1
	/**
	  * @return Whether this code contains a single line only
	  */
	def isSingleLine = lines.size < 2
	
	/**
	  * @return This code with line splitting applied
	  */
	def split = copy(lines = lines.flatMap { _.split })
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self = this
	
	/**
	  * @return Whether this code is totally empty
	  */
	override def isEmpty = lines.isEmpty
	
	override def toString = lines.map { _.toString }.mkString("\n")
	
	override def toCode = this
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param line A prefix line
	  * @return A copy of this code with the specified line prepended
	  */
	def +:(line: String) = copy(CodeLine(line) +: lines)
	/**
	  * @param line A postfix line
	  * @return A copy of this code with the specified line added
	  */
	def :+(line: String) = copy(lines :+ CodeLine(line))
	
	/**
	  * Combines these two codes, writing the back to back
	  * @param other Another code
	  * @return A combination of these codes
	  */
	def ++(other: Code) = Code(lines ++ other.lines, references ++ other.references)
	
	/**
	  * @param reference A reference
	  * @return A copy of this code including that reference also
	  */
	def referringTo(reference: Reference) = copy(references = references + reference)
	/**
	  * @param refs A set of references
	  * @return A copy of this code referring to those items also
	  */
	def referringTo(refs: IterableOnce[Reference]) =
	{
		val iter = refs.iterator
		if (iter.hasNext)
			copy(references = references ++ refs)
		else
			this
	}
	/**
	  * @param firstRef A reference
	  * @param secondRef Another reference
	  * @param moreRefs More references
	  * @return A copy of this code with those references included
	  */
	def referringTo(firstRef: Reference, secondRef: Reference, moreRefs: Reference*): Code =
		referringTo(Set(firstRef, secondRef) ++ moreRefs)
	
	/**
	  * @param f A mapping function for code lines
	  * @return A mapped code
	  */
	def mapLines(f: CodeLine => CodeLine) = copy(lines = lines.map(f))
	
	/**
	  * Creates a copy of this code where all lines are prefixed with the specified string
	  * @param prefix String to add to the beginning of every line in this code
	  * @return A prefixed copy of this code
	  */
	def prependAll(prefix: String) = if (prefix.isEmpty) this else mapLines { _.prepend(prefix) }
	
	/**
	  * @param other Another code
	  * @return Whether this code is meaningfully different from the other code
	  */
	def conflictsWith(other: Code) = lines.map { _.code }.mkString != other.lines.map { _.code }.mkString
	
	/**
	  * @param other Another code
	  * @param description Conflict description
	  * @return Merge conflict between these two codes
	  */
	def conflictWith(other: Code, description: => String = "") =
	{
		val conflictLines =
			lines.padTo(other.lines.size, CodeLine.empty).zip(other.lines.padTo(lines.size, CodeLine.empty))
				.dropWhile { case (a, b) => a.code == b.code }
				.dropRightWhile { case (a, b) => a.code == b.code }
		if (conflictLines.nonEmpty)
			Some(MergeConflict(conflictLines.map { _._2 }.dropRightWhile { _.isEmpty },
				conflictLines.map { _._1 }.dropRightWhile { _.isEmpty }, description))
		else
			None
	}
}

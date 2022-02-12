package utopia.vault.coder.model.scala.code

import utopia.flow.operator.Combinable
import utopia.vault.coder.model.scala.datatype.Reference
import utopia.vault.coder.model.scala.template.Referencing

import scala.language.implicitConversions

object CodePiece
{
	val empty = CodePiece("")
	
	implicit def textToCode(text: String): CodePiece = apply(text)
}

/**
  * Represents a segment of code that doesn't span the whole line
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.1.1
  */
case class CodePiece(text: String, references: Set[Reference] = Set())
	extends Referencing with Combinable[CodePiece, CodePiece]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Number of characters in this code piece
	  */
	def length = text.length
	
	/**
	  * @return Whether this code piece is empty
	  */
	def isEmpty = text.isEmpty
	/**
	  * @return Whether this code piece contains text
	  */
	def nonEmpty = !isEmpty
	/**
	  * @return This piece of code, if not empty (None if empty).
	  */
	def notEmpty = if (isEmpty) None else Some(this)
	
	/**
	  * @return A copy of this code piece wrapped in (parentheses)
	  */
	def withinParenthesis = copy(s"($text)")
	/**
	  * @return A copy of this code piece wrapped in [brackets]
	  */
	def withinSquareBrackets = copy(s"[$text]")
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = text
	
	/**
	  * Combines these two pieces of code together
	  * @param another Another code piece
	  * @return A combination of these two pieces
	  */
	override def +(another: CodePiece) = copy(text + another.text, references ++ another.references)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param more More text
	  * @return A copy of this code piece with more text included
	  */
	def +(more: String) = copy(text + more)
	
	/**
	  * @param other Another code piece
	  * @param separator A separator added between these pieces (default = empty)
	  * @return A combination of these pieces
	  */
	def append(other: CodePiece, separator: => String = "") = {
		// Only adds the separator if the other piece is non-empty
		val newText = if (other.isEmpty) text else if (isEmpty) other.text else text + separator + other.text
		copy(newText, references ++ other.references)
	}
	
	/**
	  * @param prefix A prefix
	  * @return A copy of this code piece with that prefix in the beginning
	  */
	def withPrefix(prefix: String) = copy(prefix + text)
	
	/**
	  * @param ref A reference
	  * @return A copy of this code piece with that reference included
	  */
	def referringTo(ref: Reference) = copy(references = references + ref)
	/**
	  * @param refs A set of references
	  * @return A copy of this code piece with those references included
	  */
	def referringTo(refs: IterableOnce[Reference]) = copy(references = references ++ refs)
}
package utopia.vault.coder.model.scala.code

import utopia.flow.generic.factory.FromValueFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Combinable.SelfCombinable
import utopia.vault.coder.model.scala.datatype.Reference
import utopia.vault.coder.model.scala.template.Referencing

import scala.language.implicitConversions

object CodePiece extends FromValueFactory[CodePiece]
{
	val empty = apply("")
	val none = apply("None")
	
	implicit def textToCode(text: String): CodePiece = apply(text)
	
	override def default = empty
	
	/**
	  * Converts a value into a code piece. The value may be either a model with properties 'code' and
	  * 'references' or 'reference', or a string. The 'references' -property is expected to contain a vector or
	  * references (as strings). The optional 'reference' -property is expected to contain a single reference
	  * (as a string)
	  * @param value A value
	  * @return A code piece read from that value
	  */
	def fromValue(value: Value) = value.model.filter { _.contains("code") } match {
		case Some(model) =>
			Some(apply(model("code").getString, model("references").getVector.flatMap { v => v.string }.toSet
				.filterNot { _.isEmpty }.map(Reference.apply) ++ model("reference").string.map(Reference.apply)))
		case None => value.string.map { apply(_) }
	}
}

/**
  * Represents a segment of code that doesn't span the whole line
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.1.1
  */
case class CodePiece(text: String, references: Set[Reference] = Set())
	extends Referencing with SelfCombinable[CodePiece]
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
	
	/**
	  * Converts this code piece into a simple sql value, if possible
	  * @return sql that matches this piece of code, if possible
	  */
	def toSql = {
		// Case: Empty code or a code that uses external references => fails
		if (text.isEmpty || references.nonEmpty)
			None
		else {
			val lowerText = text.toLowerCase
			// Case: Boolean
			if (lowerText == "true" || lowerText == "false")
				Some(text.toUpperCase)
			// Case: String literal
			else if (text.startsWith("\"") && text.endsWith("\"")) {
				val quoted = text.drop(1).dropRight(1)
				if (quoted.contains('"'))
					None
				else
					Some(s"'$quoted'")
			}
			else if (text.contains('.'))
				text.toDoubleOption.map { _.toString }
			else
				text.toIntOption.map { _.toString }
		}
	}
	
	
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
	def +:(prefix: String) = copy(prefix + text)
	
	/**
	  * Applies a mapping function to the text portion of this code piece
	  * (ie. maps texts while keeping the same references)
	  * @param f A mapping function to apply
	  * @return A copy of this code piece with mapped text
	  */
	def mapText(f: String => String) = copy(f(text))
	
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
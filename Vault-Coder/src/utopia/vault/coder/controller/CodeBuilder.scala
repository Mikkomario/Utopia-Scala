package utopia.vault.coder.controller

import utopia.flow.datastructure.mutable.{Pointer, ResettableLazy}
import utopia.vault.coder.model.scala.code
import utopia.vault.coder.model.scala.code.{Code, CodeLine, CodePiece}
import utopia.vault.coder.model.scala.template.Referencing
import utopia.vault.coder.model.scala.Reference

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

/**
  * Used for combining code lines into a code
  * @author Mikko Hilpinen
  * @since 27.9.2021, v1.1
  */
class CodeBuilder(startIndentation: Int = 0) extends mutable.Builder[String, Code]
{
	// ATTRIBUTES   --------------------------
	
	private val linesBuilder = new VectorBuilder[CodeLine]()
	private val references = mutable.Set[Reference]()
	
	private var currentIndent = startIndentation
	
	private val openLineCache = ResettableLazy { Pointer(CodeLine(currentIndent, "")) }
	
	
	// IMPLEMENTED  -------------------------
	
	override def clear() =
	{
		linesBuilder.clear()
		currentIndent = startIndentation
	}
	
	override def result() = code.Code(linesBuilder.result() ++ openLineCache.popCurrent().map { _.value }, references.toSet)
	
	override def addOne(elem: String) =
	{
		closeOpenLine()
		linesBuilder += CodeLine(currentIndent, elem)
		this
	}
	
	override def addAll(xs: IterableOnce[String]) =
	{
		closeOpenLine()
		linesBuilder ++= xs.iterator.map { code => CodeLine(currentIndent, code) }
		this
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * Adds a reference to this builder
	  * @param reference A reference to include
	  * @return This builder
	  */
	def +=(reference: Reference) =
	{
		references += reference
		this
	}
	/**
	  * Appends a piece of code to this builder, but leaves the line open for possibly more pieces
	  * @param codePiece A piece of code
	  * @return This builder
	  */
	def +=(codePiece: CodePiece) = appendPartial(codePiece)
	
	/**
	  * Appends code to this builder. Adds relative indentation.
	  * @param code Code to append
	  * @return This builder
	  */
	def ++=(code: Code) =
	{
		closeOpenLine()
		linesBuilder ++= code.lines.map { _.multiIndented(currentIndent) }
		references ++= code.references
		this
	}
	
	/**
	  * Updates the indentation used for the following lines
	  * @param indentation Indentation to use from now on
	  */
	def setIndentation(indentation: Int) = currentIndent = indentation
	/**
	  * Resets the current indentation back to 0
	  */
	def resetIndentation() = setIndentation(0)
	
	/**
	  * Adds a reference to this builder
	  * @param reference A reference to include
	  * @return This builder
	  */
	def addReference(reference: Reference) = this += reference
	/**
	  * Adds references to this builder
	  * @param references references to include
	  * @return This builder
	  */
	def addReferences(references: IterableOnce[Reference]) =
	{
		this.references ++= references
		this
	}
	/**
	  * Adds references to this builder
	  * @return This builder
	  */
	def addReferences(firstRef: Reference, secondRef: Reference, moreRefs: Reference*): CodeBuilder =
		addReferences(Set(firstRef, secondRef) ++ moreRefs)
	/**
	  * Adds items referred by the specified items to this builder
	  * @param items Items that make references
	  * @return This builder
	  */
	def addReferencesFrom(items: IterableOnce[Referencing]) =
		addReferences(items.iterator.flatMap { _.references })
	
	/**
	  * Appends a partial line to the code. This will be combined with other subsequent calls of appendPartial(...)
	  * to form a single line of code
	  * @param code Code to append
	  * @param separator Separator to place before this code, in case the line was already open (default = "")
	  * @param allowLineSplit Whether line splitting should be allowed if the open line is about to become too long.
	  *                       Default = false.
	  * @return This builder
	  */
	def appendPartial(code: CodePiece, separator: => String = "", allowLineSplit: Boolean = false) =
	{
		openLineCache.value.update { old =>
			// Case: No line open yet => just adds the code
			if (old.isEmpty)
				old + code.text
			// Case: Line split is supported and the code won't fit to the same line
			// => adds the opened line and starts a new one (indented)
			else if (allowLineSplit && old.length + separator.length + code.length > CodeLine.maxLineLength)
			{
				linesBuilder += old + separator
				CodeLine(old.indentation + 1, code.text)
			}
			// Case: Code fits to the line => continues
			else
				old + separator + code.text
		}
		references ++= code.references
		this
	}
	
	/**
	  * Indents this builder so that the following added lines will be indented
	  * @return This builder
	  */
	def indent() =
	{
		closeOpenLine()
		currentIndent += 1
		this
	}
	/**
	  * Cancels the last indentation so that the following lines will be indented one less time
	  * @return This builder
	  */
	def unindent() =
	{
		closeOpenLine()
		currentIndent = (currentIndent - 1) max 0
		this
	}
	/**
	  * Indents this builder for the duration of a function call, after which this builder is unindented again.
	  * @param f A function for adding / writing code
	  * @tparam U Function result type
	  * @return Function result
	  */
	def indented[U](f: CodeBuilder => U) =
	{
		indent()
		val result = f(this)
		unindent()
		result
	}
	
	/**
	  * Opens a new { block } of code
	  * @return This builder
	  */
	def openBlock() =
	{
		this += "{"
		indent()
	}
	/**
	  * Closes the previously openened { block } of code
	  * @return This builder
	  */
	def closeBlock() =
	{
		unindent()
		this += "}"
	}
	/**
	  * Adds an indented block to this builder. Uses the specified function to fill that block using this same builder.
	  * @param f A function used for filling the code block
	  * @tparam U Result type
	  * @return Function result
	  */
	def block[U](f: CodeBuilder => U) =
	{
		openBlock()
		val result = f(this)
		closeBlock()
		result
	}
	
	/**
	  * @return Adds an empty line to this builder
	  */
	def addEmptyLine() = this += ""
	
	/**
	  * Adds indented code to this builder
	  * @param code Code to add (before indentation)
	  * @return This builder
	  */
	def addIndented(code: Code) =
	{
		indent()
		this ++= code
		unindent()
	}
	/**
	  * Adds a block of code to this builder. The block is indented and surrounded by brackets { }
	  * @param code Code to add
	  * @return This builder
	  */
	def addBlock(code: Code) = block { _ ++= code }
	
	/**
	  * Finishes / closes a partial / open line if one is present
	  */
	def closeOpenLine() = openLineCache.popCurrent().foreach { linesBuilder += _.value }
}

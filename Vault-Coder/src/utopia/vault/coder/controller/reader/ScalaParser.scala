package utopia.vault.coder.controller.reader

import utopia.flow.collection.PollingIterator
import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.IterateLines
import utopia.vault.coder.model.reader.ReadCodeBlock
import utopia.vault.coder.model.scala.Package
import utopia.vault.coder.model.scala.Visibility.{Private, Protected, Public}
import utopia.vault.coder.model.scala.declaration.DeclarationTypeCategory.Instance
import utopia.vault.coder.model.scala.declaration.{DeclarationPrefix, DeclarationType, DeclarationTypeCategory}

import java.nio.file.Path
import scala.collection.immutable.VectorBuilder

/**
  * Used for reading and interpreting scala files
  * @author Mikko Hilpinen
  * @since 29.10.2021, v1.3
  */
object ScalaParser
{
	private val packageRegex = Regex("package ") + Regex.any
	private val importRegex = Regex("import ") + Regex.any
	
	private val objectOrClassRegex = Regex.any + (Regex("object ") || Regex("class ")).withinParenthesis + Regex.any
	
	private val quotesRegex = Regex.escape('"') + Regex.any + Regex.escape('"')
	
	private val visibilityRegex = (Regex("protected ") || Regex("private ")).withinParenthesis
	private val declarationPrefixRegex = DeclarationPrefix.values.map { p => Regex(p.keyword + " ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private val declarationModifierRegex = (visibilityRegex || declarationPrefixRegex).withinParenthesis
	private val declarationKeywordRegex = DeclarationType.values.map { d => Regex(d.keyword + " ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private val declarationStartRegex = declarationModifierRegex.zeroOrMoreTimes + declarationKeywordRegex
	private val namedDeclarationStartRegex = declarationStartRegex + Regex.word + Regex("\\_\\=").noneOrOnce
	
	// private val functionArrowRegex = Regex("\\=\\> ")
	
	private val parameterPartRegex = (declarationStartRegex + Regex.word + Regex.escape(':') + Regex.any)
		.withinParenthesis
	private val parameterListRegex = Regex.escape('(') +
		(parameterPartRegex + Regex.escape(',') + Regex.whiteSpace.noneOrOnce).withinParenthesis.zeroOrMoreTimes +
		parameterPartRegex.noneOrOnce + Regex.escape(')')
	
	private val extendsRegex = Regex(" extends ")
	private val withRegex = Regex(" with ")
	
	private val blockClosureRegex = Regex.escape('}')
	
	private val scalaDocStartRegex = Regex("\\/\\*\\*")
	private val commentEndRegex = Regex("\\*\\/")
	
	def apply(path: Path) =
	{
		IterateLines.fromPath(path) { linesIter =>
			val iter = linesIter.pollable
			
			// Searches for package declaration first
			val filePackage = iter.pollToNextWhere { _.nonEmpty }.filter(packageRegex.apply)
				.map { s => Package(s.afterFirst("package ")) }
			
			// Next looks for import statements
			val imports = iter.collectWhile { line => line.isEmpty || importRegex(line) }.filter { _.nonEmpty }
				.map { _.afterFirst("import ") }
				.flatMap { importString =>
					if (importString.contains('{'))
					{
						val (basePart, endPart) = importString.splitAtFirst("{")
						val trimmedBase = basePart.trim
						val endItems = endPart.untilFirst("}").split(',').toVector.map { _.trim }
						endItems.map { trimmedBase + _ }
					}
					else
						Vector(importString.trim)
				}
			
			// Finally, finds and processes the object and/or class statements
			val instances = iter.pollToNextWhere(objectOrClassRegex.apply) match
			{
				case Some(firstDeclaration) =>
					???
				case None => Vector()
			}
		}
	}
	
	private def instanceFrom(header: String, moreLines: Vector[String]) =
	{
		val headerLines = header +: moreLines.takeTo { line =>
			val quotesRemovedLine = quotesRegex.filterNot(line)
			quotesRemovedLine.afterLast(")").notEmpty.getOrElse(line).contains('{')
		}
		val fullHeader = headerLines.mkString(" ")
		val (beforeExtends, afterExtends) = fullHeader.splitAtLast("extends ")
		val extensions = withRegex.split(afterExtends)
		val parameterListRanges = parameterListRegex.rangesFrom(beforeExtends)
		val beforeParameters = if (parameterListRanges.isEmpty) beforeExtends else
			beforeExtends.substring(0, parameterListRanges.head.start)
		
		
		val remainingLines = moreLines.drop(headerLines.size - 1)
	}
	
	private def nextItemFrom(linesIter: PollingIterator[String]) =
	{
		// Checks what the next non-empty line looks like
		linesIter.nextWhere { _.nonEmpty }.map { firstLine =>
			// Case: Item is prepended by scaladoc => includes the documentation in the item
			if (scalaDocStartRegex.existsIn(firstLine))
			{
				if (commentEndRegex.existsIn(firstLine))
				{
					// TODO: Parse next item and attach the scaladoc to it
					val scalaDoc = firstLine
					???
				}
				else
				{
					// TODO: Same here
					val docLines = firstLine +: linesIter.collectTo(commentEndRegex.existsIn)
					???
				}
			}
			// Case: Item starts without scaladoc => parses it
			else
			{
				// Collects lines before the first declaration, if necessary
				val (beforeDeclarationLines, declarationLine) = {
					if (namedDeclarationStartRegex.existsIn(firstLine))
						Vector() -> Some(firstLine)
					else
						(firstLine +: linesIter.collectUntil(namedDeclarationStartRegex.existsIn)) ->
							linesIter.nextOption()
				}
				declarationLine match
				{
					case Some(declarationLine) =>
						// Identifies the declaration in question
						val declarationStartRange = namedDeclarationStartRegex.firstRangeFrom(declarationLine).get
						val declarationStart = declarationLine.slice(declarationStartRange)
						val afterDeclarationStart = declarationLine.substring(declarationStartRange.exclusiveEnd)
						val declarationType = DeclarationType.values
							.find { d => declarationStart.contains(d.keyword + " ") }.get
						// Parses prefixes and visibility
						val prefixes = declarationType.availablePrefixes
							.filter { prefix => declarationStart.contains(prefix.keyword + " ") }
						val visibility = {
							if (declarationStart.contains("private "))
								Private
							else if (declarationStart.contains("protected "))
								Protected
							else
								Public
						}
						// Parses parameter list, if needed
						val (parameterLists, afterParameterLists) = {
							if (declarationType.acceptsParameterList && afterDeclarationStart.startsWith("(")) {
								val (lists, remaining) = readParameterLists(afterDeclarationStart, linesIter)
								Some(lists) -> remaining
							}
							else
								None -> afterDeclarationStart.trim.notEmpty
						}
						// Handles functions and instances differently
						declarationType.category match
						{
							case DeclarationTypeCategory.Function =>
								// Expects an assignment operator before a body
								// - if no operator is given, assumes that the function is abstract
								if (afterParameterLists.exists { _.contains('=') })
									???
								else
									???
							
							case Instance =>
								// Looks for extends portion
								// The instance body is always expected to be wrapped in a block,
								// which is processed separately
								val (extensions, contentBlock) = extensionsAndBlockFrom(afterParameterLists, linesIter)
								???
						}
					case None => ???
				}
			}
		}
	}
	
	private def extensionsAndBlockFrom(openLine: Option[String], moreLinesIter: PollingIterator[String]) =
	{
		// Case: Instance block starts on the first line
		if (openLine.exists { _.contains('{') })
		{
			val (beforeBlock, afterBlockStart) = openLine.get.splitAtFirst("{")
			val (block, _) = readBlock(afterBlockStart, moreLinesIter)
			extensionsFrom(Vector(beforeBlock)) -> Some(block)
		}
		// Case: Instance block may start on a later line (after extension lines) or there may not be a code block
		else
		{
			// Looks for the block start, but terminates on a named declaration
			// Collects the in-between lines as extension lines
			val extensionLinesBuilder = new VectorBuilder[String]()
			openLine.foreach { extensionLinesBuilder += _ }
			var afterBlockPart: Option[String] = None
			while (afterBlockPart.isEmpty && moreLinesIter.hasNext &&
				!namedDeclarationStartRegex.existsIn(moreLinesIter.poll))
			{
				// Case: Extension and/or block start line found
				val line = moreLinesIter.next()
				line.optionIndexOf("{") match
				{
					// Case: Block starts on this line
					case Some(blockStartIndex) =>
						if (blockStartIndex > 0)
							extensionLinesBuilder += line.take(blockStartIndex)
						afterBlockPart = Some(line.drop(blockStartIndex + 1))
					// Case: This line is only extensions, still
					case None => extensionLinesBuilder += line
				}
			}
			val extensions = extensionsFrom(extensionLinesBuilder.result())
			afterBlockPart match
			{
				// Case: Block was found
				case Some(afterBlockStart) =>
					val (block, _) = readBlock(afterBlockStart, moreLinesIter)
					extensions -> Some(block)
				// Case: Instance didn't contain a code block
				case None => extensions -> None
			}
		}
	}
	
	private def extensionsFrom(lines: Vector[String]) =
	{
		// Finds the line that contains the extends -keyword
		lines.indexWhereOption { line => extendsRegex.existsIn(line) || line.startsWith("extends ") ||
			line.endsWith(" extends") } match
		{
			// Case: Extends -keyword found => Parses the extensions from the remaining line part + remaining lines
			case Some(firstLineIndex) =>
				val firstLineRemain = lines(firstLineIndex).afterFirst("extends")
				// Individual extensions are separated with a " with "
				withRegex.split((firstLineRemain +: lines.drop(firstLineIndex + 1)).mkString(" "))
					.map { _.trim }.toVector
			// Case: No extends -keyword found => no extensions
			case None => Vector()
		}
	}
	
	private def explicitTypeFrom(string: String) =
		if (string.startsWith(":")) Some(string.drop(1).trim) else None
	
	private def readParameterLists(listStartLine: String,
	                               remainingLinesIter: PollingIterator[String]): (Vector[Vector[String]], Option[String]) =
	{
		val listsBuilder = new VectorBuilder[Vector[String]]()
		// Code that appears after the initial parameter lists will be stored here
		var codeAfter: Option[String] = None
		// Checks whether the first line starts a parameter list
		if (listStartLine.startsWith("("))
		{
			val (list, remainingAfter) = readBlockLike(listStartLine.drop(1), remainingLinesIter,
				'(', ')')
			listsBuilder += list
			// Collects lists while the remaining part keeps initiating new lists
			codeAfter = remainingAfter
			while (codeAfter.exists { _.startsWith("(") })
			{
				val (list, remainingAfter) = readBlockLike(codeAfter.get.drop(1), remainingLinesIter,
					'(', ')')
				listsBuilder += list
				codeAfter = remainingAfter
			}
		}
		// May continue to look for lists from the next lines
		if (codeAfter.nonEmpty)
			listsBuilder.result() -> codeAfter
		else
			remainingLinesIter.pollOption match
			{
				case Some(nextLine) =>
					val nextLineWithoutIndents = nextLine.dropWhile { c => c == '\t' || c == ' ' }
					// Case: Next line opens a new parameter list => processes that and the potential lists after
					if (nextLineWithoutIndents.startsWith("("))
					{
						remainingLinesIter.skip()
						val (nextLists, remaining) = readParameterLists(nextLineWithoutIndents, remainingLinesIter)
						(listsBuilder.result() ++ nextLists) -> remaining
					}
					// Case: Next line doesn't open a new list => leaves it as it is
					else
						listsBuilder.result() -> None
				// Case: There are no more lines left
				case None => listsBuilder.result() -> None
			}
	}
	
	// Block start line must have the initiating { -char removed
	private def readBlock(blockStartLine: String, remainingLinesIter: Iterator[String]) =
	{
		val (blockLines, after) = readBlockLike(blockStartLine, remainingLinesIter, '{', '}')
		ReadCodeBlock(blockLines) -> after
	}
	
	// Block start line must have the initiating opening character removed
	private def readBlockLike(blockStartLine: String, remainingLinesIter: Iterator[String],
	                          openChar: Char, closeChar: Char) =
	{
		val blockLinesBuilder = new VectorBuilder[String]()
		// Opens the first line
		var openBlockCount = 1 + blockStartLine.count { _ == openChar } - blockStartLine.count { _ == closeChar }
		var lastLine = blockStartLine
		// Adds multiple lines if necessary
		while (openBlockCount > 0 && remainingLinesIter.hasNext)
		{
			blockLinesBuilder += lastLine
			lastLine = remainingLinesIter.next()
			openBlockCount = openBlockCount + lastLine.count { _ == openChar } - lastLine.count { _ == closeChar }
		}
		// Handles the last line, possibly splitting it to two parts
		val (lastLineBlockPart, afterPart) = separateBlockClosuresFrom(blockStartLine, -openBlockCount, closeChar)
		blockLinesBuilder += lastLineBlockPart
		// Returns all lines plus the part after the last closure
		blockLinesBuilder.result() -> Some(afterPart.trim).filter { _.nonEmpty }
	}
	
	// Returns part before the last included closure (without closing character) +
	// part after last included closure, including 'closuresToSeparate' closing characters
	private def separateBlockClosuresFrom(line: String, closuresToSeparate: Int, closeChar: Char) =
	{
		val splitIndex = {
			if (closuresToSeparate <= 0)
				line.lastIndexOf(closeChar)
			else
			{
				// Closure indices from right to left
				val closureIndices = line.indexOfIterator(closeChar.toString).toVector.reverse
				closureIndices(closuresToSeparate)
			}
		}
		line.substring(0, splitIndex) -> line.substring(splitIndex + 1)
	}
}

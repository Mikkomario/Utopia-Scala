package utopia.vault.coder.controller.reader

import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.IterateLines
import utopia.vault.coder.model.scala.Package

import java.nio.file.Path

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
	private val declarationModifierRegex = (Regex("override ") || Regex("protected ") || Regex("private ") ||
		Regex("lazy ") || Regex("implicit ")).withinParenthesis
	private val declarationStartRegex = declarationModifierRegex.zeroOrMoreTimes +
		(Regex("val ") || Regex("def ")).withinParenthesis
	private val parameterPartRegex = (declarationStartRegex + Regex.word + Regex.escape(':') + Regex.any)
		.withinParenthesis
	private val parameterListRegex = Regex.escape('(') +
		(parameterPartRegex + Regex.escape(',') + Regex.whiteSpace.noneOrOnce).withinParenthesis.zeroOrMoreTimes +
		parameterPartRegex.noneOrOnce + Regex.escape(')')
	private val withRegex = Regex(" with ")
	
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
}

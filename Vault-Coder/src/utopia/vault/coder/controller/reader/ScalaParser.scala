package utopia.vault.coder.controller.reader

import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.{IterateLines, LinesFrom}
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
		}
	}
}

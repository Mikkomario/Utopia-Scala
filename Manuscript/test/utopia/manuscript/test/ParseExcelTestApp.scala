package utopia.manuscript.test

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.manuscript.excel.{Excel, SheetTarget, UnallocatedHeaders}

import java.nio.file.Path
import scala.io.StdIn

/**
  * A test application for parsing Excel files
  * @author Mikko Hilpinen
  * @since 21.11.2024, v2.2.5
  */
object ParseExcelTestApp extends App
{
	// ATTRIBUTES   --------------------
	
	private implicit val log: Logger = SysErrLogger
	
	
	// APP CODE ------------------------
	
	// Finds the file to parse
	locateFile().foreach { path =>
		Excel.open(path) { excel =>
			requestSheetTarget().foreach { target =>
				excel(target).log.foreach { sheet =>
					requestHeaders().foreach { headers =>
						val models = sheet.modelsIteratorCompletingHeaders(UnallocatedHeaders.from(headers),
							preLoadModels = true)
						if (models.hasNext) {
							models.forNext(5)(println)
							if (models.hasNext)
								println(s"... (${ models.size } more rows)")
						}
						else
							println("Couldn't locate any headers and/or rows")
					}
				}
			}
		}
	}
	
	
	// OTHER    ------------------------
	
	private def locateFile() = {
		StdIn.readNonEmptyLine("Please specify a path to the file to read").flatMap { pathStr =>
			val path = pathStr: Path
			if (path.exists) {
				val fileType = path.fileType
				if (Excel.supportedExtensions.exists { _ ~== fileType })
					Some(path)
				else {
					fileType.ifNotEmpty match {
						case Some(t) => println(s"File type \"$t\" can't be opened with this application")
						case None => println("This type of file / directory can't be opened with this application")
					}
					None
				}
			}
			else {
				println(s"No file exists at: ${ path.toAbsolutePath }")
				None
			}
		}
	}
	
	private def requestSheetTarget() = {
		StdIn.readNonEmptyLine("Please specify the sheet to open. You can write part of the sheet's name, or its index.")
			.map { str =>
				str.int match {
					case Some(index) => SheetTarget.index(index)
					case None => SheetTarget.containing(str)
				}
			}
	}
	
	private def requestHeaders() = {
		StdIn.readNonEmptyLine("Please specify one or more headers that appear in the file. Separate the headers with ;")
			.map { input => OptimizedIndexedSeq.from(input.split(';')).map { _.trim } }
	}
}

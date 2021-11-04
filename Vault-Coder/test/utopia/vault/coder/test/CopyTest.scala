package utopia.vault.coder.test

import utopia.flow.generic.DataType
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.reader.ScalaParser

import java.nio.file.Path
import scala.io.{Codec, StdIn}

/**
  * Tests copying a parsed scala file
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  */
object CopyTest extends App
{
	// Metropolis\src\utopia\metropolis\model\stored\organization\Organization.scala
	// Citadel\src\utopia\citadel\database\access\many\organization\ManyMemberRoleLinksAccess.scala
	// Citadel\src\utopia\citadel\database\CitadelTables.scala
	
	DataType.setup()
	implicit val codec: Codec = Codec.UTF8
	
	val path: Path = args.headOption match {
		case Some(arg) => arg
		case None =>
			StdIn.printAndReadLine(
				s"Please write path to the targeted file. Current directory = ${"".toAbsolutePath}")
	}
	if (path.fileType != "scala")
		println("Please try again and specify a .scala file")
	else if (path.notExists)
		println(s"Specified file ${path.toAbsolutePath} doesn't exist")
	else
	{
		println(s"Parsing scala code from ${path.toAbsolutePath}...")
		val file = ScalaParser(path).get
		println(s"File read.")
		println(s"Read ${file.declarations.size} instance declarations")
		val defaultPath = path.withMappedFileName { _.untilLast(".") + "-copy.scala" }
		val outputPath = StdIn
			.readNonEmptyLine(s"Where do you want to save the file copy? Default = $defaultPath") match {
			case Some(input) => input: Path
			case None => defaultPath
		}
		outputPath.createParentDirectories().get
		file.writeTo(outputPath)
	}
}

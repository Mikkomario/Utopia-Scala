package utopia.flow.test.file

import utopia.flow.parse.file.FileExtensions._

import java.nio.file.Path
import scala.io.StdIn

/**
 * A simple test application for listing files in a directory
 *
 * @author Mikko Hilpinen
 * @since 06.05.2024, v2.4
 */
object ListFilesApp extends App
{
	println("Specify the targeted path")
	private val dir: Path = StdIn.readLine()
	private val children = dir.children.get
	children.foreach { file => println(file.fileName) }
}

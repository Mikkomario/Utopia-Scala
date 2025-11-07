package utopia.flow.test.file

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Lines

import java.nio.file.Path
import scala.io.Codec

/**
 * Tests file writing & reading
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.8
 */
object FileWriteTest extends App
{
	private implicit val codec: Codec = Codec.UTF8
	private val path: Path = "Flow/data/test-material/test-out.txt"
	
	path
		.writeUsing { writer =>
			writer.println("Line 1/2")
			writer.println("Line 2/2")
		}
		.get
	
	val readLines = Lines.from.path(path).get
	
	assert(readLines.size == 2)
	assert(readLines.head == "Line 1/2")
	assert(readLines(1) == "Line 2/2")
	
	println("Success")
}

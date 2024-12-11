package utopia.flow.test.file

import utopia.flow.test.TestContext._
import utopia.flow.parse.StreamExtensions._
import utopia.flow.parse.string.StringFrom

import java.io.FileInputStream
import scala.io.Codec

/**
  * Tests file reading
  * @author Mikko Hilpinen
  * @since 06.12.2024, v2.5.1
  */
object ReadFileTest extends App
{
	implicit private val codec: Codec = Codec.UTF8
	
	println("File contents:")
	println(StringFrom.stream(new FileInputStream("Flow/data/test-material/test.txt")).get)
	
	println("\nUsing .notEmpty:")
	new FileInputStream("Flow/data/test-material/test.txt").notEmpty match {
		case Some(stream) => println(StringFrom.stream(stream).get)
		case None => println("Empty stream")
	}
}

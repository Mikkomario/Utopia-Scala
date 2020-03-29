package utopia.flow.test

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ThreadPool
import utopia.flow.filesearch.Guild
import utopia.flow.generic.DataType

import scala.concurrent.ExecutionContext

/**
 * Tests the file search algorithm
 * @author Mikko
 * @since 6.1.2020, v1.6.1
 */
object FileSearchTest extends App
{
	DataType.setup()
	implicit val exc: ExecutionContext = new ThreadPool("File Search Test").executionContext
	val resultFuture = Guild.explore(".") { dir => dir.children.flatMap { _.filter { _.isRegularFile }
		.tryMap { _.size }.map { _.sum / 1000 } } }
	
	println("Calculates number of regular files in each directory")
	println(resultFuture.waitFor())
}

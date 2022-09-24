package utopia.flow.test.file

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.file.search.Guild
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
 * Tests the file search algorithm
 * @author Mikko
 * @since 6.1.2020, v1.6.1
 */
object FileSearchTest extends App
{
	DataType.setup()
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("File Search Test").executionContext
	val resultFuture = Guild.explore(".") { dir =>
		dir.children.flatMap {
			_.filter { _.isRegularFile }
				.tryMap { _.size }.map { _.sum / 1000 }
		}
	}
	
	println("Calculates number of regular files in each directory")
	println(resultFuture.waitFor())
}

package utopia.flow.filesearch

import java.nio.file.Path

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
 * Used for searching for files or directories in a file system
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
class Miner[R](origin: Mine[R], startingPath: Vector[Mine[R]] = Vector())(private val search: Path => R)
	extends Explorer(origin, startingPath)
{
	// IMPLEMENTED	---------------------
	
	override def explore()(implicit exc: ExecutionContext) = Future { exploreSync() }
	
	
	// OTHER	------------------------
	
	/**
	 * Mines the current location (asynchronously), then continues exploring normally
	 * @param exc Implicit execution context
	 * @return Asynchronous completion of the exploration
	 */
	def mineCurrentLocationThenExplore()(implicit exc: ExecutionContext) =
	{
		currentLocation.declareStarted()
		Future { mine() }.andThen { case Success(_) => exploreSync() }
	}
	
	private def exploreSync() =
	{
		// Traverses forward until a suitable dead-end is found
		while (findDeadEnd())
		{
			// Mines the found location
			currentLocation.declareStarted()
			mine()
		}
		
		// Returns once the whole passage has been completed
	}
	
	private def mine() =
	{
		// Either checks the directory itself or the files under the directory
		val result = search(currentLocation.directory)
		currentLocation.declareCompleted(result)
	}
}

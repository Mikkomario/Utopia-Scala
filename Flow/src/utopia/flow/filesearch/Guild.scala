package utopia.flow.filesearch

import java.nio.file.Path

import scala.concurrent.ExecutionContext

/**
 * Used for organizing search over a file system, transforming directories to tree-like data structures,
 * recursively and asynchronously
 * @author Mikko Hilpinen
 * @since 6.1.2020, v1.6.1
 */
object Guild
{
	/**
	 * Explores a file system and returns mapped results in a tree-like format
	 * @param directory Origin directory that will be searched. All directories under this one will also be searched.
	 * @param numberOfMiners Number of miners to send to perform the search. Correlates with the number of threads
	 *                       operating on the search algorithm. The actual number of threads will be larger but the
	 *                       number of threads performing 'resultsFromDirectory' calls will be equal to this parameter.
	 *                       Default = 4.
	 * @param resultsFromDirectory A function that produces a result from a directory. Won't be called for regular files,
	 *                             only directories.
	 * @param exc Implicit execution context
	 * @tparam R Type of result produced for each directory
	 * @return A future for the results, which are categorized into a tree that matches the structure of the file system.
	 */
	def explore[R](directory: Path, numberOfMiners: Int = 4)(resultsFromDirectory: Path => R)(implicit exc: ExecutionContext) =
	{
		val mine = new Mine[R](directory)
		val entourage = new Entourage[R](mine, numberOfMiners)(resultsFromDirectory)
		entourage.explore()
		mine.futureResults
	}
}

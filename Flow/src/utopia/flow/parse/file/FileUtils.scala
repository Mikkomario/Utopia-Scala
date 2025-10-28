package utopia.flow.parse.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair}
import utopia.flow.parse.file.FileConflictResolution.Overwrite
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

import java.nio.file.{Path, Paths}
import scala.util.{Failure, Success}

/**
 * Utility functions related to files / paths
 * @author Mikko Hilpinen
 * @since 18.10.2023, v2.3
 */
object FileUtils
{
	// ATTRIBUTES   --------------------
	
	private lazy val dashRegex = Regex.escape('-')
	private lazy val multiDashRegex = dashRegex + dashRegex.oneOrMoreTimes
	private lazy val validFileNamePartRegex = (Regex.letterOrDigit || dashRegex).withinParentheses.oneOrMoreTimes
	
	/**
	 * The current working directory
	 */
	lazy val workingDirectory = Paths.get("")
	
	
	// OTHER    ------------------------
	
	/**
	 * Removes any special characters from a file name.
	 * Replaces all separators with '-'
	 * @param fileName Original file name
	 * @return Normalized file name
	 */
	def normalizeFileName(fileName: String) = {
		val (namePart, extensionPart) = fileName.splitAtLast(".").toTuple
		val normalizedName = validFileNamePartRegex.matchesIteratorFrom(namePart)
			.filter { _.nonEmpty }.mkString("-")
			.replaceEachMatchOf(multiDashRegex, "-")
			.notStartingWith("-").notEndingWith("-")
		s"$normalizedName${extensionPart.mapIfNotEmpty { ext => s".$ext" }}"
	}
	
	/**
	 * Moves 0-n files to a new directory
	 * @param pathsToMove Paths to move
	 * @param targetDirectory Directory where the files will be placed
	 * @param conflictResolution Logic for handling file conflicts (default = overwrite existing files)
	 * @param restoreOnFailure Whether to restore the original state on a failure
	 * @return Either:
	 *              - Right (if successful): The targeted paths in the new directory
	 *              - Left (if failure): The encountered failure, plus the paths as they are now located
	 */
	def moveFilesTo(pathsToMove: Seq[Path], targetDirectory: Path,
	                conflictResolution: FileConflictResolution = Overwrite, restoreOnFailure: Boolean = false) =
	{
		// Case: No files to move => Succeeds
		if (pathsToMove.isEmpty)
			Right(Empty)
		else {
			// Case: On a failure, the original state must be restored => Prepares to move the files back, if needed
			if (restoreOnFailure) {
				val moveResults = pathsToMove.iterator
					.map { p => p.moveTo(targetDirectory, conflictResolution).map { Pair(p, _) } }
					// Terminates moving if one of the actions fails
					.collectTo { _.isFailure }
				val movedPaths = moveResults.flatMap { _.toOption }
				
				moveResults.last match {
					// Case: Moving succeeded => Success
					case Success(_) => Right(movedPaths.view.map { _.second }.toOptimizedSeq)
					// Case: Failed to move some of the paths
					//       => Moves the already moved files back to their original positions
					case Failure(error) =>
						val restoredPaths =
							movedPaths.map { paths => paths.second.moveAs(paths.first).getOrElse(paths.second) }
						Left(error -> OptimizedIndexedSeq.concat(restoredPaths, pathsToMove.drop(restoredPaths.size)))
				}
			}
			// Case: No restoration needed => Moves files until a failure is encountered
			else {
				val moveResults = pathsToMove.iterator.map { _.moveTo(targetDirectory, conflictResolution) }
					.collectTo { _.isFailure }
				val movedPaths = moveResults.flatMap { _.toOption }
				
				moveResults.last match {
					// Case: Moving succeeded => Success
					case Success(_) => Right(movedPaths)
					// Case: Moving failed => Locates the paths and fails
					case Failure(error) =>
						Left(error -> OptimizedIndexedSeq.concat(movedPaths, pathsToMove.drop(movedPaths.size)))
				}
			}
		}
	}
}

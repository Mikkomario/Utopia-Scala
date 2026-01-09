package utopia.flow.parse.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.parse.file.FileConflictResolution.Overwrite
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.result.TryCatch

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
	def normalizeFileName(fileName: String) = mapFileNameWithoutExtension(fileName) { name =>
		if (name.startsWith("."))
			s".${ normalize(name.tail) }"
		else
			normalize(name)
	}
	
	/**
	 * Maps a file name, not modifying the file extension, if present
	 * @param fileName A file name, including the file extension, if applicable
	 * @param f A mapping function applied to the specified file's name part (excluding the extension)
	 * @return A copy of the specified file name, where the file name part has been modified using 'f',
	 *         keeping the extension (if applicable) the same.
	 */
	def mapFileNameWithoutExtension(fileName: String)(f: Mutate[String]) =
		mapFileOrDirectoryName(fileName)(f) { (fullName, splitIndex) =>
			val (namePart, extension) = fullName.splitAt(splitIndex)
			s"${ f(namePart) }$extension"
		}
	/**
	 * Maps a file name using a mapping function intended for directory names or file names separately
	 * @param fileName A file name to map
	 * @param ifDirectory A mapping function called if 'fileName' specifies no file extension
	 * @param ifFile A mapping function called if 'fileName' specifies a file extension.
	 *               Receives two parameters:
	 *                  1. The full file name
	 *                  1. Index of the '.' character separating the file name part from the file type.
	 * @tparam A Type of mapping results
	 * @return The result of either 'ifDirectory' or 'ifFile'
	 */
	def mapFileOrDirectoryName[A](fileName: String)(ifDirectory: String => A)(ifFile: (String, Int) => A) = {
		// Case: A very short file name => Can never be interpreted as a valid file name including a type
		if (fileName.length < 3)
			ifDirectory(fileName)
		else {
			// Looks for a '.' indicating a file extension
			// Ignores '.' at the beginning and at the end of the string
			fileName.findLastIndexOf(".", fileName.length - 2).filter { _ > 0 } match {
				// Case: Seemingly valid '.' found => Interprets as a regular file name
				case Some(separatorIndex) => ifFile(fileName, separatorIndex)
				// Case: '.' not found in the correct position => Interprets as a directory name
				case None => ifDirectory(fileName)
			}
		}
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
	
	/**
	 * Determines a directory for an application.
	 * By default, this is {user.home}/.{app name}.
	 * @param appName Name of the targeted application
	 *                (call-by-name, called if the user's home directory is resolved)
	 * @param allowWorkingDirectory Whether to allow the use of the current working directory as a backup,
	 *                              if the user's home directory can't be resolved.
	 *                              Default = false.
	 * @return If the user's home directory was resolved, yields {user.home}/.{app name};
	 *         Otherwise yields the current working directory, or a failure if 'allowWorkingDirectory' was set to false.
	 *         Also yields a failure if failed to create the app directory under the user's home directory.
	 */
	def appDirectory(appName: => String, allowWorkingDirectory: Boolean = false) =
		Option(System.getProperty("user.home")) match {
			case Some(home) =>
				// Determines the app directory under home
				val dir = Paths.get(s"$home/.${ normalize(appName) }")
				// Creates the app directory, if necessary
				if (dir.notExists)
					dir.createDirectories().map { dir =>
						// Hides this directory. If hiding fails, ignores the error.
						dir.hide().getOrElse(dir)
					}
				else
					Success(dir)
			
			// Case: The home directory can't be resolved => Uses the current working directory, if allowed
			case None =>
				if (allowWorkingDirectory)
					Success(workingDirectory)
				else
					Failure(new NoSuchElementException("System property user.home is undefined"))
		}
	
	/**
	 * Creates or accesses a configuration file for a specific application.
	 * The configuration file is placed, by default, under the app's directory under the user home directory.
	 * If a new file is created, access to it is restricted to the current user only (if possible).
	 * @param appName Name of the targeted application (call-by-name).
	 * @param fileName Name of the targeted configuration file. Default = config.conf.
	 * @param allowWorkingDirectory Whether to allow the use of the current working directory as a backup,
	 *                              if no app directory could be created under the user's home directory.
	 *                              Default = false.
	 * @return Path to an existing configuration file.
	 *         May yield a failure. Yields partial failures, if file-restriction or hiding failed.
	 */
	def appConfigFile(appName: => String, fileName: => String = "config.conf",
	                  allowWorkingDirectory: Boolean = false): TryCatch[Path] =
	{
		// Resolves the application directory
		appDirectory(appName) match {
			// Case: Application directory resolved => Resolves the config file path and creates it, if necessary
			case Success(appDir) =>
				val path = appDir / fileName
				if (path.notExists)
					path.restrictAccess(allowCreation = true) match {
						case Success(path) => TryCatch.Success(path)
						case Failure(error) => TryCatch.Success(path, Single(error))
					}
				else
					TryCatch.Success(path)
			
			// Case: App directory couldn't be created => Defaults to the current working directory, if allowed
			case Failure(error) =>
				// Case: Working directory allowed => Uses a modified file name and a hidden file
				if (allowWorkingDirectory) {
					var path = workingDirectory /
						mapFileNameWithoutExtension(fileName) { name => normalize(s"$appName-$name") }.startingWith(".")
					
					// Case: A new file needs to be created
					//       => Attempts to restrict access to the config file, and to hide it
					if (path.notExists) {
						// Catches encountered errors
						val failuresBuilder = OptimizedIndexedSeq.newBuilder[Throwable]
						failuresBuilder += error
						
						path = path.restrictAccess(allowCreation = true).getOrMap { error =>
							failuresBuilder += error
							path
						}
						path = path.hide().getOrMap { error =>
							failuresBuilder += error
							path
						}
						
						TryCatch.Success(path, failuresBuilder.result())
					}
					// Case: A file already existed => Yields that
					else
						TryCatch.Success(path, Single(error))
				}
				// Case: Use of the working directory is not acceptable => Fails
				else
					TryCatch.Failure(error)
		}
	}
	
	/**
	 * Normalizes a file name
	 * @param fileName A file name's name part, not including any possible extension
	 * @return A normalized copy of the specified name
	 */
	private def normalize(fileName: String) =
		validFileNamePartRegex.matchesIteratorFrom(fileName).filter { _.nonEmpty }.mkString("-")
			.replaceEachMatchOf(multiDashRegex, "-").notStartingWith("-").notEndingWith("-")
}

package utopia.flow.util

import java.awt.Desktop
import java.io.{BufferedOutputStream, FileInputStream, FileNotFoundException, FileOutputStream, IOException}
import java.nio.file.{DirectoryNotEmptyException, Files, Path, Paths, StandardOpenOption}

import utopia.flow.parse.JSONConvertible

import scala.language.implicitConversions
import utopia.flow.util.StringExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.AutoClose._
import utopia.flow.util.NullSafe._

import scala.io.Codec
import scala.util.{Failure, Success, Try}

/**
 * Provides some extensions to be used with java.nio.file classes
 * @author Mikko Hilpinen
 * @since 17.11.2019, v1+
 */
object FileExtensions
{
	/**
	 * Converts a string to a path
	 */
	implicit def stringToPath(pathString: String): Path = Paths.get(pathString)
	
	implicit class RichPath(val p: Path) extends AnyVal
	{
		/**
		 * @return Whether this file exists in the file system (false if undetermined)
		 */
		def exists = Files.exists(p)
		
		/**
		 * @return Whether this file doesn't exist in the file system (false if undetermined)
		 */
		def notExists = Files.notExists(p)
		
		/**
		 * @return File name portion of this path
		 */
		def fileName = p.getFileName.toOption.map { _.toString }.getOrElse("")
		
		/**
		 * @return The last modified time of this file (may fail). May not work properly for directories.
		 */
		def lastModified = Try { Files.getLastModifiedTime(p).toInstant }
		
		/**
		 * @return the type of this file (portion after last '.'). Returns an empty string for directories and
		 *         for files without type.
		 */
		def fileType = fileName.afterLast(".")
		
		/**
		 * @param another A sub-path
		 * @return This path extended with another path
		 */
		def /(another: Path) = p.resolve(another)
		
		/**
		 * @param another A sub-path
		 * @return This path extended with another path
		 */
		def /(another: String) = p.resolve(another)
		
		/**
		 * @return An absolute path based on this path (if this path is already absolute, returns this)
		 */
		def absolute = p.toAbsolutePath
		
		/**
		 * @return Whether this path represents an existing directory
		 */
		def isDirectory = Files.isDirectory(p)
		
		/**
		 * @return Whether this path represents an existing regular file (non-directory)
		 */
		def isRegularFile = Files.isRegularFile(p)
		
		/**
		 * @return This path as an existing directory. Fails if this is a regular file and not a directory.
		 */
		def asExistingDirectory =
		{
			if (notExists)
				createDirectories()
			else if (isRegularFile)
				Failure(new IOException(s"$p is not a directory"))
			else
				Success(p)
		}
		
		/**
		 * @return A parent path for this path. None if this path is already a root path
		 */
		def parentOption = p.getParent.toOption
		
		/**
		 * @return A parent path for this path. Return this path if already a root path
		 */
		def parent = parentOption.getOrElse(p)
		
		/**
		 * @return All children (files and directories) directly under this directory (empty vector if not directory). May fail.
		 */
		def children =
		{
			// Non-directory paths don't have children
			if (isDirectory)
				Try { Files.list(p).consume { _.collect(new VectorCollector[Path]) } }
			else
				Success(Vector())
		}
		
		/**
		 * @return Directories directly under this one (returns empty vector for regular files). May fail.
		 */
		def subDirectories =
		{
			if (isDirectory)
				Try { Files.list(p).consume { _.filter { p => p.isDirectory }.collect(new VectorCollector[Path]) } }
			else
				Success(Vector())
		}
		
		/**
		 * @return The size of this file in bytes. If called for a directory, returns the combined size of all files and
		 *         directories under this directory. Please note that this method may take a while to complete.
		 */
		def size: Try[Long] =
		{
			// Size of a regular file is delegated to java.nio.Files while size of a directory is calculated recursively
			if (isRegularFile)
				Try { Files.size(p) }
			else
				children.flatMap { _.tryMap { _.size }.map { _.sum } }
		}
		
		/**
		 * @param newFileName New file name
		 * @return A copy of this path with specified file name
		 */
		def withFileName(newFileName: String) =
		{
			if (fileName == newFileName)
				p
			else
				parentOption.map { _/newFileName }.getOrElse(newFileName: Path)
		}
		
		/**
		 * Performs an operation on all files directly under this path
		 * @param filter A filter applied to child paths (default = no filter)
		 * @param operation Operation performed for each path
		 * @return A try that may contain a failure if this operation failed
		 */
		def forChildren(filter: Path => Boolean = _ => true)(operation: Path => Unit): Try[Unit] =
		{
			if (isDirectory)
				Try { Files.list(p).consume { _.filter(p => filter(p)).forEach(p => operation(p)) } }
			else
				Success(())
		}
		
		/**
		 * Merges values of child paths into a single value
		 * @param start Starting value
		 * @param filter A filter applied to the childre (default = no filtering)
		 * @param f A folding function
		 * @tparam A Type of fold result
		 * @return Fold result. May contain a failure.
		 */
		def foldChildren[A](start: A, filter: Path => Boolean = _ => true)(f: (A, Path) => A) =
		{
			if (isDirectory)
			{
				Try { Files.list(p).consume { stream =>
					var result = start
					stream.filter(p => filter(p)).forEach(p => result = f(result, p))
					result
				} }
			}
			else
				Success(start)
		}
		
		/**
		 * Moves this file / directory to another directory
		 * @param targetDirectory Target parent directory for this file
		 * @param replaceIfExists Whether a file already existing at target path should be replaced with this one,
		 *                        if present (default = true)
		 * @return Link to the target path. Failure if file moving failed or if couldn't replace an existing file
		 */
		def moveTo(targetDirectory: Path, replaceIfExists: Boolean = true): Try[Path] =
		{
			// Might not need to move at all
			if (parentOption.contains(targetDirectory))
				Success(p)
			else if (notExists)
				Failure(new FileNotFoundException(s"Cannot move $p because it doesn't exist"))
			else
			{
				// Directories with content will have to be first copied, then removed
				if (isDirectory)
					copyTo(targetDirectory, replaceIfExists).flatMap { newDir => delete().map { _ => newDir } }
				else
				{
					// May need to create target directory if it doesn't exist yet
					targetDirectory.asExistingDirectory.flatMap { dir =>
						// May need to delete an existing file / directory
						val newLocation = dir/fileName
						val emptyTarget =
						{
							if (newLocation.exists)
							{
								if (replaceIfExists)
									newLocation.delete().map { _ => newLocation}
								else
									Failure(new IOException(
										s"Cannot move $p to $targetDirectory because $newLocation already exists and overwrite is disabled"))
							}
							else
								Success(newLocation)
						}
						emptyTarget.flatMap { target => Try { Files.move(p, target) } }
					}
				}
			}
		}
		
		/**
		 * Copies this file / directory to another directory
		 * @param targetDirectory Target parent directory for this file
		 * @param replaceIfExists Whether a file already existing at target path should be replaced with this one,
		 *                        if present (default = true)
		 * @return Link to the target path. Failure if file moving failed or if couldn't replace an existing file
		 */
		def copyTo(targetDirectory: Path, replaceIfExists: Boolean = true) = copyAs(targetDirectory/fileName, replaceIfExists)
		
		/**
		 * Copies this file / directory to a new location (over specified path)
		 * @param targetPath Location, including file name, for the new copy
		 * @param allowReplace Whether a file already existing at target path should be replaced with this one,
		 *                        if present (default = true)
		 * @return Link to the target path. Failure if file moving failed or if couldn't replace an existing file
		 */
		def copyAs(targetPath: Path, allowReplace: Boolean = true) =
		{
			// May not need to perform any copy
			if (targetPath == p)
				Success(p)
			else if (notExists)
				Failure(new FileNotFoundException(s"$p cannot be copied over $targetPath because $p doesn't exist"))
			else
			{
				// If the target path already exists, it may need to deleted first, if not, parent directories may
				// need to be created
				val prepareResult =
				{
					if (targetPath.exists)
					{
						if (allowReplace)
							targetPath.delete().map { _ => targetPath }
						else
							Failure(new IOException(
								s"Cannot copy $p over $targetPath because $targetPath already exists and overwrite is disabled"))
					}
					else
						targetPath.createParentDirectories()
				}
				
				// May need to create parent directories
				prepareResult.flatMap { target => recursiveCopyAs(target) }
			}
		}
		
		private def recursiveCopyTo(targetDirectory: Path): Try[Path] = recursiveCopyAs(targetDirectory/fileName)
		
		// First copies the file / directory, then the children files, if there are any
		private def recursiveCopyAs(newPath: Path): Try[Path] =
		{
			// May need to delete the existing file first
			newPath.delete().flatMap { _ => Try { Files.copy(p, newPath) } }.flatMap { newParent =>
				children.flatMap { _.tryForEach { c => new RichPath(c).recursiveCopyTo(newParent) } }.map { _ => newParent }}
		}
		/**
		 * Renames this file or directory
		 * @param newFileName New name for this file or directory (just file name, not the full path)
		 * @param allowOverwrite Whether renaming could overwrite another existing file (default = false). If this is
		 *                       false, fails when trying to rename over an existing file.
		 * @return Path to the newly named file. Failure if renaming failed.
		 */
		def rename(newFileName: String, allowOverwrite: Boolean = false) =
		{
			// Might not need to rename the file at all
			if (fileName == newFileName)
				Success(p)
			else
			{
				// Checks whether another path would be overwritten (only when allowOverwrite = false)
				val newPath: Path = p.parentOption.map { _/newFileName }.getOrElse(newFileName)
				if (!allowOverwrite && newPath.exists)
					Failure(new IOException(
						s"Cannot rename $p to $newFileName because such a file already exists and overwriting is disabled"))
				else if (notExists) // Paths to non-existing files are simply changed
					Success(newPath)
				else
					newPath.delete().flatMap { _ => Try { Files.move(p, newPath) } }
			}
		}
		
		/**
		 * Overwrites this path with file from another path
		 * @param anotherPath A path leading to the file that will overwrite this one
		 * @return Path to this file. May contain failure.
		 */
		def overwriteWith(anotherPath: Path) =
		{
			// May not need to do anything
			if (anotherPath == p)
				Success(p)
			// The target path must exist
			else if (anotherPath.notExists)
				Failure(new FileNotFoundException(s"Cannot overwrite $p with $anotherPath because $anotherPath doesn't exist"))
			// If both of the files are in the same directory, simply deletes this file
			else if (anotherPath.parentOption == parentOption)
				delete().map { _ => anotherPath }
			else
			{
				anotherPath.copyAs(p.withFileName(anotherPath.fileName)).flatMap { newFilePath =>
					// May need to delete this file / directory afterwards
					if (p == newFilePath)
						Success(newFilePath)
					else
						delete().map { _ => newFilePath }
				}
			}
		}
		
		/**
		 * Overwrites this path with file from another path, but only if the file was changed (had different last
		 * modified time). In case where directories are being overwritten, checks each file within the directories
		 * separately. In the end, this path will match the provided path.
		 * @param anotherPath Another file that will overwrite this one
		 * @return Path to this file. May contain a failure
		 */
		def overwriteWithIfChanged(anotherPath: Path): Try[Path] =
		{
			// May not need to do anything
			if (anotherPath == p)
				Success(p)
			else if (anotherPath.notExists)
				Failure(new FileNotFoundException(s"Cannot overwrite $p with $anotherPath because $anotherPath doesn't exist"))
			else if (notExists)
				overwriteWith(anotherPath)
			// Copying from directory to directory is handled recursively
			else if (isDirectory)
			{
				if (anotherPath.isDirectory)
				{
					children.flatMap { myChildren =>
						anotherPath.children.flatMap { newChildren =>
							val myChildrenByName = myChildren.map { c => c.fileName -> c }.toMap
							val newChildrenByName = newChildren.map { c => c.fileName -> c }.toMap
							val myChildNames = myChildrenByName.keySet
							val newChildNames = newChildrenByName.keySet
							
							// Files that didn't exists previously will be copied over
							(newChildNames -- myChildNames).tryMap { name =>
								newChildrenByName(name).copyTo(p) }.flatMap { _ =>
								// Files that already existed will be overwritten, if changed
								(newChildNames & myChildNames).tryMap { name => myChildrenByName(name)
									.overwriteWithIfChanged(newChildrenByName(name)) }.flatMap { _ =>
									// Files that existed but can't be found from new children will be removed
									(myChildNames -- newChildNames).tryMap { name => myChildrenByName(name).delete() }
								}
							}
						}
						// Renames this directory afterwards to match specified name
					}.flatMap { _ => rename(anotherPath.fileName) }
				}
				else
					overwriteWith(anotherPath)
			}
			else if (hasSameLastModifiedAs(anotherPath) && fileName == anotherPath.fileName)
				Success(p)
			else
				overwriteWith(anotherPath)
		}
		
		/**
		 * Deletes this file or directory
		 * @param allowDeletionOfDirectoryContents Whether deletion of a non-empty directory should be allowed
		 *                                         (resulting in deletion of all files under it) (default = true)
		 * @return Whether any files were deleted (false if this file didn't exist).
		 *         May contain a failure if some of the files couldn't be deleted.
		 */
		def delete(allowDeletionOfDirectoryContents: Boolean = true): Try[Boolean] =
		{
			if (notExists)
				Success(false)
			// In case of a directory, may need to clear contents first
			else if (isDirectory)
			{
				// If any of child deletion fails, the whole process is interrupted
				// Deletes this directory once the children have been removed
				if (allowDeletionOfDirectoryContents)
					children.flatMap { _.tryMap { _.delete() } }.flatMap { _ => Try { Files.deleteIfExists(p) }}
				else
					Failure(new DirectoryNotEmptyException(
						s"Targeted directory $p is not empty and recursive deletion is disabled"))
			}
			else
				Try { Files.deleteIfExists(p) }
		}
		
		/**
		 * Deletes all child paths from under this directory. Stops deletion if any deletion fails.
		 * @return Whether any files were deleted. May contain failure.
		 */
		def deleteChildren() = children.flatMap { _.tryMap { _.delete() } }.map { _.contains(true) }
		
		/**
		 * Creates this directory (and ensures existence of parent directories as well). If this is not a directory,
		 * simply creates parent directories.
		 * @return This path. Failure if couldn't create directories.
		 */
		def createDirectories() =
		{
			if (notExists)
			{
				// Checks whether this file should be a directory (doesn't have a file type) or a regular file
				// (has file type)
				if (fileType.isEmpty)
					Try { Files.createDirectories(p) }
				else
					createParentDirectories()
			}
			else
				Success(p)
		}
		
		/**
		 * Creates directories above this path. Eg. for path "dir1/dir2/fileX.txt" would ensure existence of dir1 and dir2
		 * @return This path, failure if couldn't create directories
		 */
		def createParentDirectories() = parentOption.map { dir => Try[Unit] { Files.createDirectories(dir) } }
			.getOrElse(Success(())).map { _ => p }
		
		/**
		 * @param another Another file
		 * @return Whether these two files have same last modified time
		 */
		def hasSameLastModifiedAs(another: Path): Boolean =
		{
			// Directories need to be handled a bit differently (files inside the directory may have changed)
			val directLastModifiedComparison = lastModified.success.exists { another.lastModified.success.contains }
			if (isDirectory && another.isDirectory)
			{
				if (directLastModifiedComparison)
				{
					children.getOrElse(Vector()).sortBy { _.fileName }
						.compareWith(another.children.getOrElse(Vector()).sortBy { _.fileName }) { (a, b) =>
							a.hasSameLastModifiedAs(b)
						}
				}
				else
					false
			}
			else
				directLastModifiedComparison
		}
		
		/**
		 * Writes specified text to this file (creates or empties file if necessary)
		 * @param text Text to be written to this file
		 * @param codec Charset / codec used (implicit)
		 * @return This path. Failure if writing failed.
		 */
		def write(text: String)(implicit codec: Codec) = Try { Files.write(p, text.getBytes(codec.charSet)) }
		
		/**
		 * Appends specified text to this file
		 * @param text Text to append to this file
		 * @param codec Charset / codec used (implicit)
		 * @return This path. Failure if writing failed.
		 */
		def append(text: String)(implicit codec: Codec) = Try { Files.write(p, text.getBytes(codec.charSet),
			StandardOpenOption.APPEND) }
		
		/**
		 * Writes a json-convertible instance to this file
		 * @param json A json-convertible instance that will produce contents of this file
		 * @return This path. Failure if writing failed.
		 */
		def writeJSON(json: JSONConvertible) = write(json.toJSON)(Codec.UTF8)
		
		/**
		 * Writes into this file with a function. An output stream is opened for the duration of the function.
		 * @param writer A writer function that uses an output stream (may throw)
		 * @return This path. Failure if writing function threw or stream couldn't be opened (Eg. trying to write to a file).
		 */
		def writeWith(writer: BufferedOutputStream => Unit) =
			Try { new FileOutputStream(p.toFile).consume { new BufferedOutputStream(_).consume(writer) } }.map { _ => p }
		
		/**
		 * Reads data from this file
		 * @param reader A function that reads this file's data stream (may throw)
		 * @tparam A Return type of the function
		 * @return Returned value or failure if stream couldn't be opened / read or the reader function threw.
		 */
		def readWith[A](reader: FileInputStream => A) = Try { new FileInputStream(p.toFile).consume(reader) }
		
		/**
		 * Reads data from this file
		 * @param reader A function that reads this file's data stream and returns a Try
		 * @tparam A Return type of the function
		 * @return Returned value or failure if stream couldn't be opened or a failure was returned.
		 */
		def tryReadWith[A](reader: FileInputStream => Try[A]) = readWith(reader).flatten
		
		/**
		 * Opens this file in the default desktop application. If this is a directory, opens it in resource manager
		 * @return Success of failure
		 */
		def openInDesktop() = performDesktopOperation { _.open(p.toFile) }
		
		/**
		 * Opens this file for editing in the default desktop application. If this is a directory, opens it in resource manager
		 * @return Success or failure
		 */
		def editInDesktop() = performDesktopOperation { d => if (isDirectory) d.open(p.toFile) else d.edit(p.toFile) }
		
		/**
		 * Prints this file using the default desktop application
		 * @return Success or failure
		 */
		def print() = performDesktopOperation { _.print(p.toFile) }
		
		/**
		 * Opens resource manager for this directory, or the directory containing this file
		 * @return Success or failure
		 */
		def openDirectory() = performDesktopOperation { d =>
			if (isDirectory) d.open(p.toFile) else d.open(parent.toFile) }
		
		/**
		 * Opens resource manager for this file's / directory's parent directory
		 * @return Success or failure
		 */
		def openFileLocation() = performDesktopOperation { _.open(parent.toFile) }
		
		private def performDesktopOperation(f: Desktop => Unit) =
		{
			if (Desktop.isDesktopSupported)
				Try { f(Desktop.getDesktop) }
			else
				Failure(new UnsupportedOperationException("Desktop is not supported on this platform"))
		}
	}
}

package utopia.flow.parse.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.LazyTree
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.mutable.iterator.{OptionsIterator, PollableOnce, UntilExternalFailureIterator}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.{ApproxEquals, EqualsFunction}
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileConflictResolution.Overwrite
import utopia.flow.parse.json.JsonConvertible
import utopia.flow.parse.string.IterateLines
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryCatch
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{CollectSingleFailureLogger, Logger}
import utopia.flow.view.immutable.caching.Lazy

import java.awt.Desktop
import java.io._
import java.nio.channels.FileChannel
import java.nio.file._
import scala.io.Codec
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Provides some extensions to be used with java.nio.file classes
  * @author Mikko Hilpinen
  * @since 17.11.2019, v1+
  */
object FileExtensions
{
	private lazy val hasSameLastModifiedEquals = EqualsFunction[Path] { _.hasSameLastModifiedAs(_) }
	
	/**
	  * Converts a string to a path
	  */
	implicit def stringToPath(pathString: String): Path = Paths.get(pathString.stripControlCharacters)
	
	implicit class RichPath(val p: Path) extends AnyVal with ApproxEquals[Path] with MaybeEmpty[Path]
	{
		// COMPUTED ----------------------------
		
		/**
		  * @return Names of the parts that form this path
		  */
		def parts = (0 until p.getNameCount).map { p.getName(_).toString }
		/**
		  * @return An iterator that returns the names of the parts that form this path
		  */
		def partsIterator = (0 until p.getNameCount).iterator.map { p.getName(_).toString }
		
		/**
		  * @return The length of this path, as a number of parts
		  */
		def length = p.getNameCount
		
		/**
		  * @return A json representation of this path (uses / as the directory separator)
		  */
		def toJson = p.toString.replace("\\", "/")
		
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
		def fileName = Option(p.getFileName).map { _.toString }.getOrElse("")
		/**
		  * @return File name portion of this path, without the extension portion (such as ".txt")
		  */
		def fileNameWithoutExtension = Option(p.getFileName) match {
			case Some(part) => part.toString.untilLast(".")
			case None => ""
		}
		/**
		  * @return The name and the extension of this file.
		 *         In case of directories, the extension is an empty string.
		  */
		def fileNameAndType = fileName.splitAtLast(".")
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
		  * @return True if this path is not absolute.
		  *         Absolute means that this path doesn't need to be combined with another path in order
		  *         to locate a file.
		  */
		def isNotAbsolute = !p.isAbsolute
		/**
		  * @return An absolute path based on this path (if this path is already absolute, returns this)
		  */
		def absolute = Try { p.toAbsolutePath }.getOrElse(p)
		/**
		  * @return A cleaned absolute version of this path, if possible to acquire.
		  *         If real path and absolute path formation are impossible, returns a normalized version of this path.
		  */
		def real = Try { p.toRealPath() }
			.orElse { Try { p.toAbsolutePath.normalize() } }
			.getOrElse { p.normalize() }
		
		/**
		  * @return Whether this path represents an existing directory
		  */
		def isDirectory = Files.isDirectory(p)
		/**
		  * @return Whether this path represents an existing regular file (non-directory)
		  */
		def isRegularFile = Files.isRegularFile(p)
		
		/**
		  * @return A parent path for this path. None if this path is already a root path.
		  */
		def parentOption = Option(p.getParent)
			.orElse {
				// Case: Absolute root path => None
				if (p.isAbsolute)
					None
				// Case: Relative path => Converts to an absolute path (if possible) and tries again
				else
					Try { p.toAbsolutePath }.toOption.flatMap { p => Option(p.getParent) }
			}
		/**
		  * @return A parent path for this path. Return this path if already a root path
		  */
		def parent = parentOption.getOrElse(p)
		/**
		  * @return An iterator that returns all parents of this path, from closest to furthest
		  */
		def parentsIterator = OptionsIterator.iterate(parentOption) { _.parentOption }
		
		/**
		  * @return All children (files and directories) directly under this directory
		  *         (empty vector if not directory). May fail.
		  */
		def children = iterateChildren { _.toVector }
		/**
		  * @return An iterator that accesses all child paths within this directory from top to bottom.
		  *         Terminates and yields a failure in case a file-read failure is encountered.
		  */
		def allChildrenIterator: Iterator[Try[Path]] = {
			implicit val log: CollectSingleFailureLogger = new CollectSingleFailureLogger()
			new UntilExternalFailureIterator(toTree.topDownNodesBelowIterator.map { _.nav }, log)
		}
		/**
		  * @return Directories directly under this one (returns empty vector for regular files). May fail.
		  */
		def subDirectories = iterateChildren { _.filter { _.isDirectory }.toVector }
		/**
		  * @return An iterator that returns all sub-directories of this directory, their sub-directories and so-on.
		  *         Includes the regular files with each directory, also.
		  */
		def allSubDirectoriesIterator = {
			children match {
				case Success(children) =>
					children.iterator.filter { _.isDirectory }.flatMap { new RecursiveDirectoriesIterator(_) }
				case Failure(error) => PollableOnce(Failure(error))
			}
		}
		/**
		  * @return An iterator that returns this directory, all sub-directories under this directory,
		  *         all their sub-directories, and so on.
		  *         The regular files under each directory are also included.
		  */
		def allDirectoriesIterator = {
			if (isDirectory)
				new RecursiveDirectoriesIterator(p)
			else
				Iterator.empty
		}
		/**
		  * @return All non-directory files in this directory and its sub-directories
		  */
		def allRegularFileChildren = findDescendants { _.isRegularFile }
		
		/**
		  * @return This path as an existing directory (creating the directory if possible & necessary).
		  *         Fails if this is a regular file and not a directory.
		  */
		def asExistingDirectory = {
			if (notExists)
				createDirectories()
			else if (isRegularFile)
				Failure(new IOException(s"$p is not a directory"))
			else
				Success(p)
		}
		
		/**
		  * @return This path in case it doesn't exist yet. Otherwise another non-existing path similar name
		  *         in the same directory.
		  */
		def unique = {
			// Case: This path doesn't exist yet => Can't conflict
			if (notExists)
				p
			// Case: This path exists => Has to generate a new name
			else {
				val myName = fileName
				// Finds similar file names that are already being used
				val competitorNames = iterateSiblings { _.map { _.fileName }.filter { _.startsWith(myName) }.toSet }
					.getOrElse(Set())
				// Checks which character to use to separate the index from the main file name part
				val separatorChar = {
					if (myName.contains('-'))
						'-'
					else if (myName.contains('_'))
						'_'
					else if (myName.containsMany("."))
						'.'
					else if (myName.contains(' '))
						' '
					else
						'-'
				}
				val (myNameBeginning, myExtension) = myName.splitAtLast(".").toTuple
				val myFullExtension = if (myExtension.isEmpty) myExtension else s".$myExtension"
				// Generates new names until one is found which isn't a duplicate
				val newName = Iterator.iterate(2) { _ + 1 }
					.map { index => s"$myNameBeginning$separatorChar$index$myFullExtension" }
					.find { !competitorNames.contains(_) }.get
				withFileName(newName)
			}
		}
		
		/**
		  * @return The size of this file in bytes. If called for a directory, returns the combined size of all files and
		  *         directories under this directory. Please note that this method may take a while to complete.
		  */
		def size: Try[Long] = {
			// Size of a regular file is delegated to java.nio.Files while size of a directory is calculated recursively
			if (isRegularFile)
				Try { Files.size(p) }
			else {
				// Iterates through the children, calculating file sizes.
				// Terminates the process if any failure is found.
				val iterator = allChildrenIterator
				var total = 0L
				var failure: Option[Throwable] = None
				while (iterator.hasNext && failure.isEmpty) {
					iterator.next() match {
						case Success(path) =>
							if (path.isRegularFile)
								Try { Files.size(path) } match {
									case Success(size) => total += size
									case Failure(error) => failure = Some(error)
								}
						case Failure(error) => failure = Some(error)
					}
				}
				failure match {
					case Some(error) => Failure(error)
					case None => Success(total)
				}
			}
		}
		
		/**
		  * Lazily converts this path (and its sub-directories and files) into a tree structure
		  * @param log A logger to use to log possible failures to scan for directory children
		  * @return A lazily initialized tree structure based on this path,
		  *         where each node represents a file or a directory.
		  */
		def toTree(implicit log: Logger): LazyTree[Path] = {
			// Case: Regular file => Doesn't need any scanning
			if (isRegularFile)
				LazyTree.initializedEmpty(p)
			// Case: Directory => lazily reads the directory contents when going downwards
			else
				LazyTree.iterate[Path](Lazy.initialized(p)) { path =>
					// Case: Directory => lazily scans further
					if (path.isDirectory)
						path.children match {
							// Case: Directory scanning succeeded => opens a new layer of nodes
							case Success(children) => children.map(Lazy.initialized)
							// Case: Directory reading failed => logs as an error and acts as if the directory was empty
							case Failure(error) =>
								log(error, s"Failed to read the children of $path")
								Empty
						}
					// Case: Regular file => No need to scan for child nodes
					else
						Empty
				}
		}
		
		
		// IMPLEMENTED  ---------------------------
		
		override def self: Path = p
		
		override def isEmpty: Boolean = {
			// Case: Directory => Checks whether this directory contains any files
			//                    Considered nonEmpty on file read failures
			if (isDirectory)
				iterateChildren { !_.hasNext }.getOrElse(false)
			// Case: Regular file => Existing regular files are never considered empty
			//                       Returns false on a read failure
			else
				!notExists
		}
		
		override def ~==(other: Path): Boolean =
			Try { Files.isSameFile(p, other) }.getOrElse { p.absolute.normalize() == other.absolute.normalize() }
		
		
		// OTHER    -------------------------------
		
		/**
		 * @param directory A directory
		 * @return Whether this path is a sub-path of the specified path
		 */
		def isChildOf(directory: Path) = relativeTo(directory).isRight
		
		/**
		  * @param another A sub-path
		  * @return This path extended with another path
		  */
		def /(another: Path) = if (another.partsIterator.forall { _ == "." }) p else p.resolve(another)
		/**
		  * @param another A sub-path
		  * @return This path extended with another path
		  */
		def /(another: String) = another match {
			case "." | "" => p
			case p2 => p.resolve(p2)
		}
		
		/**
		  * @param count Number of elements to take (from the root)
		  * @return A path containing at most 'count' elements, matching this path for its whole length
		  */
		def take(count: Int) =
			if (count >= p.getNameCount) p else if (count <= 0) Paths.get("") else p.subpath(0, count)
		/**
		  * @param n Number of elements to drop (from the root)
		  * @return A copy of this path without the first n parts.
		  *         I.e. a copy of this path that's relative to the nth element in this path.
		  */
		def drop(n: Int) = {
			if (n <= 0)
				p
			else {
				val l = length
				if (n >= l)
					Paths.get("")
				else
					p.subpath(n, l)
			}
		}
		/**
		  * @param count Number of elements to take (from the right)
		  * @return A copy of this path that's at most 'count' length, with the same last item
		  */
		def takeRight(count: Int) = {
			if (count <= 0)
				Paths.get("")
			else {
				val l = length
				if (count >= l)
					p
				else
					p.subpath(l - count, l)
			}
		}
		/**
		  * @param n Number of elements to drop from the right
		  * @return The nth parent of this path
		  */
		def dropRight(n: Int) = {
			if (n <= 0)
				p
			else {
				val l = length
				if (n >= l)
					Paths.get("")
				else
					p.subpath(0, l - n)
			}
		}
		
		/**
		  * Iterates over the children of this directory
		  * @param f A function that accepts an iterator that returns all paths that are the direct children of this directory.
		  *          Receives an empty iterator in case this is not an existing directory. The function may throw.
		  *          The errors thrown by the function are caught by this function.
		  * @tparam A Type of returned value
		  * @return The returned value. Failure if something threw during this operation.
		  */
		def iterateChildren[A](f: Iterator[Path] => A) = {
			if (isDirectory)
				Try { Files.list(p).consume { stream => f(stream.iterator().asScala) } }
			else
				Try { f(Iterator.empty) }
		}
		/**
		  * Iterates over the children of this directory
		  * @param f A function that accepts an iterator that returns all paths that are the children of this directory.
		  *          Receives an empty iterator in case this is not an existing directory.
		  *          Returns a success or a failure.
		  * @tparam A Type of returned value
		  * @return The returned value. Failure if something threw during this operation or if the specified
		  *         function returned a failure.
		  */
		def tryIterateChildren[A](f: Iterator[Path] => Try[A]) = iterateChildren(f).flatten
		/**
		 * Iterates over the children of this directory
		 * @param f A function that accepts an iterator that returns all paths that are the children of this directory.
		 *          Receives an empty iterator in case this is not an existing directory.
		 *          Returns a success or a failure. May catch exceptions.
		 * @tparam A Type of returned value
		 * @return The returned value with the possible included failures.
		 *         Failure if something threw during this operation or if the specified function returned a full failure.
		 */
		def tryIterateChildrenCatching[A](f: Iterator[Path] => TryCatch[A]) = iterateChildren(f).flattenCatching
		
		/**
		  * Iterates over the siblings of this file / path
		  * @param f A function that accepts an iterator that returns all siblings of this path
		  * @tparam A Type of returned value
		  * @return The returned value. Failure if something threw during this operation.
		  */
		def iterateSiblings[A](f: Iterator[Path] => A) = parentOption match {
			case Some(parent) => parent.iterateChildren { children => f(children.filterNot { _ == p }) }
			case None => Try { f(Iterator.empty) }
		}
		
		/**
		  * Seeks the lowest common parent with another path
		  * @param other Another path
		  * @return
		  *     1. The (root) path common to both of these paths, None if there is no common path
		  *     1. This path, relative to the common root path
		  *     1. The other path, relative to the common root path
		  */
		def commonParentWith(other: Path): (Option[Path], Path, Path) = {
			val (common, relative, otherRelative) = commonParentWith(Single(other))
			(common, relative, otherRelative.head)
		}
		/**
		  * Seeks the lowest common parent with another path
		  * @param others Other paths
		  * @return
		  *     1. The (root) path common to all of these paths, None if there is no common path
		  *     1. This path, relative to the common root path
		  *     1. The other paths, relative to the common root path
		  */
		def commonParentWith(others: Seq[Path]) = {
			val allParents = (p +: others).map { _.parentsIterator.toVector.reverse }
			val commonParents = (0 until allParents.map { _.size }.min)
				.iterator.map { i => allParents.map { _ (i) } }
				.takeWhile { _.areAllEqual }
				.map { _.head }
				.toVector
			commonParents.lastOption match {
				case Some(lowestCommonParent) =>
					(Some(lowestCommonParent), lowestCommonParent.relativize(p), others.map(lowestCommonParent.relativize))
				case None => (None, p, others)
			}
		}
		def commonParentWith(other: Path, second: Path, more: Path*): (Option[Path], Path, Seq[Path]) =
			commonParentWith(Vector(other, second) ++ more)
		
		/**
		  * @param other Another path, which may be some parent of this path
		  * @return Either:
		  *         - Right: A path relative to the 'other' path that represents the same element / file as this path
		  *         - Left: This path that is not relative to the other path
		  */
		def relativeTo(other: Path) = {
			// Standardizes both paths, so that they're normalized and either both absolute or relative
			val standardize = {
				if (p.isAbsolute != other.isAbsolute) { path: Path => path.normalize().absolute }
				else { path: Path => path.normalize() }
			}
			
			val a = standardize(p)
			val b = standardize(other)
			val aLength = a.getNameCount
			val bLength = b.getNameCount
			
			// Case: The other path is longer than this one => can't be a parent
			if (bLength > aLength)
				Left(a)
			// Case: The other path is a parent of this path => Returns the remaining portion as a relative path
			else if ((0 until bLength).forall { i => b.getName(i) == a.getName(i) }) {
				if (bLength == aLength)
					Right(Paths.get(""))
				else
					Right(a.subpath(bLength, aLength))
			}
			// Case: The other path is not a parent of this path
			else
				Left(a)
		}
		
		/**
		  * @param childFileName Name of a child file
		  * @return Whether this directory contains the specified file (false if this is not a directory)
		  */
		def containsDirect(childFileName: String) = (this / childFileName).exists
		/**
		  * Checks whether this directory or any sub-directory within this directory contains a file with the
		  * specified name (case-insensitive).
		  * @param childFileName Name of the searched file (including file extension)
		  * @return Whether this directory system contains a file with the specified name
		  */
		def containsRecursive(childFileName: String): Boolean = {
			iterateChildren {
				_.exists { child =>
					(child.fileName ~== childFileName) || child.containsRecursive(childFileName)
				}
			}.getOrElse(false)
		}
		
		/**
		 * Compares this path with another path, possibly including the file contents, and determines whether
		 * these two files have equal content.
		 * Doesn't mean that these paths necessarily point to the same file,
		 * but shows if there are identical file copies.
		 * @param other Another file
		 * @return Whether these two files are identical. Failure in read failure.
		 */
		def hasSameContentAs(other: Path) = {
			// Case: These paths are equal => Won't check their contents
			if (p == other)
				Success(true)
			// Case: One or both files don't exist => Non-existing files are considered equal
			else if (notExists)
				Try { other.notExists }
			else if (other.notExists)
				Success(false)
			// Case: Normalized paths are equal => Won't compare contents
			else if (Try { p.toRealPath() == other.toRealPath() }.getOrElse(false))
				Success(true)
			// Case: Paths are different => Checks whether the file contents are equal
			else
				Try {
					// Compares the files on memory level
					// See: https://www.baeldung.com/java-compare-files
					new RandomAccessFile(p.toFile, "r").consume { file1 =>
						new RandomAccessFile(other.toFile, "r").consume { file2 =>
							val channels = Pair(file1, file2).map { _.getChannel }
							val sizes = channels.map { _.size }
							if (sizes.isAsymmetric)
								false
							else
								channels.map { _.map(FileChannel.MapMode.READ_ONLY, 0L, sizes.first) }
									.isSymmetric
						}
					}
				}
		}
		
		/**
		  * @param newFileName New file name (may or may not contain an extension)
		  * @return A copy of this path with specified file name (NB: No file is being renamed as part of this operation)
		  */
		def withFileName(newFileName: String) = {
			val myName = fileName
			// Case: Already has that file name => returns self
			if (myName == newFileName)
				p
			// Case: Name needs to be changed
			else {
				// Checks whether extension was specified in the new file name.
				// Includes the extension from the old name if necessary
				val actualNewName = if (!newFileName.contains('.') && myName.contains('.'))
					s"$newFileName.${ myName.afterLast(".") }" else newFileName
				// Resolves a new path
				parentOption match {
					case Some(parent) => parent / actualNewName
					case None => actualNewName: Path
				}
			}
		}
		/**
		  * @param f a file name mapping function
		  * @return A copy of this path that has the mapped file name
		  */
		def withMappedFileName(f: String => String) = withFileName(f(fileName))
		
		/**
		  * @param filter A filter that determines which paths will be included. Will be called once for each file
		  *               within this directory and its sub-directories (including the directories themselves).
		  * @return Paths accepted by the filter
		  */
		def findDescendants(filter: Path => Boolean): Try[Vector[Path]] = {
			allChildrenIterator.tryFlatMap { _.map { Some(_).filter(filter) } }.map { _.toVector }
		}
		
		/**
		  * @param extension A file extension (Eg. "png"), not including the '.'
		  * @return All files directly or indirectly under this directory that have the specified file extension / type
		  */
		def allRegularFileChildrenOfType(extension: String) = findDescendants { f =>
			f.isRegularFile && (f.fileType ~== extension)
		}
		
		/**
		  * Moves this file / directory to another directory
		  * @param targetDirectory Target parent directory for this file
		  * @param conflictResolve How file overlap conflicts should be handled. Default = Overwrite the old file.
		  * @return Link to the target path. Failure if file moving failed or if couldn't replace an existing file
		  */
		def moveTo(targetDirectory: Path, conflictResolve: FileConflictResolution = Overwrite): Try[Path] = {
			// Might not need to move at all
			if (parentOption.contains(targetDirectory))
				Success(p)
			else if (notExists)
				Failure(new FileNotFoundException(s"Cannot move ${p.toAbsolutePath} because it doesn't exist"))
			else {
				// Directories with content will have to be first copied, then removed
				if (isDirectory)
					copyTo(targetDirectory, conflictResolve).flatMap { newDir => delete().map { _ => newDir } }
				else {
					// May need to create target directory if it doesn't exist yet
					targetDirectory.asExistingDirectory.flatMap { dir =>
						// May need to delete an existing file / directory
						val newLocation = dir / fileName
						val emptyTarget = {
							if (newLocation.exists)
								conflictResolve(newLocation)
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
		  * @param conflictResolve How file overlap conflicts should be handled. Default = Overwrite the old file.
		  * @return Link to the target path. Failure if file moving failed or if couldn't replace an existing file
		  */
		def copyTo(targetDirectory: Path, conflictResolve: FileConflictResolution = Overwrite) =
			copyAs(targetDirectory/fileName, conflictResolve)
		/**
		  * Copies this file / directory to a new location (over specified path)
		  * @param targetPath   Location, including file name, for the new copy
		  * @param conflictResolve How file overlap conflicts should be handled. Default = Overwrite the old file.
		  * @return Link to the target path. Failure if file moving failed or if couldn't replace an existing file
		  */
		def copyAs(targetPath: Path, conflictResolve: FileConflictResolution = Overwrite) = {
			// May not need to perform any copy
			if (targetPath == p)
				Success(p)
			else if (notExists)
				Failure(new FileNotFoundException(s"$p cannot be copied over $targetPath because $p doesn't exist"))
			else {
				// If the target path already exists, it may need to deleted first, if not, parent directories may
				// need to be created
				val prepareResult = {
					if (targetPath.exists)
						conflictResolve(targetPath)
					else
						targetPath.createParentDirectories()
				}
				// May need to create parent directories
				prepareResult.flatMap { target => recursiveCopyAs(target) }
			}
		}
		private def recursiveCopyTo(targetDirectory: Path): Try[Path] = recursiveCopyAs(targetDirectory / fileName)
		// First copies the file / directory, then the children files, if there are any
		private def recursiveCopyAs(newPath: Path): Try[Path] = {
			// May need to delete the existing file first
			newPath.delete().flatMap { _ => Try { Files.copy(p, newPath) } }.flatMap { newParent =>
				children.flatMap { _.tryForeach { c => new RichPath(c).recursiveCopyTo(newParent).map { _ => () } } }
					.map { _ => newParent }
			}
		}
		/**
		  * Moves and possibly renames this file
		  * @param targetPath   New path for this file, including the file name
		  * @param conflictResolve How file overlap conflicts should be handled. Default = Overwrite the old file.
		  * @return This file's new path. Failure if moving or file deletion failed or if tried to overwrite a file
		  *         while allowReplace = false.
		  */
		def moveAs(targetPath: Path, conflictResolve: FileConflictResolution = Overwrite) = {
			if (targetPath == p)
				Success(p)
			else
				copyAs(targetPath, conflictResolve).flatMap { newPath => delete().map { _ => newPath } }
		}
		/**
		  * Renames this file or directory
		  * @param newFileName    New name for this file or directory (just file name, not the full path)
		  * @param allowOverwrite Whether renaming could overwrite another existing file (default = false). If this is
		  *                       false, fails when trying to rename over an existing file.
		  * @return Path to the newly named file. Failure if renaming failed.
		  */
		def rename(newFileName: String, allowOverwrite: Boolean = false) = {
			// Might not need to rename the file at all
			if (fileName == newFileName)
				Success(p)
			else {
				// Checks whether another path would be overwritten (only when allowOverwrite = false)
				val newPath: Path = p.parentOption.map { _ / newFileName }.getOrElse(newFileName)
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
		def overwriteWith(anotherPath: Path) = {
			// May not need to do anything
			if (anotherPath == p)
				Success(p)
			// The target path must exist
			else if (anotherPath.notExists)
				Failure(new FileNotFoundException(s"Cannot overwrite $p with $anotherPath because $anotherPath doesn't exist"))
			// If both of the files are in the same directory, simply deletes this file
			else if (anotherPath.parentOption == parentOption)
				delete().map { _ => anotherPath }
			else {
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
		def overwriteWithIfChanged(anotherPath: Path): Try[Path] = {
			// May not need to do anything
			if (anotherPath == p)
				Success(p)
			else if (anotherPath.notExists)
				Failure(new FileNotFoundException(s"Cannot overwrite $p with $anotherPath because $anotherPath doesn't exist"))
			else if (notExists)
				overwriteWith(anotherPath)
			// Copying from directory to directory is handled recursively
			else if (isDirectory) {
				if (anotherPath.isDirectory) {
					children.flatMap { myChildren =>
						anotherPath.children.flatMap { newChildren =>
							val myChildrenByName = myChildren.map { c => c.fileName -> c }.toMap
							val newChildrenByName = newChildren.map { c => c.fileName -> c }.toMap
							val myChildNames = myChildrenByName.keySet
							val newChildNames = newChildrenByName.keySet
							
							// Files that didn't exists previously will be copied over
							(newChildNames -- myChildNames).tryMap { name =>
								newChildrenByName(name).copyTo(p)
							}.flatMap { _ =>
								// Files that already existed will be overwritten, if changed
								(newChildNames & myChildNames).tryMap { name =>
									myChildrenByName(name)
										.overwriteWithIfChanged(newChildrenByName(name))
								}.flatMap { _ =>
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
		def delete(allowDeletionOfDirectoryContents: Boolean = true): Try[Boolean] = {
			if (notExists)
				Success(false)
			// In case of a directory, may need to clear contents first
			else if (isDirectory) {
				// If any of child deletion fails, the whole process is interrupted
				// Deletes this directory once the children have been removed
				if (allowDeletionOfDirectoryContents)
					deleteContents().flatMap { dir => Try { Files.deleteIfExists(dir) } }
				else
					Failure(new DirectoryNotEmptyException(
						s"Targeted directory $p is not empty and recursive deletion is disabled"))
			}
			else
				Try { Files.deleteIfExists(p) }.recoverWith { _ =>
					Try {
						Files.setAttribute(p, "dos:readonly", false)
						Files.deleteIfExists(p)
					}
				}
		}
		/**
		  * Deletes all files from under this directory
		  * @return Success containing this directory if all deletions succeeded.
		  *         Failure if one or more of the deletions failed.
		  */
		def deleteContents() =
			iterateChildren { _.map { _.delete() }.toVector.find { _.isFailure }.getOrElse { Success(()) } }
				.flatten.map { _ => p }
		
		/**
		  * Creates this directory (and ensures existence of parent directories as well). If this is not a directory,
		  * simply creates parent directories.
		  * @return This path. Failure if couldn't create directories.
		  */
		def createDirectories() = {
			if (notExists) {
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
		def createParentDirectories() =
			parentOption.map { dir => Try[Unit] { Files.createDirectories(dir) } }.getOrElse(Success(())).map { _ => p }
		
		/**
		  * @param another Another file
		  * @return Whether these two files have same last modified time
		  */
		def hasSameLastModifiedAs(another: Path): Boolean = {
			// Directories need to be handled a bit differently (files inside the directory may have changed)
			val directLastModifiedComparison = lastModified.success.exists { another.lastModified.success.contains }
			if (isDirectory && another.isDirectory) {
				if (directLastModifiedComparison) {
					Pair(children, another.children)
						.map { _.getOrElse(Vector()).sortBy { _.fileName } }
						.isSymmetricWith { _.~==(_)(hasSameLastModifiedEquals) }
				}
				else
					false
			}
			else
				directLastModifiedComparison
		}
		
		/**
		  * Writes specified text to this file (creates or empties file if necessary)
		  * @param text  Text to be written to this file
		  * @param codec Charset / codec used (implicit)
		  * @return This path. Failure if writing failed.
		  */
		def write(text: String)(implicit codec: Codec) =
			createParentDirectories().flatMap { p => Try { Files.write(p, text.getBytes(codec.charSet)) } }
		/**
		  * Writes a json-convertible instance to this file
		  * @param json A json-convertible instance that will produce contents of this file
		  * @return This path. Failure if writing failed.
		  */
		def writeJson(json: JsonConvertible) = write(json.toJson)(Codec.UTF8)
		/**
		  * Writes the specified text lines to a file
		  * @param lines  Lines to write to the file
		  * @param append Whether the lines should be appended to the end of the existing data (true) or whether
		  *               to overwrite the current contents of the file (false). Default = false.
		  * @param codec  Encoding used (implicit)
		  * @return This path. Failure if writing failed.
		  */
		def writeLines(lines: IterableOnce[String], append: Boolean = false)(implicit codec: Codec) = {
			Try {
				new FileOutputStream(p.toFile, append)
					.consume {
						new OutputStreamWriter(_, codec.charSet)
							.consume {
								new PrintWriter(_).consume { writer =>
									lines.iterator.foreach(writer.println)
								}
							}
					}
			}.map { _ => p  }
		}
		/**
		  * Writes into this file with a function. An output stream is opened for the duration of the function.
		  * @param writer A writer function that uses an output stream (may throw)
		  * @return Writer function result. Failure if writing function threw or stream couldn't be opened
		  */
		def writeWith[A](writer: BufferedOutputStream => A) = _writeWith(append = false)(writer)
		/**
		  * Writes a file using a function.
		  * A PrintWriter instance is acquired for the duration of the function execution.
		  * @param writer A function that uses a PrintWriter and then returns
		  * @param codec  Implicit codec used when writing the file
		  * @tparam A Writer function result type
		  * @return Writer function result. Failure if the writing process, or the function, threw an exception.
		  */
		def writeUsing[A](writer: PrintWriter => A)(implicit codec: Codec) =
			_writeUsing(append = false)(writer)
		def _writeUsing[A](append: Boolean)(writer: PrintWriter => A)(implicit codec: Codec) =
			_writeWith(append) { stream =>
				stream.consume { new OutputStreamWriter(_, codec.charSet).consume { new PrintWriter(_).consume(writer) } }
			}
		private def _writeWith[A](append: Boolean)(writer: BufferedOutputStream => A) =
			Try { new FileOutputStream(p.toFile, append).consume { new BufferedOutputStream(_).consume(writer) } }
		/**
		  * Writes into this file by reading data from a reader.
		  * @param reader Reader that supplies the data
		  * @param append Whether the lines should be appended to the end of the existing data (true) or whether
		  *               to overwrite the current contents of the file (false). Default = false.
		  * @return This path. Failure if reading or writing failed or the file stream couldn't be opened
		  */
		def writeFromReader(reader: Reader, append: Boolean = false) = _writeWith(append) { output =>
			// See: https://stackoverflow.com/questions/6927873/
			// how-can-i-read-a-file-to-an-inputstream-then-write-it-into-an-outputstream-in-sc
			Iterator.continually(reader.read)
				.takeWhile { _ != -1 }
				.foreach(output.write)
		}
		/**
		  * Writes the specified input stream into this file
		  * @param inputStream Reader that supplies the data
		  * @return This path. Failure if reading or writing failed or the file stream couldn't be opened
		  */
		def writeStream(inputStream: InputStream) = Try {
			Files.copy(inputStream, p,
				StandardCopyOption.REPLACE_EXISTING)
		}.map { _ => p }
		/**
		  * Appends specified text to this file
		  * @param text  Text to append to this file
		  * @param codec Charset / codec used (implicit)
		  * @return This path. Failure if writing failed.
		  */
		def append(text: String)(implicit codec: Codec) = Try {
			Files.write(p, text.getBytes(codec.charSet),
				StandardOpenOption.APPEND, StandardOpenOption.CREATE)
		}
		/**
		  * Writes into this file with a function. An output stream is opened for the duration of the function.
		  * Doesn't overwrite the current contents of this file but appends to them instead.
		  * @param writer A writer function that uses an output stream (may throw)
		  * @return This path. Failure if writing function threw or stream couldn't be opened
		  */
		def appendWith[U](writer: BufferedOutputStream => U) = _writeWith(append = true)(writer)
		/**
		  * Appends new lines to a file utilizing a PrintWriter
		  * @param writer A function that uses a PrintWriter
		  * @param codec  Implicit codec used when writing the file
		  * @tparam U Arbitrary result type
		  * @return This path. Failure if the writing process, or the specified function, threw an exception.
		  */
		def appendUsing[U](writer: PrintWriter => U)(implicit codec: Codec) =
			_writeUsing(append = true)(writer)
		/**
		  * Writes the specified text lines to the end of this file
		  * @param lines Lines to write to the file
		  * @param codec Encoding used (implicit)
		  */
		def appendLines(lines: IterableOnce[String])(implicit codec: Codec) = writeLines(lines, append = true)
		/**
		  * Writes into this file by reading data from a reader. Doesn't overwrite existing file data but appends
		  * to it instead.
		  * @param reader Reader that supplies the data
		  * @return This path. Failure if reading or writing failed or the file stream couldn't be opened
		  */
		def appendFromReader(reader: Reader) = writeFromReader(reader, append = true)
		
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
		  * Edits this file, saving the edited copy as a separate file
		  * @param copyPath Path of the edited copy
		  * @param f        A function that uses the specified file editor to perform the edits
		  * @param codec    Implicit codec to use when writing the new file
		  * @tparam A Writer function result type
		  * @return Write function result. Failure if this path was a directory or didn't exist,
		  *         or if the writing or reading failed.
		  */
		def editToCopy[A](copyPath: Path)(f: FileEditor => A)(implicit codec: Codec) = {
			// Makes sure this is an existing regular file
			if (isDirectory)
				Failure(new IOException("Directories can't be edited using .editToCopy(...)"))
			else if (notExists)
				Failure(new FileNotFoundException(s"$p doesn't exists and therefore can't be edited"))
			else
				// Writes into the new file using an editor and the specified controlling function
				copyPath.writeUsing { writer =>
					IterateLines.fromPath(p) { linesIterator =>
						val editor = new FileEditor(linesIterator.pollable, writer)
						val result = f(editor)
						// Remaining non-edited lines are copied as is
						editor.flush()
						result
					}
				}.flatten
		}
		/**
		  * Edits this file, saving the edited copy as a separate file in the same directory
		  * @param copyName Name of the edited copy file (extension isn't required)
		  * @param f        A function that uses the specified file editor to perform the edits
		  * @param codec    Implicit codec to use when writing the new file
		  * @tparam A Arbitrary function result type
		  * @return Writer function result type. Failure if this path was a directory or didn't exist,
		  *         or if the writing or reading failed.
		  */
		def editToCopy[A](copyName: String)(f: FileEditor => A)(implicit codec: Codec): Try[A] =
			editToCopy(withFileName(copyName))(f)
		/**
		  * Edits the contents of this file. The edits actualize at the end of this method call.
		  * @param f     A function that uses a file editor to make the edits
		  * @param codec Implicit codec used when writing the new version
		  * @tparam A Writer function result type
		  * @return Writer function result.
		  *         Failure if this file was not editable (e.g. non-existing or a directory) or if
		  *         reading, writing, or replacing failed
		  */
		def edit[A](f: FileEditor => A)(implicit codec: Codec) = {
			// Finds a copy name that hasn't been taken yet
			val (fileNamePart, extensionPart) = fileName.splitAtLast(".").toTuple
			// Writes to copy by editing the original
			val copyPath = withFileName(s"$fileNamePart-temp.$extensionPart").unique
			editToCopy(copyPath)(f)
				.flatMap { result =>
					// Replaces the original with the copy
					copyPath.rename(fileName, allowOverwrite = true).map { _ => result }
				}
		}
		
		/**
		  * Opens this file in the default desktop application. If this is a directory, opens it in resource manager
		  * @return Success of failure
		  */
		def openInDesktop() = performDesktopOperation { _.open(p.toFile) }
		/**
		  * Opens this file for editing in the default desktop application. If this is a directory, opens it in resource manager
		  * @return Success or failure
		  */
		def editInDesktop() =
			performDesktopOperation { d => if (isDirectory) d.open(p.toFile) else d.edit(p.toFile) }
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
			if (isDirectory) d.open(p.toFile) else d.open(parent.toFile)
		}
		/**
		  * Opens resource manager for this file's / directory's parent directory
		  * @return Success or failure
		  */
		def openFileLocation() = performDesktopOperation { _.open(parent.toFile) }
		
		private def performDesktopOperation(f: Desktop => Unit) = {
			if (Desktop.isDesktopSupported)
				Try { f(Desktop.getDesktop) }
			else
				Failure(new UnsupportedOperationException("Desktop is not supported on this platform"))
		}
	}
}

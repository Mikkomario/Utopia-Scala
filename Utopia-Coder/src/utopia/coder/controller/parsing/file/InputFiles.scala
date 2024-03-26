package utopia.coder.controller.parsing.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version

import java.nio.file.Path
import java.time.Instant
import scala.util.{Failure, Success}

/**
  * An utility interface for dealing with input data files
  * @author Mikko Hilpinen
  * @since 26/03/2024, v1.0.2
  */
object InputFiles
{
	/**
	  * Finds the latest (version) file from the specified directory or file
	  * @param path Path to a file or a directory
	  * @param targetExtension Targeted file extension
	  * @return The latest file of the specified type (primarily based on the file version number)
	  *         that appears as or under the specified path.
	  */
	def versionedFileFrom(path: Path, targetExtension: String): Option[Path] =
		OptionsIterator.iterate(versionedFileOrDirectoryFrom(path, targetExtension)) {
			versionedFileOrDirectoryFrom(_, targetExtension) }
			.find { _.isRegularFile }
	
	/**
	  * Finds the latest (version) file or directory from the specified directory or file
	  * @param path Path to a file or a directory
	  * @param targetExtension Targeted file extension
	  * @return The latest file of the specified type (primarily based on the file version number)
	  *         that appears as or under the specified path.
	  *         May also yield a directory containing such files.
	  */
	def versionedFileOrDirectoryFrom(path: Path, targetExtension: String): Option[Path] = {
		// Case: Targeting a file => Returns that file
		if (path.fileType ~== targetExtension)
			Some(path)
		else {
			path.children match {
				// Case: Targeting a directory => Checks whether that directory contains valid options
				case Success(children) =>
					children.filter { _.fileType ~== targetExtension }.emptyOneOrMany match {
						// Case: Directory doesn't contain valid options as children => Looks for sub-directories instead
						//       NB: Returns a sub-directory, not a specific file
						case None =>
							children.filter { _.isDirectory }.emptyOneOrMany
								.map {
									case Left(only) => only
									case Right(options) => selectLatestFrom(options)
								}
							
						// Case: Directory contains only one valid option => Selects that one
						case Some(Left(file)) => Some(file)
						// Case: Directory contains valid options =>
						// Selects one of them based on their version number or last modified time
						case Some(Right(options)) => Some(selectLatestFrom(options))
					}
				// Case: Not a directory either => Fails
				case Failure(_) => None
			}
		}
	}
	
	// Assumes non-empty input
	private def selectLatestFrom(options: Iterable[Path]) = {
		// Searches for version numbers and uses those, if present
		options.flatMap { p => Version.findFrom(p.fileName.untilLast(".")).map { _ -> p } }
			.maxByOption { _._1 } match
		{
			case Some(latest) => latest._2
			// Case: Version numbers not present => Uses last modified -times instead
			case None => options.maxBy { _.lastModified.getOrElse(Instant.EPOCH) }
		}
	}
}

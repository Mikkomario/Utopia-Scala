package utopia.flow.parse.file

import FileExtensions._

import java.io.IOException
import java.nio.file.Path
import scala.util.{Failure, Success, Try}

/**
 * An enumeration for different ways to resolve file conflicts,
 * i.e. situations where two different files would have the same path.
 * @author Mikko Hilpinen
 * @since 20.2.2023, v2.1
 */
trait FileConflictResolution
{
	/**
	 * Resolves a file conflict
	 * @param existingFile An existing file path
	 * @return Path given to the file that would otherwise overwrite the specified file.
	 *         Failure if the operation should fail.
	 */
	def apply(existingFile: Path): Try[Path]
}

object FileConflictResolution
{
	/**
	 * Overwrites an existing file with the new file
	 */
	case object Overwrite extends FileConflictResolution
	{
		override def apply(existingFile: Path) = existingFile.delete().map { _ => existingFile }
	}
	
	/**
	 * Refuses to overwrite an existing file and fails instead
	 */
	case object Fail extends FileConflictResolution
	{
		override def apply(existingFile: Path) =
			Failure[Path](new IOException(s"$existingFile already exists"))
	}
	
	/**
	 * Writes the new file with a different, non-conflicting name
	 */
	case object Rename extends FileConflictResolution
	{
		override def apply(existingFile: Path) = Success(existingFile.unique)
	}
}

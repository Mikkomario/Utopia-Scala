package utopia.trove.controller

import utopia.flow.collection.immutable.{Empty, Single}

import java.nio.file.Path
import utopia.flow.util.Version
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.IterateLines
import utopia.flow.util.StringExtensions._
import utopia.trove.model.DatabaseStructureSource
import utopia.trove.model.enumeration.SqlFileType
import utopia.trove.model.enumeration.SqlFileType.{Changes, Full}

/**
  * Used for searching for usable sql source files
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
object ScanSourceFiles
{
	/**
	  * Finds the sql files to import. Only .sql files will be recognized. The files to read should include following
	  * key-value pairs in first comments, separated with ':': 'version' (or 'to'), 'type' containing value
	  * 'full' or 'changes' (or 'update') (if type is omitted, Full is used). Also, if a file specifies changes
	  * from one version to another, it should contain a 'from' or 'origin' parameter with a version number value.<br>
	  * For example:<br>
	  * ---- File Start ----<br>
	  * -- Type: Changes<br>
	  * -- From: v1.2.2<br>
	  * -- To: v1.2.3<br>
	  * Or:<br>
	  * ---- File Start ----<br>
	  * -- Version: v1.2.3<br>
	  * -- Type: Full<br>
	  * @param directory The directory from which to search for sql files. Will also search files from sub-directories.
	  * @param currentDbVersion Current database version number. None if no database has been set up yet (default)
	  * @return Sql file sources to import (in order). Resulting vector contains either a single full update file,
	  *         a list of changes to apply back to back. The vector may also be empty in case no update was needed
	  *         or no update files were found. Returns a failure if directory reading failed. Individual file read
	  *         errors are ignored.
	  */
	def apply(directory: Path, currentDbVersion: Option[Version] = None) =
	{
		// Starts by finding all sql files in source directory
		directory.allRegularFileChildrenOfType("sql").map { files =>
			// Categorizes the files by checking first comments
			val sources = files.flatMap { file =>
				val comments = IterateLines.fromPath(file) { _.filterNot { _.isEmpty }.takeWhile { _.startsWith("--") }
					.flatMap { line =>
						val keyValuePair = line.drop(2).splitAtFirst(":").map { _.trim.toLowerCase }
						if (keyValuePair.forall { _.nonEmpty })
							Some(keyValuePair.toTuple)
						else
							None
					}.toMap
				}
				comments.toOption.flatMap { comments =>
					comments.get("version").orElse(comments.get("to")).map { versionString =>
						val sourceVersion = comments.get("from").orElse(comments.get("origin")).map(Version.apply)
						val fileType = comments.get("type").map(SqlFileType.forString)
							.getOrElse { if (sourceVersion.isEmpty) Full else Changes }
						DatabaseStructureSource(file, fileType, Version(versionString), sourceVersion)
					}
				}
			}.sortBy { _.targetVersion }
			
			if (sources.isEmpty)
				Empty
			else {
				// Checks which of the sources need to be read
				currentDbVersion match {
					case Some(currentVersion) =>
						val latestVersion = sources.last.targetVersion
						if (latestVersion > currentVersion) {
							// Reads only update files, if they form a path to the latest version.
							// Otherwise reads the full version, if present.
							val updates = sources.filter { _.fileType == Changes }.dropWhile { s =>
								s.targetVersion <= currentVersion || s.originVersion.exists { _ < currentVersion } }
							if (updates.headOption.exists { _.originVersion.forall { origin =>
								origin == currentVersion || origin == currentVersion.withoutSuffix } } &&
								updates.lastOption.exists { _.targetVersion == latestVersion })
								updates
							else
								sources.findLast { _.fileType == Full }.map { Single(_) }.getOrElse(updates)
						}
						else
							Empty
					case None => sources.findLast { _.fileType == Full }.toVector
				}
			}
		}
	}
}

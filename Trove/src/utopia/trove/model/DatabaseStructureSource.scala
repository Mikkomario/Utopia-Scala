package utopia.trove.model

import utopia.flow.util.Version

import java.nio.file.Path
import utopia.trove.model.enumeration.SqlFileType
import utopia.trove.model.enumeration.SqlFileType.{Changes, Full}

/**
  * Represents a file from which database structure can be imported. Contains important metadata.
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
case class DatabaseStructureSource(path: Path, fileType: SqlFileType, targetVersion: Version,
                                   originVersion: Option[Version] = None)
{
	override def toString = fileType match
	{
		case Full => s"$path (Full $targetVersion)"
		case Changes => s"$path (Changes${originVersion.map { v => s" from $v" }} to $targetVersion)"
	}
}

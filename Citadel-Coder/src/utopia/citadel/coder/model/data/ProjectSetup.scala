package utopia.citadel.coder.model.data

import java.nio.file.Path

/**
  * Represents project specific settings used when writing documents
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  * @param projectPackage Package that is common to all files in the target project
  * @param sourceRoot Path to the export source directory
  */
case class ProjectSetup(projectPackage: String, sourceRoot: Path)

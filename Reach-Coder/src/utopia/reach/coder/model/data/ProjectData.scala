package utopia.reach.coder.model.data

import utopia.coder.model.data.Name
import utopia.flow.util.Version

/**
  * Contains all user-defined project data
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  * @constructor Creates a new project data set
  * @param name Name of this project
  * @param factories Defined component factories
  * @param version Specified project version, if known
  */
case class ProjectData(name: Name, factories: Vector[ComponentFactory], version: Option[Version] = None)

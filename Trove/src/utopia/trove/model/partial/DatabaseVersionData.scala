package utopia.trove.model.partial

import utopia.flow.time.Now
import utopia.flow.util.Version

import java.time.Instant

/**
  * Contains basic information about a database version
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1
  * @param number Database version number
  * @param created Creation time of this version
  */
case class DatabaseVersionData(number: Version, created: Instant = Now)
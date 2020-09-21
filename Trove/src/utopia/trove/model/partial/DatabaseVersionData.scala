package utopia.trove.model.partial

import java.time.Instant

import utopia.trove.model.VersionNumber

/**
  * Contains basic information about a database version
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1
  * @param number Database version number
  * @param created Creation time of this version
  */
case class DatabaseVersionData(number: VersionNumber, created: Instant = Instant.now())
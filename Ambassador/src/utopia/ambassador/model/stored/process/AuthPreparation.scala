package utopia.ambassador.model.stored.process

import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.vault.model.template.Stored

/**
  * Represents an authentication preparation that has been stored to DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthPreparation(id: Int, data: AuthPreparationData) extends Stored[AuthPreparationData, Int]

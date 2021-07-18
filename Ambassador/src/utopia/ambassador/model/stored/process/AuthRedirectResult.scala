package utopia.ambassador.model.stored.process

import utopia.ambassador.model.partial.process.AuthRedirectResultData
import utopia.vault.model.template.Stored

/**
  * Represents an authentication result that has been stored to the DB already
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthRedirectResult(id: Int, data: AuthRedirectResultData) extends Stored[AuthRedirectResultData, Int]

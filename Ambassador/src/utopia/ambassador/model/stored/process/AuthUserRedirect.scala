package utopia.ambassador.model.stored.process

import utopia.ambassador.model.partial.process.AuthUserRedirectData
import utopia.vault.model.template.Stored

/**
  * Represents a user redirection event that has been stored to DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthUserRedirect(id: Int, data: AuthUserRedirectData) extends Stored[AuthUserRedirectData, Int]

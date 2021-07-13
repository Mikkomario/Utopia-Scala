package utopia.ambassador.model.stored.token

import utopia.ambassador.model.partial.token.AuthTokenData
import utopia.vault.model.template.Stored

/**
  * Represents an authorization token that has been stored in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class AuthToken(id: Int, data: AuthTokenData) extends Stored[AuthTokenData, Int]

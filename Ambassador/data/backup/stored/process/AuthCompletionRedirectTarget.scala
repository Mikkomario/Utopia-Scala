package utopia.ambassador.model.stored.process

import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.vault.model.template.Stored

/**
  * Represents a client-specified redirection target that has been stored to the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthCompletionRedirectTarget(id: Int, data: AuthCompletionRedirectTargetData)
	extends Stored[AuthCompletionRedirectTargetData, Int]

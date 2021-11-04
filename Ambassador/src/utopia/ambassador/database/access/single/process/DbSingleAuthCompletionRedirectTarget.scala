package utopia.ambassador.database.access.single.process

import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthCompletionRedirectTargets, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthCompletionRedirectTarget(id: Int) 
	extends UniqueAuthCompletionRedirectTargetAccess with SingleIntIdModelAccess[AuthCompletionRedirectTarget]


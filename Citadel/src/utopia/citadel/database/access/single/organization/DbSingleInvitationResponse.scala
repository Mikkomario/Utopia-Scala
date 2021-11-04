package utopia.citadel.database.access.single.organization

import utopia.metropolis.model.stored.organization.InvitationResponse
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual InvitationResponses, based on their id
  * @since 2021-10-23
  */
case class DbSingleInvitationResponse(id: Int) 
	extends UniqueInvitationResponseAccess with SingleIntIdModelAccess[InvitationResponse]


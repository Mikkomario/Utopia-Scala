package utopia.citadel.database.access.many.organization

import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing multiple invitations at once, including their responses
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object DbInvitationsWithResponses
	extends ManyInvitationsWithResponsesAccess with NonDeprecatedView[InvitationWithResponse]

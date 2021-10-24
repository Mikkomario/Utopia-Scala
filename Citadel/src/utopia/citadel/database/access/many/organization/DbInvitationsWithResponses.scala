package utopia.citadel.database.access.many.organization

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple invitations at once, including their responses
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object DbInvitationsWithResponses extends ManyInvitationsWithResponsesAccess with UnconditionalView

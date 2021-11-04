package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.InvitationFactory
import utopia.citadel.database.model.organization.InvitationModel
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual Invitations
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbInvitation extends SingleRowModelAccess[Invitation] with NonDeprecatedView[Invitation] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = InvitationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Invitation instance
	  * @return An access point to that Invitation
	  */
	def apply(id: Int) = DbSingleInvitation(id)
}


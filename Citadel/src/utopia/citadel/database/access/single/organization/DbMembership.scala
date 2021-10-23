package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.citadel.database.model.organization.MembershipModel
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual Memberships
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMembership extends SingleRowModelAccess[Membership] with NonDeprecatedView[Membership] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MembershipModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Membership instance
	  * @return An access point to that Membership
	  */
	def apply(id: Int) = DbSingleMembership(id)
}


package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.{InvitationFactory, InvitationResponseFactory, InvitationWithResponseFactory}
import utopia.citadel.database.model.organization.{InvitationModel, InvitationResponseModel}
import utopia.flow.time.Now
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.Larger
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.sql.{JoinType, Select, Where}

/**
  * Common trait for multiple invitations -access points
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1.0
  */
@deprecated("Replaced with ManyInvitationsAccess", "v2.0")
trait InvitationsAccess extends ManyModelAccess[Invitation]
{
	// IMPLEMENTED	------------------------
	
	override def factory = InvitationFactory
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return Primary model type used for creating conditions etc.
	  */
	protected def model = InvitationModel
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Invitations that have been blocked
	  */
	def blocked(implicit connection: Connection) =
	{
		val additionalCondition = InvitationResponseModel.blocked.toCondition
		InvitationWithResponseFactory.getMany(mergeCondition(additionalCondition))
	}
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Invitations that are currently without response
	  */
	def pending(implicit connection: Connection) =
	{
		// Pending invitations must not be joined to a response and not be expired
		val noResponseCondition = InvitationResponseFactory.table.primaryColumn.get.isNull
		val pendingCondition = InvitationModel.withExpires(Now).toConditionWithOperator(Larger)
		// Has to join invitation response table for the condition to work
		connection(Select(InvitationFactory.target.join(InvitationResponseFactory.table, JoinType.Left), InvitationModel.table) +
			Where(mergeCondition(noResponseCondition && pendingCondition))).parse(factory)
	}
}

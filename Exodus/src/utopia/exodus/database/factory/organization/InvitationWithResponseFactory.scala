package utopia.exodus.database.factory.organization

import utopia.exodus.database.model.organization.InvitationModel
import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.FromRowFactory
import utopia.vault.sql.JoinType

/**
  * Used for reading invitation data, including response data
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object InvitationWithResponseFactory extends FromRowFactory[InvitationWithResponse]
{
	// IMPLEMENTED	------------------------------
	
	override def apply(row: Row) = InvitationFactory(row).flatMap { invitation =>
		InvitationResponseFactory(row).map { response => InvitationWithResponse(invitation, response) }
	}
	
	override def joinType = JoinType.Inner
	
	override def table = InvitationModel.table
	
	override def joinedTables = InvitationResponseFactory.tables
}

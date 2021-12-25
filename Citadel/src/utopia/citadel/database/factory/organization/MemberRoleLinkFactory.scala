package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.organization.MemberRoleLinkModel
import utopia.flow.datastructure.immutable.Model
import utopia.metropolis.model.partial.organization.MemberRoleLinkData
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading MemberRole data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object MemberRoleLinkFactory extends FromValidatedRowModelFactory[MemberRoleLink] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def nonDeprecatedCondition = MemberRoleLinkModel.nonDeprecatedCondition
	
	override def defaultOrdering = None
	
	override def table = CitadelTables.memberRoleLink
	
	override def fromValidatedModel(valid: Model) =
		MemberRoleLink(valid("id").getInt, MemberRoleLinkData(valid("membershipId").getInt, valid("roleId").getInt,
			valid("creatorId").int, valid("created").getInstant, valid("deprecatedAfter").instant))
}


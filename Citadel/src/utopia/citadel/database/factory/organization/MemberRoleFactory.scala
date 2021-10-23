package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.organization.MemberRoleModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.MemberRoleData
import utopia.metropolis.model.stored.organization.MemberRole
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading MemberRole data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object MemberRoleFactory extends FromValidatedRowModelFactory[MemberRole] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def nonDeprecatedCondition = MemberRoleModel.nonDeprecatedCondition
	
	override def table = CitadelTables.memberRole
	
	override def fromValidatedModel(valid: Model[Constant]) = 
		MemberRole(valid("id").getInt, MemberRoleData(valid("membershipId").getInt, valid("roleId").getInt, 
			valid("creatorId").int, valid("created").getInstant, valid("deprecatedAfter").instant))
}


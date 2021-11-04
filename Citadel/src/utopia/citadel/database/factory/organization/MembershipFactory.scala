package utopia.citadel.database.factory.organization

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.organization.MembershipModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading Membership data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object MembershipFactory 
	extends FromValidatedRowModelFactory[Membership] with FromRowFactoryWithTimestamps[Membership] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "started"
	
	override def nonDeprecatedCondition = MembershipModel.nonDeprecatedCondition
	
	override def table = CitadelTables.membership
	
	override def fromValidatedModel(valid: Model) =
		Membership(valid("id").getInt, MembershipData(valid("organizationId").getInt, valid("userId").getInt, 
			valid("creatorId").int, valid("started").getInstant, valid("ended").instant))
}


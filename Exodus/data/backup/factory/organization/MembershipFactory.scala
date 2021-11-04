package utopia.exodus.database.factory.organization

import utopia.exodus.database.Tables
import utopia.exodus.database.model.organization.MembershipModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading organization memberships from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object MembershipFactory extends FromValidatedRowModelFactory[Membership] with Deprecatable
{
	// IMPLEMENTED	--------------------------
	
	override val nonDeprecatedCondition = table("ended").isNull
	
	override protected def fromValidatedModel(model: Model) = Membership(model("id").getInt,
		MembershipData(model(this.model.organizationIdAttName).getInt, model("userId").getInt, model("creatorId").int,
			model("started").getInstant, model("ended").instant))
	
	override def table = Tables.organizationMembership
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Model referenced by this factory
	  */
	def model = MembershipModel
}

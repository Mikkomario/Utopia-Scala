package utopia.citadel.database.factory.organization

import utopia.citadel.database.Tables
import utopia.citadel.database.model.organization.MembershipModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading organization memberships from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1.0
  */
object MembershipFactory extends FromValidatedRowModelFactory[Membership] with Deprecatable
{
	// IMPLEMENTED	--------------------------
	
	override val nonDeprecatedCondition = table("ended").isNull
	
	override protected def fromValidatedModel(model: Model[Constant]) = Membership(model("id").getInt,
		MembershipData(model(this.model.organizationIdAttName).getInt, model("userId").getInt, model("creatorId").int,
			model("started").getInstant, model("ended").instant))
	
	override def table = Tables.organizationMembership
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Model referenced by this factory
	  */
	def model = MembershipModel
}

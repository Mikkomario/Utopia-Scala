package utopia.citadel.database.factory.organization

import utopia.citadel.database.Tables
import utopia.citadel.database.model.organization.MembershipModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading organization memberships from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1.0
  */
object MembershipFactory extends FromValidatedRowModelFactory[Membership]
	with FromRowFactoryWithTimestamps[Membership] with Deprecatable
{
	// ATTRIBUTES   --------------------------
	
	override val creationTimePropertyName = "started"
	
	
	// IMPLEMENTED	--------------------------
	
	override def table = Tables.organizationMembership
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model) = Membership(model("id").getInt,
		MembershipData(model(this.model.organizationIdAttName).getInt, model("userId").getInt, model("creatorId").int,
			model(creationTimePropertyName).getInstant, model("ended").instant))
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Model referenced by this factory
	  */
	def model = MembershipModel
}

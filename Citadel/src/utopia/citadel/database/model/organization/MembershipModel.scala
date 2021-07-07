package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.citadel.database.model.NullDeprecatable
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object MembershipModel extends NullDeprecatable[MembershipModel]
{
	// ATTRIBUTES	--------------------------
	
	override val deprecationAttName = "ended"
	
	/**
	  * Name of the attribute that contains the associated organization's id
	  */
	val organizationIdAttName = "organizationId"
	
	
	// COMPUTED	-------------------------------
	
	/**
	 * @return The factory used by this model
	 */
	def factory = MembershipFactory
	
	/**
	 * @return Column that contains the id of the organization this membership applies to
	 */
	def organizationIdColumn = table(organizationIdAttName)
	
	/**
	  * @return A model that has just been marked as an ended membership
	  */
	def nowEnded = apply(ended = Some(Now))
	
	
	// IMPLEMENTED  ---------------------------
	
	override def table = factory.table
	
	override def withDeprecatedAfter(deprecation: Instant) = apply(ended = Some(deprecation))
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param organizationId Id of the target organization
	  * @return A model with only organization id set
	  */
	def withOrganizationId(organizationId: Int) = apply(organizationId = Some(organizationId))
	
	/**
	  * @param userId Targeted user's id
	  * @return A model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * Inserts a new membership to DB
	  * @param data Membership data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted membership
	  */
	def insert(data: MembershipData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.organizationId), Some(data.userId), data.creatorId, Some(data.started), data.ended)
			.insert().getInt
		Membership(newId, data)
	}
}

/**
  * Used for interacting with organization memberships in DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
case class MembershipModel(id: Option[Int] = None, organizationId: Option[Int] = None,
						   userId: Option[Int] = None, creatorId: Option[Int] = None, started: Option[Instant] = None,
						   ended: Option[Instant] = None) extends StorableWithFactory[Membership]
{
	import MembershipModel._
	
	// IMPLEMENTED	---------------------------
	
	override def factory = MembershipModel.factory
	
	override def valueProperties = Vector("id" -> id, organizationIdAttName -> organizationId, "userId" -> userId,
		"creatorId" -> creatorId, "started" -> started, deprecationAttName -> ended)
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param organizationId Id of target organization
	  * @return A copy of this model with specified organization id
	  */
	def withOrganizationId(organizationId: Int) = copy(organizationId = Some(organizationId))
}

package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.NullDeprecatable

/**
  * Used for constructing MembershipModel instances and for inserting Memberships to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object MembershipModel 
	extends DataInserter[MembershipModel, Membership, MembershipData] with NullDeprecatable[MembershipModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Membership organizationId
	  */
	val organizationIdAttName = "organizationId"
	
	/**
	  * Name of the property that contains Membership userId
	  */
	val userIdAttName = "userId"
	
	/**
	  * Name of the property that contains Membership creatorId
	  */
	val creatorIdAttName = "creatorId"
	
	/**
	  * Name of the property that contains Membership started
	  */
	val startedAttName = "started"
	
	/**
	  * Name of the property that contains Membership ended
	  */
	val endedAttName = "ended"
	
	override val deprecationAttName = "ended"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Membership organizationId
	  */
	def organizationIdColumn = table(organizationIdAttName)
	
	/**
	  * Column that contains Membership userId
	  */
	def userIdColumn = table(userIdAttName)
	
	/**
	  * Column that contains Membership creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	
	/**
	  * Column that contains Membership started
	  */
	def startedColumn = table(startedAttName)
	
	/**
	  * Column that contains Membership ended
	  */
	def endedColumn = table(endedAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = MembershipFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: MembershipData) = 
		apply(None, Some(data.organizationId), Some(data.userId), data.creatorId, Some(data.started), 
			data.ended)
	
	override def complete(id: Value, data: MembershipData) = Membership(id.getInt, data)
	
	override def withDeprecatedAfter(deprecationTime: Instant) = withEnded(deprecationTime)
	
	
	// OTHER	--------------------
	
	/**
	  * @param creatorId Id of the user who created/started this membership
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	
	/**
	  * @param ended Time when this membership ended (if applicable)
	  * @return A model containing only the specified ended
	  */
	def withEnded(ended: Instant) = apply(ended = Some(ended))
	
	/**
	  * @param id A Membership id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param organizationId Id of the organization the referenced user is/was a member of
	  * @return A model containing only the specified organizationId
	  */
	def withOrganizationId(organizationId: Int) = apply(organizationId = Some(organizationId))
	
	/**
	  * @param started Time when this membership started
	  * @return A model containing only the specified started
	  */
	def withStarted(started: Instant) = apply(started = Some(started))
	
	/**
	  * @param userId Id of the user who is/was a member of the referenced organization
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with Memberships in the database
  * @param id Membership database id
  * @param organizationId Id of the organization the referenced user is/was a member of
  * @param userId Id of the user who is/was a member of the referenced organization
  * @param creatorId Id of the user who created/started this membership
  * @param started Time when this membership started
  * @param ended Time when this membership ended (if applicable)
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class MembershipModel(id: Option[Int] = None, organizationId: Option[Int] = None, 
	userId: Option[Int] = None, creatorId: Option[Int] = None, started: Option[Instant] = None, 
	ended: Option[Instant] = None) 
	extends StorableWithFactory[Membership]
{
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipModel.factory
	
	override def valueProperties = 
	{
		import MembershipModel._
		Vector("id" -> id, organizationIdAttName -> organizationId, userIdAttName -> userId, 
			creatorIdAttName -> creatorId, startedAttName -> started, endedAttName -> ended)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param creatorId A new creatorId
	  * @return A new copy of this model with the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = copy(creatorId = Some(creatorId))
	
	/**
	  * @param ended A new ended
	  * @return A new copy of this model with the specified ended
	  */
	def withEnded(ended: Instant) = copy(ended = Some(ended))
	
	/**
	  * @param organizationId A new organizationId
	  * @return A new copy of this model with the specified organizationId
	  */
	def withOrganizationId(organizationId: Int) = copy(organizationId = Some(organizationId))
	
	/**
	  * @param started A new started
	  * @return A new copy of this model with the specified started
	  */
	def withStarted(started: Instant) = copy(started = Some(started))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}


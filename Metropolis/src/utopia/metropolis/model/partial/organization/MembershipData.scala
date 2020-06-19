package utopia.metropolis.model.partial.organization

import java.time.Instant

/**
  * Contains basic data about an organization membership
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  * @param organizationId Id of the organization the user belongs to
  * @param userId Id of the user who belongs to the organization
  * @param creatorId Id of the user who created this membership
  * @param started Timestamp when the membership started (default = current time)
  * @param ended Timestamp when the membership ended. None if not ended (default).
  */
case class MembershipData(organizationId: Int, userId: Int, creatorId: Option[Int] = None,
						  started: Instant = Instant.now(), ended: Option[Instant] = None)

package utopia.metropolis.model.combined.organization

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.organization.Organization

object DescribedOrganization extends DescribedFromModelFactory[Organization, DescribedOrganization]
{
	// IMPLEMENTED  ---------------------------
	
	override protected def undescribedFactory = Organization
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param organizationId A organization id
	  * @param descriptions Descriptions of that organization
	  * @return A described wrapper for that id
	  */
	def apply(organizationId: Int, descriptions: Set[LinkedDescription]): DescribedOrganization =
		apply(Organization(organizationId), descriptions)
}

/**
  * Combines Organization with the linked descriptions
  * @param organization Organization to wrap
  * @param descriptions Descriptions concerning the wrapped Organization
  * @since 2021-10-23
  */
case class DescribedOrganization(organization: Organization, descriptions: Set[LinkedDescription])
	extends DescribedWrapper[Organization] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = organization
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) =
		Model(Vector("id" -> wrapped.id))
}


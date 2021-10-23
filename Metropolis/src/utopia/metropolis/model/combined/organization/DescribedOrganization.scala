package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.organization.Organization

object DescribedOrganization extends DescribedFromModelFactory[Organization, DescribedOrganization]
{
	override protected def undescribedFactory = Organization
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


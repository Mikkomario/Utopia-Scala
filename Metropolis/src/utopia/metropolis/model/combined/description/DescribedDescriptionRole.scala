package utopia.metropolis.model.combined.description

import utopia.metropolis.model.stored.description.DescriptionRole

object DescribedDescriptionRole extends DescribedFromModelFactory[DescriptionRole, DescribedDescriptionRole]
{
	override protected def undescribedFactory = DescriptionRole
}

/**
  * Combines DescriptionRole with the linked descriptions
  * @param descriptionRole DescriptionRole to wrap
  * @param descriptions Descriptions concerning the wrapped DescriptionRole
  * @since 2021-10-23
  */
case class DescribedDescriptionRole(descriptionRole: DescriptionRole, descriptions: Set[LinkedDescription])
	extends DescribedWrapper[DescriptionRole] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = descriptionRole
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}


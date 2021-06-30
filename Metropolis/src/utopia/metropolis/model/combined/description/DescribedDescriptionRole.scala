package utopia.metropolis.model.combined.description

import utopia.metropolis.model.stored.description.{DescriptionLink, DescriptionRole}

object DescribedDescriptionRole extends DescribedFromModelFactory[DescribedDescriptionRole, DescriptionRole]
{
	override protected def undescribedFactory = DescriptionRole
}

/**
  * Combines a description role with some descriptions
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
case class DescribedDescriptionRole(wrapped: DescriptionRole, descriptions: Set[DescriptionLink])
	extends DescribedWrapper[DescriptionRole] with SimplyDescribed
{
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}

package utopia.metropolis.model.combined.description

import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.stored.description.DescriptionRole

/**
  * A wrapper-based version of the SimplyDescribed trait
  * @author Mikko Hilpinen
  * @since 30.6.2021, v1.1
  */
trait SimplyDescribedWrapper[+A <: StyledModelConvertible] extends DescribedWrapper[A] with SimplyDescribed
{
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toSimpleModel
}

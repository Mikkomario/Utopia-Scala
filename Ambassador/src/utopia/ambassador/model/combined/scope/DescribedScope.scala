package utopia.ambassador.model.combined.scope

import utopia.ambassador.model.stored.scope.Scope
import utopia.metropolis.model.combined.description.{DescribedFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole

object DescribedScope extends DescribedFactory[Scope, DescribedScope]

/**
  * Combines Scope with the linked descriptions
  * @param scope Scope to wrap
  * @param descriptions Descriptions concerning the wrapped Scope
  * @since 2021-10-26
  */
case class DescribedScope(scope: Scope, descriptions: Set[LinkedDescription]) 
	extends DescribedWrapper[Scope] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = scope
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}


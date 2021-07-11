package utopia.ambassador.model.combined.scope

import utopia.ambassador.model.stored.scope.Scope
import utopia.metropolis.model.combined.description.SimplyDescribedWrapper
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * Combines a scope with its descriptions
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class DescribedScope(scope: Scope, descriptions: Set[DescriptionLink]) extends SimplyDescribedWrapper[Scope]
{
	override def wrapped = scope
}

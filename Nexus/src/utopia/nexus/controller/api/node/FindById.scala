package utopia.nexus.controller.api.node

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.{Follow, NotFound}

/**
  * A common trait for API nodes that provide access to other nodes based on an item id / a variable path parameter.
  * For example, a node 'items' would provide access to 'items/1', 'items/2' etc.,
  * depending on the type of the ID in question (in this example, integer)
  * @author Mikko Hilpinen
  * @since 6.11.2025, v2.0, based on ItemsByIdResource written 13.10.2022 for v1.9
  */
trait FindById[-C] extends ApiNode[C]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param id A resource id in value format. Parsed from a path parameter.
	  * @return A resource accessible with that id. None if the id was invalid (i.e. not of correct type or format).
	  */
	protected def find(id: Value): Option[ApiNode[C]]
	
	
	// IMPLEMENTED  ---------------------
	
	override def follow(step: String)(implicit context: C): PathFollowResult[C] = find(step) match {
		case Some(node) => Follow(node)
		case None => NotFound(s"$step is not a valid ID")
	}
}

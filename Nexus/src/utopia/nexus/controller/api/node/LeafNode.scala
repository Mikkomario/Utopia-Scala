package utopia.nexus.controller.api.node

import utopia.nexus.model.api.PathFollowResult.NotFound

/**
  * A common utility trait for Rest nodes that don't have any child nodes
  * @author Mikko Hilpinen
  * @since 8.3.2021, v1.5.1
  */
trait LeafNode[-C] extends ApiNode[C]
{
	override def follow(step: String)(implicit context: C) = NotFound(s"$name doesn't contain any child nodes")
}

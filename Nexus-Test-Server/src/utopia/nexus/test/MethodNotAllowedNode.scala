package utopia.nexus.test

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.flow.collection.immutable.Single
import utopia.nexus.controller.api.node.LeafNode
import utopia.nexus.model.response.RequestResult

/**
  * A simple test resource that only allows the POST method
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
object MethodNotAllowedNode extends LeafNode[Any]
{
	override val name: String = "unallowed"
	override val allowedMethods = Single(Post)
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: Any): RequestResult =
		RequestResult.Empty
}

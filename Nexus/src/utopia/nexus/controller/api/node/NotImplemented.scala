package utopia.nexus.controller.api.node

import utopia.access.model.enumeration.{Method, Status}
import utopia.flow.collection.immutable.Empty
import utopia.nexus.model.response.RequestResult

/**
  * A common trait for API nodes which don't contain any request handling implementation.
 * These nodes solely rely on their child nodes.
  * @tparam C Type of context required by the child nodes
 * @author Mikko Hilpinen
  * @since 06.11.2025, v2.0, based on NotImplementedResource written 18.2.2022 for v1.7
  */
trait NotImplemented[-C] extends ApiNode[C]
{
	override def allowedMethods: Seq[Method] = Empty
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult =
		Status.NotImplemented -> s"No implementation for $name exists at this time"
}

package utopia.nexus.test

import utopia.access.model.enumeration.ContentCategory.Application
import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Get
import utopia.flow.async.process.Wait
import utopia.flow.collection.immutable.Single
import utopia.flow.time.TimeExtensions._
import utopia.nexus.controller.api.node.LeafNode
import utopia.nexus.controller.write.WriteResponseBody
import utopia.nexus.model.response.RequestResult

import scala.concurrent.ExecutionContext

/**
 * An API node that slowly produces a content stream
 * @author Mikko Hilpinen
 * @since 09.11.2025, v2.0
 */
class StreamNode(implicit exc: ExecutionContext) extends LeafNode[Any]
{
	// ATTRIBUTES   ----------------------
	
	override val name: String = "stream"
	override val allowedMethods: Iterable[Method] = Single(Get)
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: Any): RequestResult =
		RequestResult.withBody(WriteResponseBody.stream.usingWriter(Application.json) { writer =>
			writer.print(s"[1")
			writer.flush()
			(2 to 10).foreach { i =>
				Wait(0.5.seconds)
				writer.print(s", $i")
				writer.flush()
			}
			writer.print("]")
			writer.flush()
		})
}

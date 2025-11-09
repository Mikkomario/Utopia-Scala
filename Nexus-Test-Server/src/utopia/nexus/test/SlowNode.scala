package utopia.nexus.test

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Get
import utopia.flow.async.process.Wait
import utopia.flow.collection.immutable.Single
import utopia.flow.time.TimeExtensions._
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.Redirected
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.RequestResult

import scala.concurrent.ExecutionContext

/**
  * Delays request handling somewhat, and then forwards the request
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
class SlowNode(implicit exc: ExecutionContext) extends ApiNode[RequestContext[Any]]
{
	// ATTRIBUTES   ----------------------
	
	override val name: String = "slow"
	override val allowedMethods: Iterable[Method] = Single(Get)
	
	
	// IMPLEMENTED  ----------------------
	
	override def follow(step: String)(implicit context: RequestContext[Any]): PathFollowResult[RequestContext[Any]] = {
		// Waits X seconds and then redirects to the resource at the end of the remaining path
		wait()
		// TODO: Should redirect whole remaining path
		Redirected(Single(step))
	}
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: RequestContext[Any]): RequestResult = {
		wait()
		RequestResult.Empty
	}
	
	
	// OTHER    -------------------------
	
	private def wait()(implicit context: RequestContext[Any]) = {
		val waitTime = context.request.parameters("wait").double match {
			case Some(customSeconds) => ((customSeconds max 0.0) min 20.0).seconds
			case None => 4.seconds
		}
		Wait(waitTime)
	}
}

package utopia.nexus.test

import utopia.access.http.Method
import utopia.access.http.Method.Get
import utopia.flow.async.process.Wait
import utopia.flow.collection.immutable.Single
import utopia.flow.time.TimeExtensions._
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.ResourceSearchResult.Redirected
import utopia.nexus.rest.{Context, Resource, ResourceSearchResult}

import scala.concurrent.ExecutionContext

/**
  * Delays request handling somewhat, and then forwards the request
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
class SlowNode(implicit exc: ExecutionContext) extends Resource[Context]
{
	// ATTRIBUTES   ----------------------
	
	override val name: String = "slow"
	
	
	// IMPLEMENTED  ----------------------
	
	override def allowedMethods: Iterable[Method] = Single(Get)
	
	override def follow(path: Path)(implicit context: Context): ResourceSearchResult[Context] = {
		// Waits X seconds and then redirects to the resource at the end of the remaining path
		wait()
		Redirected(path)
	}
	
	override def toResponse(remainingPath: Option[Path])(implicit context: Context): Response = {
		wait()
		Response.empty()
	}
	
	
	// OTHER    -------------------------
	
	private def wait()(implicit context: Context) = {
		val waitTime = context.request.parameters("wait").double match {
			case Some(customSeconds) => ((customSeconds max 0.0) min 20.0).seconds
			case None => 4.seconds
		}
		Wait(waitTime)
	}
}

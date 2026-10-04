package utopia.echo.model.request.vastai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Put
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.request.HttpEntityConvertible
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.util.{Failure, Success}

object StopOrStartInstance
{
	// OTHER    -----------------------
	
	/**
	 * @param instanceId ID of the targeted Vast AI instance
	 * @return A factory for constructing requests concerning that instance
	 */
	def apply(instanceId: Int) = StopOrStartInstanceRequestFactory(instanceId)
	
	
	// NESTED   -----------------------
	
	case class StopOrStartInstanceRequestFactory(instanceId: Int, deprecationView: View[Boolean] = AlwaysFalse)
	{
		// COMPUTED -------------------
		
		/**
		 * @return A new request for starting the targeted instance (unless running already)
		 */
		def start = new StopOrStartInstance(instanceId, deprecationView, start = true)
		/**
		 * @return A new request for stopping the targeted instance
		 */
		def stop = new StopOrStartInstance(instanceId, deprecationView, start = false)
		
		
		// OTHER    --------------------
		
		/**
		 * @param view A request-deprecation view to apply. If this view contains true, this request may be retracted.
		 * @return Copy of this factory using the specified deprecation view.
		 */
		def deprecatingIf(view: View[Boolean]) = copy(deprecationView = view)
	}
}

/**
 * An API request for stopping an instance.
 * Stopped instances incur lower costs than active / running instances.
 * @param instanceId ID of the targeted Vast AI instance
 * @param deprecationView A view that contains true if this request should be retracted (unless sent already)
 * @param start Whether to start the targeted instance.
 *              False (default) if stopping the targeted instance.
 * @author Mikko Hilpinen
 * @since 04.10.2026, v1.6
 */
class StopOrStartInstance(instanceId: Int, deprecationView: View[Boolean] = AlwaysFalse, start: Boolean = false)
	extends ApiRequest[Unit]
{
	// ATTRIBUTES   ---------------------
	
	override val method: Method = Put
	override val path: String = s"instances/$instanceId"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED	---------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def body: Either[Value, HttpEntityConvertible] =
		Left(Model.from("state" -> (if (start) "running" else "stopped")))
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Unit]] = prepared.parseValue { body =>
		body.model match {
			case Some(body) =>
				if (body("success").boolean.contains(false)) {
					val errorDetails = Pair("error", "msg").flatMap { body(_).string }.mkString(": ")
					Failure(new IllegalStateException(s"Failed to ${
						if (start) "start" else "stop" } instance #$instanceId${
						errorDetails.prependIfNotEmpty("; ") }"))
				}
				else
					Success(())
			case None =>
				Success(())
		}
	}
}

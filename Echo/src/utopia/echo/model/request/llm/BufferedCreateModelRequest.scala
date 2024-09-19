package utopia.echo.model.request.llm

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.llm.CreateModelRequest.CreateModelRequestFactory
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import java.nio.file.Path
import scala.annotation.unused
import scala.concurrent.Future
import scala.language.implicitConversions

object BufferedCreateModelRequest
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * Factory for constructing new requests
	  */
	lazy val factory = new BufferedCreateModelRequestFactory()
	
	
	// IMPLICIT -------------------------
	
	implicit def objectToFactory(@unused o: BufferedCreateModelRequest.type): BufferedCreateModelRequestFactory =
		factory
	
	
	// NESTED   -------------------------
	
	class BufferedCreateModelRequestFactory(override protected val deprecationCondition: Option[View[Boolean]] = None)
		extends CreateModelRequestFactory[BufferedCreateModelRequest, BufferedCreateModelRequestFactory]
	{
		override protected def apply(name: String, modelFile: Either[Path, String],
		                             deprecationView: View[Boolean]): BufferedCreateModelRequest =
			BufferedCreateModelRequest(name, modelFile, deprecationView)
		
		override def withDeprecationCondition(condition: View[Boolean]): BufferedCreateModelRequestFactory =
			new BufferedCreateModelRequestFactory(Some(condition))
	}
}

/**
  * A response for creating a new (local) Ollama model from a model-file.
  * Only returns one response.
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
case class BufferedCreateModelRequest(name: String, modelFile: Either[Path, String],
                                      deprecationView: View[Boolean] = AlwaysFalse)
	extends CreateModelRequest[String]
{
	// IMPLEMENTED  --------------------
	
	override def stream: Boolean = false
	override def deprecated: Boolean = deprecationView.value
	
	// WET WET (from PullWithoutStatusRequest)
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[String]] = prepared.getOne { model =>
		model("status").string.toTry {
			new NoSuchElementException(
				s"Response model didn't contain property \"status\". Available properties were: [${
					model.nonEmptyPropertiesIterator.map { _.name }.mkString(", ") }]")
		}
	}
}

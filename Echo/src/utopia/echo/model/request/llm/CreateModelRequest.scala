package utopia.echo.model.request.llm

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.http.request.Body
import utopia.echo.model.request.RetractableRequestFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import java.nio.file.Path
import scala.concurrent.ExecutionContext

object CreateModelRequest
{
	// COMPUTED --------------------------
	
	/**
	  * @return A factory for creating buffered create model -requests
	  */
	def buffered = BufferedCreateModelRequest.factory
	/**
	  * @param exc Implicit execution context used
	  * @param jsonParser Implicit json parser used
	  * @param log Implicit logging implementation used
	  * @return A factory for creating streaming create model -requests
	  */
	def streamed(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
		StreamedCreateModelRequest.factory
	
	
	// NESTED   --------------------------
	
	trait CreateModelRequestFactory[+R, +Repr] extends RetractableRequestFactory[Repr]
	{
		// ABSTRACT ----------------------
		
		/**
		  * @param name Name of the model to create
		  * @param modelFile Either
		  *                     - Right: Model-file contents as a string (recommended)
		  *                     - Left: Path to the model-file
		  * @param deprecationView A view that contains true when/if this request becomes deprecated
		  *                        and should be retracted (unless already sent).
		  * @return A new request
		  */
		protected def apply(name: String, modelFile: Either[Path, String], deprecationView: View[Boolean]): R
		
		
		// OTHER    ----------------------
		
		/**
		  * @param name Name of the model to create
		  * @param modelFile Either
		  *                     - Right: Model-file contents as a string (recommended)
		  *                     - Left: Path to the model-file
		  * @return A new request
		  */
		def apply(name: String, modelFile: Either[Path, String]): R =
			apply(name, modelFile, deprecationCondition.getOrElse(AlwaysFalse))
		
		/**
		  * @param name Name of the model to create
		  * @param modelFile Model-file contents as a string
		  * @return A new request
		  */
		def apply(name: String, modelFile: String): R = apply(name, Right(modelFile))
		/**
		  * @param name Name of the model to create
		  * @param modelFilePath Path to the model-file
		  * @return A new request
		  */
		def fromFile(name: String, modelFilePath: Path) = apply(name, Left(modelFilePath))
	}
}

/**
  * A request for creating a new model from a model-file
  * @tparam R Type of parsed server response
  * @author Mikko Hilpinen
  * @since 19.09.2024, v11.1
  */
trait CreateModelRequest[+R] extends ApiRequest[R]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Name of the model to create
	  */
	def name: String
	/**
	  * @return Either
	  *             - Right: Model-file contents as a string (recommended)
	  *             - Left: Path to the model-file
	  */
	def modelFile: Either[Path, String]
	/**
	  * @return Whether this request expects a streamed response.
	  *         If false, the response will only contain the final status.
	  */
	def stream: Boolean
	
	
	// IMPLEMENTED  ----------------------
	
	override def method: Method = Post
	override def path: String = "create"
	
	override def body: Either[Value, Body] = {
		val modelFileProp = modelFile match {
			case Right(str) => Constant("modelfile", str)
			case Left(path) => Constant("path", path.real.toJson)
		}
		Left(Model.from("name" -> name, "stream" -> stream) + modelFileProp)
	}
}
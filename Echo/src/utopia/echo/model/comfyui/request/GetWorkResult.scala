package utopia.echo.model.comfyui.request

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.disciple.model.error.RequestFailedException
import utopia.echo.model.comfyui.ComfyUiDir
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{StringType, VectorType}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import java.nio.file.Path
import scala.util.{Success, Try}

object GetWorkResult
{
	// OTHER    ------------------------
	
	/**
	 * @param outputNode The name of the workflow node which is used for saving the generated images
	 * @param dir Implicit path to the ComfyUI directory
	 * @return A result parser which yields the generated images
	 */
	def imagesExtractor(outputNode: String)(implicit dir: ComfyUiDir) =
		new ExtractGeneratedImagePaths(outputNode)
	
	/**
	 * Creates a request for querying for generated output images
	 * @param outputNode The name of the workflow node which is used for saving the generated images
	 * @param promptId ID of the executed / queried prompt
	 * @param deprecationView A view that, once it contains false, will cause this request to not be sent anymore.
	 *                        Default = always false.
	 * @param comfyUiDir Implicit path to the ComfyUI directory
	 * @return A new request for retrieving paths to the generated image files, once they're ready
	 */
	def generatedImages(outputNode: String, promptId: String, deprecationView: View[Boolean] = AlwaysFalse)
	                   (implicit comfyUiDir: ComfyUiDir): GetWorkResult[Seq[Path]] =
		new GetWorkResult(promptId, new ExtractGeneratedImagePaths(outputNode), deprecationView)
	
	
	// NESTED   ------------------------
	
	/**
	 * A response model parser for extracting paths to the generated images
	 * @param outputNode Name of the node that saved / saves the images
	 * @param dir Implicit ComfyUI directory
	 */
	class ExtractGeneratedImagePaths(outputNode: String)(implicit dir: ComfyUiDir)
		extends FromModelFactory[Seq[Path]]
	{
		// ATTRIBUTES   ----------------
		
		private lazy val schema = ModelDeclaration.empty.withChild(outputNode, ModelDeclaration("images" -> VectorType))
		private lazy val imageSchema = ModelDeclaration("filename" -> StringType)
		
		
		// IMPLEMENTED  ---------------
		
		override def apply(model: HasProperties): Try[Seq[Path]] = schema.validate(model).flatMap { model =>
			model(outputNode)("images").getVector.tryMap { image =>
				image.tryModel.flatMap(imageSchema.validate).map { image =>
					dir.path/s"output/${ image("subfolder").getString.appendIfNotEmpty("/") }${
						image("filename").getString }"
				}
			}
		}
	}
}

/**
 * A request used for querying for prompt completion output
 *
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
class GetWorkResult[+A](promptId: String, parser: FromModelFactory[A], deprecationView: View[Boolean] = AlwaysFalse)
	extends GetRequest[Option[A]]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val path: String = s"history/$promptId"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  -----------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest) = prepared.getOne { model =>
		// Before the query has completed, the response model will be empty
		model(promptId).model match {
			// Case: Response contains a prompt model => Checks the status
			case Some(model) =>
				model("status").model.filter { _("completed").getBoolean } match {
					// Case: Completed => Attempts to get and parse the outputs object
					case Some(status) =>
						model.tryGet("outputs") { _.tryModel }
							// If a status string is included, includes that in the error message
							.mapFailure { error =>
								status("status_str").string match {
									case Some(status) =>
										new RequestFailedException(s"Couldn't read prompt output; Status: $status",
											error)
									case None => error
								}
							}
							.flatMap(parser.apply).map { Some(_) }
					
					// Case: Still incomplete
					case None => Success(None)
				}
			// Case: No content => Incomplete
			case None => Success(None)
		}
	}
}

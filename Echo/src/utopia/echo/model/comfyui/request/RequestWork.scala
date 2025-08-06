package utopia.echo.model.comfyui.request

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.request.Body
import utopia.echo.model.comfyui.request.RequestWork.{ExtractPromptId, defaultClientId}
import utopia.echo.model.comfyui.workflow.node.WorkflowNode
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

object RequestWork
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * A random client ID used by default
	 */
	lazy val defaultClientId = UUID.randomUUID().toString
	
	
	// NESTED   --------------------------
	
	private object ExtractPromptId extends FromModelFactory[String]
	{
		override def apply(model: ModelLike[Property]): Try[String] = model("prompt_id").tryString
	}
}

/**
 * A request used for requesting the ComfyUI server to perform a workflow
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
class RequestWork(workflow: Iterable[WorkflowNode], clientId: String = defaultClientId,
                  deprecationView: View[Boolean] = AlwaysFalse)
	extends ApiRequest[String]
{
	// ATTRIBUTES   --------------------------
	
	override val method: Method = Post
	override val path: String = "prompt"
	
	
	// IMPLEMENTED  --------------------------
	
	// TODO: Remove test prints
	override def body: Either[Value, Body] = Left(Model.from(
		"prompt" -> Model.withConstants(workflow.map { _.toConstant }),
		"client_id" -> clientId
	))
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[String]] =
		prepared.getOne(ExtractPromptId)
}

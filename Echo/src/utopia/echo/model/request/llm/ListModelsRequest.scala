package utopia.echo.model.request.llm

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.llm.GeneralOllamaModelInfo
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.concurrent.Future
import scala.util.Try

/**
 * A request for listing the locally available models
 *
 * @author Mikko Hilpinen
 * @since 03.09.2024, v1.1
 */
object ListModelsRequest extends GetRequest[Seq[GeneralOllamaModelInfo]]
{
	// IMPLEMENTED  ------------------------
	
	override def path: String = "tags"
	override def deprecated: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[GeneralOllamaModelInfo]]] =
		prepared.getOne(ModelsParser)
		
	
	// NESTED   ---------------------------
	
	private object ModelsParser extends FromModelFactory[Vector[GeneralOllamaModelInfo]]
	{
		// IMPLEMENTED  -------------------
		
		override def apply(model: ModelLike[Property]): Try[Vector[GeneralOllamaModelInfo]] =
			model("models").tryVectorWith { v => v.tryModel.flatMap(GeneralOllamaModelInfo.apply) }
	}
}

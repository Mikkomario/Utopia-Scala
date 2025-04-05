package utopia.echo.model.response.openai

import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.template.{ModelLike, Property}

object OpenAiError extends SureFromModelFactory[OpenAiError]
{
	override def parseFrom(model: ModelLike[Property]): OpenAiError =
		apply(model("code").getString, model("message").getString)
}

/**
  * Contains information about an error returned by Open AI
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
case class OpenAiError(code: String = "", message: String = "")

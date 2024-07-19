package utopia.echo.model.response

import utopia.echo.model.LlmDesignator
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.Future

/**
  * Represents a reply from an LLM
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  * @param model Model used in generating this response
  * @param textPointer A pointer which contains the reply message as text.
  *                    Will stop changing once this reply has been fully generated / read.
  * @param statisticsFuture A future that resolves into statistics about this response,
  *                         once this response has been fully generated.
  */
case class Reply(model: LlmDesignator, textPointer: Changing[String], statisticsFuture: Future[ResponseStatistics])

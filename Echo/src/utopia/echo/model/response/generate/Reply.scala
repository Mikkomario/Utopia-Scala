package utopia.echo.model.response.generate

import utopia.echo.model.response.OllamaResponse

/**
  * Common trait / interface for LLM replies, whether they're streamed or buffered
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait Reply extends OllamaResponse[BufferedReply]

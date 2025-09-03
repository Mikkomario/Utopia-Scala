package utopia.echo.model.response.ollama.generate

import utopia.echo.model.response.ollama.{OllamaResponse, OllamaResponseLike}

/**
  * Common trait / interface for LLM replies, whether they're streamed or buffered
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
@deprecated("Deprecated for removal. Replaced with OllamaResponse.", "v1.4")
trait Reply extends OllamaResponse with OllamaResponseLike[BufferedReply]

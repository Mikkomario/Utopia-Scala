package utopia.echo.model.response

import scala.concurrent.duration.FiniteDuration

/**
  * Contains statistics about response & generation durations
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  * @param total Total duration spent handling the request
  * @param load Duration spent loading the LLM
  * @param evaluation Duration spent evaluating the prompt
  */
case class GenerationDurations(total: FiniteDuration, load: FiniteDuration, evaluation: FiniteDuration)
package utopia.echo.model.response

import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.TimeExtensions._

import scala.concurrent.duration.{Duration, FiniteDuration}

object GenerationDurations
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * A set of durations where all values are set to 0
	  */
	val zero = apply(Duration.Zero, Duration.Zero, Duration.Zero)
	
	
	// OTHER    -----------------------------
	
	/**
	  * Parses a response json object returned by the Ollama API into a set of durations.
	  * This should only be called for the final response (where "done" is set to true)
	  * @param responseModel A response model returned by the Ollama API
	  * @return Parsed durations
	  */
	// According to the API docs, all durations are specified in nanoseconds
	def fromOllamaResponse(responseModel: Model) = apply(
		responseModel("total_duration").getLong.nanos,
		responseModel("load_duration").getLong.nanos,
		responseModel("prompt_eval_duration").getLong.nanos)
}

/**
  * Contains statistics about response & generation durations
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  * @param total Total duration spent handling the request
  * @param load Duration spent loading the LLM
  * @param evaluation Duration spent evaluating the prompt
  */
case class GenerationDurations(total: FiniteDuration, load: FiniteDuration, evaluation: FiniteDuration)
{
	override def toString =
		s"loaded ${ load.description }, evaluated ${ evaluation.description }, generated ${
			(total - evaluation - load).description }, total ${ total.description }"
}
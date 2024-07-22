package utopia.echo.model.response

import utopia.echo.model.request.generate.GenerateBufferedOrStreamed
import utopia.flow.generic.model.immutable.{Model, Value}

object ResponseStatistics
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty set of statistics
	  */
	val empty = apply(Value.empty, GenerationDurations.zero, 0, 0)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Parses a response json object returned by the Ollama API into a set of statistics.
	  * Should only be called for the final response models, where "done" is set to true.
	  * @param responseModel A json model returned by the Ollama API
	  * @return Parsed statistics from the specified model
	  */
	def fromOllamaResponse(responseModel: Model) = ResponseStatistics(
		responseModel("context"),
		GenerationDurations.fromOllamaResponse(responseModel),
		responseModel("prompt_eval_count").getInt,
		responseModel("eval_count").getInt)
}

/**
  * Contains statistical information about an LLM response.
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  * @param context A value which represents the conversation context so far.
  *                May be passed to the next outbound [[GenerateBufferedOrStreamed]].
  * @param duration Information about the duration it took to generate the response
  * @param promptTokenCount Amount of tokens used in the prompt
  * @param responseTokenCount Amount of tokens used in the response
  */
case class ResponseStatistics(context: Value, duration: GenerationDurations,
                              promptTokenCount: Int, responseTokenCount: Int)
{
	override def toString = s"Tokens: $promptTokenCount + $responseTokenCount; Durations: $duration"
}
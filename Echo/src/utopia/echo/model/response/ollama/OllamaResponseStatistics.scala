package utopia.echo.model.response.ollama

import utopia.echo.model.response.TokenUsage
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.LinearScalable

object OllamaResponseStatistics
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty set of statistics
	  */
	val empty = apply(Value.empty, GenerationDurations.zero, TokenUsage.zero)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Parses a response json object returned by the Ollama API into a set of statistics.
	  * Should only be called for the final response models, where "done" is set to true.
	  * @param responseModel A json model returned by the Ollama API
	  * @return Parsed statistics from the specified model
	  */
	// TODO: Add support for done_reason ("done": true, "done_reason": "stop")
	def fromOllamaResponse(responseModel: HasProperties) = OllamaResponseStatistics(
		responseModel("context"),
		GenerationDurations.fromOllamaResponse(responseModel),
		TokenUsage(responseModel("prompt_eval_count").getInt, responseModel("eval_count").getInt))
}

/**
  * Contains statistical information about an LLM response.
 *
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  * @param context A value which represents the conversation context so far.
  *                May be passed to the next outbound [[utopia.echo.model.request.ollama.generate.GenerateRequest]].
  * @param duration Information about the duration it took to generate the response
  * @param tokenUsage Number of tokens used in input & output
  */
case class OllamaResponseStatistics(context: Value, duration: GenerationDurations, tokenUsage: TokenUsage)
	extends SelfCombinable[OllamaResponseStatistics] with LinearScalable[OllamaResponseStatistics]
{
	// COMPUTED ---------------------------
	
	@deprecated("Please use tokenUsage.input instead", "v1.4")
	def promptTokenCount: Int = tokenUsage.input
	@deprecated("Please use tokenUsage.output instead", "v1.4")
	def responseTokenCount: Int = tokenUsage.output
	
	/**
	  * @return Total number of tokens used within request context.
	  *         Contains both prompt and reply tokens.
	  */
	@deprecated("Please use tokenUsage.total instead", "v1.4")
	def contextTokenCount = tokenUsage.total
	
	
	// IMPLEMENTED  -----------------------
	
	override def self: OllamaResponseStatistics = this
	
	override def toString = s"Tokens: $tokenUsage; Durations: $duration"
	
	override def +(other: OllamaResponseStatistics): OllamaResponseStatistics = OllamaResponseStatistics(
		context = other.context.nonEmptyOrElse(context), duration = duration + other.duration,
		tokenUsage = tokenUsage + other.tokenUsage)
	
	override def *(mod: Double): OllamaResponseStatistics =
		copy(duration = duration * mod, tokenUsage = tokenUsage * mod)
}
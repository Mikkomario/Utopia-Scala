package utopia.echo.model.response

import utopia.flow.generic.model.immutable.Value

/**
  * Contains statistical information about an LLM response.
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  * @param context A value which represents the conversation context so far.
  *                May be passed to the next outbound [[utopia.echo.controller.GenerateRequest]].
  * @param duration Information about the duration it took to generate the response
  * @param promptTokenCount Amount of tokens used in the prompt
  * @param responseTokenCount Amount of tokens used in the response
  */
case class ResponseStatistics(context: Value, duration: GenerationDurations,
                              promptTokenCount: Int, responseTokenCount: Int)

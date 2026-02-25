package utopia.echo.controller

import utopia.annex.controller.PreparingResponseParser
import utopia.disciple.controller.parse.ResponseParser
import utopia.annex.util.ResponseParseExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

/**
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
package object client
{
	/**
	 * @param jsonParser JSON parser used
	 * @return A new response parser that parses values
	 */
	def newValueResponseParser(implicit jsonParser: JsonParser) =
		ResponseParser.value.unwrapToResponse { v => v("error", "message", "msg").stringOr(v.getString) }
	/**
	 * @param log Implicit logging implementation used
	 * @return A new response parser that only parses response bodies on failure
	 */
	def newEmptyResponseParser(implicit log: Logger) =
		PreparingResponseParser.onlyRecordFailures(ResponseParser.stringOrLog)
}

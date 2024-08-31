package utopia.echo.model.request.generate

import utopia.echo.model.LlmDesignator
import utopia.flow.operator.equality.ComparisonOperator.{DirectionalComparison, Equality}
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.util.UncertainNumber.{AnyNumber, CertainNumber, NumberComparison, UncertainInt, UncertainNumberRange}
import utopia.flow.util.StringExtensions._

import scala.annotation.tailrec
import scala.language.implicitConversions

object Query
{
	// Queries can be implicitly converted to request parameters
	implicit def queryToParams(q: Query)(implicit llm: LlmDesignator): GenerateParams = GenerateParams(q)
}

/**
  * Represents some type of query made to the Ollama (or other LLM API) generate or chat end-point.
  * Contains information about the prompt, as well as instructions for the LLM on how to respond.
  * @param prompt The primary prompt (text)
  * @param responseSchema Instructions on how json responses should be built
  *                       (default = empty = no instructions given or no json format expected)
  * @param numberOfExpectedResponses The number of responses / values that should be received for this query.
  *                                  Default = 1.
  *                                  If set to something other than 1, the LLM is requested to return
  *                                  a json value (or object) array.
  * @param requestJson Whether to specifically request the LLM to reply in JSON
  *                    (and to consequently expect json formatting).
  *                    Default = false.
  *                    This property is ignored if the response needs to be in JSON format anyway
  *                    because of the specified 'responseSchema' or 'numberOfExpectedResponses' value.
  * @author Mikko Hilpinen
  * @since 11.07.2024, v1.0
  */
case class Query(private val prompt: Prompt, responseSchema: ObjectSchema = ObjectSchema.empty,
                 numberOfExpectedResponses: UncertainInt = 1, requestJson: Boolean = false)
{
	// COMPUTED ----------------------------
	
	/**
	  * @return Whether the response to this query is expected (and requested) to consist of valid JSON.
	  */
	def expectsJsonResponse =
		requestJson || responseSchema.nonEmpty || numberOfExpectedResponses != CertainNumber(1)
	
	/**
	  * @return Images to send along with this query. As Base 64 encoded strings.
	  */
	def encodedImages = prompt.encodedImages
	
	/**
	  * @return Prompt that should be sent out to the LLM
	  */
	def toPrompt = {
		val builder = new StringBuilder()
		
		// Specifies the question's context, if appropriate
		// If a custom system message is applied, includes the context there rather than here
		if (prompt.systemMessage.isEmpty && prompt.context.nonEmpty)
			builder ++= s"Context: ${prompt.context}\n\nPrompt: "
		builder ++= prompt.text
		
		// Case: Expecting a string response (or a json array with multiple strings)
		if (responseSchema.isEmpty)
			numberOfExpectedResponses match {
				// Case: Exactly one string expected (normal chat use-case)
				case e: CertainNumber[Int] if e.value <= 1 =>
					if (requestJson)
						builder ++= "\n\nRespond in JSON only"
				// Case: Expects a json array of strings
				case n => builder ++= s"\n\nReply with ${ _expectedValuesCountString(n, "string") }"
			}
		// Case: Expects the LLM to fill json object properties
		else {
			builder ++= "\n\n"
			numberOfExpectedResponses match {
				// Case: Filling a single object
				case e: CertainNumber[Int] if e.value <= 1 =>
					builder ++= s"Reply with the following JSON object, replacing the values with your responses: $responseSchema"
				// Case: Filling multiple objects
				case n =>
					builder ++= s"\n\nReply with ${
						_expectedValuesCountString(n, "JSON object") }, where each object matches the following object, with values replaced with your responses: $responseSchema"
			}
		}
		
		// Queries with multiple lines are wrapped in triple quotes
		val query = builder.result()
		if (query.isMultiLine)
			s"\"\"\"$query\"\"\""
		else
			query
	}
	
	/**
	  * @return System message that should override the message defined in the LLM's Modelfile.
	  *         Empty if system message should not be overwritten.
	  */
	def toSystem = prompt.systemMessage.mapIfNotEmpty { system =>
		if (prompt.context.nonEmpty)
			s"\"\"\"$system\n\n${prompt.context}\"\"\""
		else if (system.isMultiLine)
			s"\"\"\"$system\"\"\""
		else
			system
	}
	
	/**
	  * @param llm Implicit LLM to target
	  * @return Request parameters for sending out this query
	  */
	def toRequestParams(implicit llm: LlmDesignator) = GenerateParams(this)
	
	
	// OTHER    ----------------------------
	
	@tailrec
	private def _expectedValuesCountString(n: UncertainInt, typeStr: String): String = n match {
		case CertainNumber(n) =>
			if (n == 1)
				s"a $typeStr"
			else
				s"a JSON array containing exactly $n ${typeStr}s"
		case AnyNumber() => s"a JSON array of ${typeStr}s"
		case UncertainNumberRange(range) =>
			if (range.ends.isSymmetric)
				_expectedValuesCountString(range.start, typeStr)
			else
				s"a JSON array containing ${range.start}-${range.end} ${typeStr}s"
		case NumberComparison(threshold, operator) =>
			operator match {
				case DirectionalComparison(dir, allowEqual) =>
					val dirStr = dir match {
						case Positive => if (allowEqual) "at least" else "more than"
						case Negative => if (allowEqual) "at most" else "less than"
					}
					s"a JSON array containing $dirStr $threshold ${typeStr}s"
				case Equality => _expectedValuesCountString(threshold, typeStr)
				case _ => s"a JSON array containing $n ${typeStr}s"
			}
		case e => s"$e ${typeStr}s"
	}
}
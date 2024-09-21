package utopia.echo.model.response.llm

import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.{ModelType, StringType}
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.util.StringExtensions._

import scala.util.Try

object ModelShowInfo extends FromModelFactory[ModelShowInfo]
{
	// ATTRIBUTES   --------------------
	
	private lazy val schema = ModelDeclaration("modelfile" -> StringType, "details" -> ModelType)
	
	
	// IMPLEMENTED  --------------------
	
	override def apply(model: ModelLike[Property]): Try[ModelShowInfo] = schema.validate(model).flatMap { model =>
		OllamaModelDetails(model("details").getModel).map { details =>
			apply(model("modelfile"), details, parseParameters(model("parameters")), model("template"),
				model("model_info").getModel)
		}
	}
	
	
	// OTHER    -----------------------
	
	// Each line contains one parameter definition
	// Some parameters, such as stop, may have multiple values, each defined on their own line
	private def parseParameters(paramsString: String) =
		paramsString.linesIterator.map { line => line.trim.splitAtFirst(" ").map { _.trim } }.toOptimizedSeq
			// Since model files are case-insensitive, makes sure the parameters are in lower case
			.groupMap { _.first.toLowerCase } { _.second }
			.flatMap { case (key, values) =>
				// Ignores parameters that don't match any ModelParameter
				ModelParameter.findForKey(key).map { param =>
					val value: Value = values.oneOrMany match {
						case Left(only) => only
						case Right(many) => many
					}
					param -> value
				}
			}
}

/**
  * Contains information returned by Ollama show.
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  *
  * @constructor Creates a new model info model
  * @param modelFile Full contents of the model's model-file, as a String
  * @param details Details about this model
  * @param parameters Specified model (default) parameters as a Map
  * @param template Model prompt template as specified in the model-file
  * @param extraInfo Additional model information as a model (keys may be model-specific)
  */
case class ModelShowInfo(modelFile: String, details: OllamaModelDetails = OllamaModelDetails(),
                         parameters: Map[ModelParameter, Value] = Map(), template: String = "",
                         extraInfo: Model = Model.empty)
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * System message specified in the model-file
	  */
	lazy val systemMessage = {
		val linesIter = modelFile.linesIterator.map { _.trim }
		// Finds the SYSTEM line
		linesIter.find { _.startsWithIgnoreCase("SYSTEM") } match {
			case Some(messageLine) =>
				val firstLine = messageLine.drop(6).dropWhile { _ == ' ' }
				
				// Checks whether this SYSTEM message is multi-line (i.e. starts with ")
				// Case: Multi-line => Parses until "
				if (firstLine.startsWith("\"") && (firstLine.length == 1 || !firstLine.endsWith("\""))) {
					val moreLines = linesIter.collectTo { _.endsWith("\"") }
					((firstLine.dropWhile {  _ == '\"'} +: moreLines.dropRight(1)) ++
						moreLines.lastOption.map { _.dropRightWhile { _ == '\"' } }.filterNot { _.isEmpty })
						.dropWhile { _.isEmpty }
						.mkString("\n")
				}
				// Case: Single line => Returns this line
				else
					firstLine
				
			// Case: No system message specified
			case None => ""
		}
	}
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = s"Model-file: $modelFile\nDetails: $details\nParameters: ${
		parameters.map { case (k, v) => s"$k = $v" }.mkString(", ") }\nTemplate: $template\nMore: $extraInfo"
}
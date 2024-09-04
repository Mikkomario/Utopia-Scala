package utopia.echo.model.response.llm

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.util.StringExtensions._

object OllamaModelDetails extends SureFromModelFactory[OllamaModelDetails]
{
	override def parseFrom(model: ModelLike[Property]): OllamaModelDetails = {
		val family = model("family").getString
		val families = model("families").vector match {
			case Some(familiesVector) => familiesVector.map { _.getString }
			case None => family.ifNotEmpty.emptyOrSingle
		}
		val parameterSize = model("parameter_size").string.flatMap { _.dropRightWhile { _.toUpper == 'B' }.double }
		
		apply(model("format"), family, families, parameterSize, model("quantization_level"), model("parent_model"))
	}
}

/**
 * Contains specific details about an Ollama model
 *
 * @param format Format in which this model is stored
 * @param family The primary family of models this one belongs to
 * @param families Families to which this model belongs to
 * @param parameterSize Parameter size of this model, in billions of parameters, if known.
 * @param quantizationLevel The quantization level of this model as a string. Empty if not available.
 * @param parentName Name of this model's direct parent. Empty if not applicable or unknown.
 * @author Mikko Hilpinen
 * @since 03.09.2024, v1.1
 */
case class OllamaModelDetails(format: String = "", family: String = "", families: Seq[String] = Empty,
                              parameterSize: Option[Double] = None,
                              quantizationLevel: String = "", parentName: String = "")
	extends ModelConvertible
{
	override def toModel: Model = {
		val parameterSizeStr = parameterSize match {
			case Some(ps) => if (ps % 1 == 0) s"${ ps.toInt }B" else s"${ps}B"
			case None => ""
		}
		Model.from("format" -> format, "family" -> family, "families" -> families,
			"parameter_size" -> parameterSizeStr, "quantization_level" -> quantizationLevel,
			"parent_model" -> parentName)
	}
}
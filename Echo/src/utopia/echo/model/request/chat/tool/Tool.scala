package utopia.echo.model.request.chat.tool

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.template.ModelConvertible

/**
  * Common trait for tools which certain LLMs may use in their response-producing logic.
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
trait Tool extends ModelConvertible
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Name of this tool / function
	  */
	def name: String
	/**
	  * @return Description of this tool / function. Presented to the LLM.
	  */
	def description: String
	
	/**
	  * @return Input parameters accepted by this tool
	  */
	def parameters: Seq[ToolParameter]
	
	/**
	  * @param args Input arguments
	  * @return String to return back to the LLM
	  */
	def apply(args: Model): String
	
	
	// IMPLEMENTED  -----------------------
	
	override def toModel: Model = {
		val paramsPart: Value = parameters.notEmpty match {
			case Some(params) =>
				Model.from(
					"type" -> "object",
					"properties" -> Model.withConstants(params.map { _.toConstant }),
					"required" -> params.view.filter { _.required }.map { _.name }.toVector
				)
			case None => Value.empty
		}
		Model.from(
			"type" -> "function",
			"function" -> Model.from(
				"name" -> "name",
				"description" -> description,
				"parameters" -> paramsPart
			).withoutEmptyValues
		)
	}
}

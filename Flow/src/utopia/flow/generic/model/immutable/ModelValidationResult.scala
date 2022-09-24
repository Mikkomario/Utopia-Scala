package utopia.flow.generic.model.immutable

import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.Property

import scala.util.{Failure, Success}

object ModelValidationResult
{
	/**
	 *  @param original Original model
	  * @param missingDeclarations Declarations that were missing or empty
	  * @return A result for missing properties
	  */
	def missing(original: template.Model[Property], missingDeclarations: Set[PropertyDeclaration]) =
		ModelValidationResult(original, missingProperties = missingDeclarations)
	/**
	 *  @param original Original model
	  * @param failedConversions Conversions that failed (original attribute -> desired data type)
	  * @return A result for cast failure
	  */
	def castFailed(original: template.Model[Property], failedConversions: Set[(Constant, DataType)]) =
		ModelValidationResult(original, invalidConversions = failedConversions)
	/**
	 * @param original Original model
	 * @param missingChildren Expected child model declarations that couldn't be found from the model
	 * @return A failure result
	 */
	def missingChildren(original: template.Model[Property], missingChildren: Map[String, ModelDeclaration]) =
		ModelValidationResult(original, missingChildren = missingChildren)
	
	/**
	 *  @param original Original model
	  * @param validated Successfully validated model
	  * @return A result for successful validation
	  */
	def success(original: template.Model[Property], validated: Model) =
		ModelValidationResult(original, success = Some(validated))
}

/**
  * A simple struct that contains model validation results (model on success or missing information on failure)
  * @author Mikko Hilpinen
  * @since 16.7.2019, v1.6+
  * @param success The successfully validated model. None if validation wasn't successful.
  * @param missingProperties The properties that were missing from the model
  * @param invalidConversions Properties that failed to convert to desired data type
 *  @param missingChildren Missing declared child declarations
  */
case class ModelValidationResult private(original: template.Model[Property], success: Option[Model] = None,
                                         missingProperties: Set[PropertyDeclaration] = Set(),
                                         invalidConversions: Set[(Constant, DataType)] = Set(),
                                         missingChildren: Map[String, ModelDeclaration] = Map())
{
	// COMPUTED	-----------------
	
	/**
	  * @return Whether the validation was a success
	  */
	def isSuccess = success.isDefined
	/**
	  * @return Whether validation failed
	  */
	def isFailure = !isSuccess
	
	/**
	  * @return Names of missing properties
	  */
	def missingPropertyNames = missingProperties.map { _.name } ++ missingChildren.keySet
	
	/**
	  * @return This result converted to a try
	  */
	def toTry = success match
	{
		case Some(model) => Success(model)
		case None => Failure(new ModelValidationFailedException(toString))
	}
	
	
	// IMPLEMENTED	------------
	
	override def toString =
	{
		if (isSuccess)
			success.get.toString
		else if (missingProperties.nonEmpty)
			s"Missing properties: ${missingProperties.map { a => s"'${a.name}'" }.mkString(", ")}. Searching from: ${
				original.toJson}"
		else if (invalidConversions.nonEmpty)
			s"Couldn't convert: ${invalidConversions.map { case (prop, target) => s"$prop -> $target" }.mkString(", ")}"
		else if (missingChildren.nonEmpty)
			s"Missing child properties: ${missingChildren.keys.toVector.sorted.mkString(", ")}. Searching from ${
				original.toJson}"
		else
			"Validation failed"
	}
}

/**
  * Thrown when model validation fails
  * @param message Message that describes exception cause
  */
class ModelValidationFailedException(message: String) extends Exception(message)

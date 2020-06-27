package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.DataType

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
	 *  @param original Original model
	  * @param validated Successfully validated model
	  * @return A result for successful validation
	  */
	def success(original: template.Model[Property], validated: Model[Constant]) =
		ModelValidationResult(original, success = Some(validated))
}

/**
  * A simple struct that contains model validation results (model on success or missing information on failure)
  * @author Mikko Hilpinen
  * @since 16.7.2019, v1.6+
  * @param success The successfully validated model. None if validation wasn't successful.
  * @param missingProperties The properties that were missing from the model
  * @param invalidConversions Properties that failed to convert to desired data type
  */
case class ModelValidationResult private(original: template.Model[Property], success: Option[Model[Constant]] = None,
                                         missingProperties: Set[PropertyDeclaration] = Set(),
                                         invalidConversions: Set[(Constant, DataType)] = Set())
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
	def missingPropertyNames = missingProperties.map { _.name }
	
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
		else
			s"Couldn't convert: ${invalidConversions.map { case (prop, target) => s"$prop -> $target" }.mkString(", ")}"
	}
}

/**
  * Thrown when model validation fails
  * @param message Message that describes exception cause
  */
class ModelValidationFailedException(message: String) extends Exception(message)

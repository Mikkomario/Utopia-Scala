package utopia.flow.datastructure.immutable

import utopia.flow.generic.DataType

import scala.util.{Failure, Success}

object ModelValidationResult
{
	/**
	  * @param missingDeclarations Declarations that were missing or empty
	  * @return A result for missing properties
	  */
	def missing(missingDeclarations: Set[PropertyDeclaration]) = ModelValidationResult(missingProperties = missingDeclarations)
	
	/**
	  * @param failedConversions Conversions that failed (original attribute -> desired data type)
	  * @return A result for cast failure
	  */
	def castFailed(failedConversions: Set[(Constant, DataType)]) = ModelValidationResult(invalidConversions = failedConversions)
	
	/**
	  * @param model Successfully validated model
	  * @return A result for successful validation
	  */
	def success(model: Model[Constant]) = ModelValidationResult(success = Some(model))
}

/**
  * A simple struct that contains model validation results (model on success or missing information on failure)
  * @author Mikko Hilpinen
  * @since 16.7.2019, v1.6+
  * @param success The successfully validated model. None if validation wasn't successful.
  * @param missingProperties The properties that were missing from the model
  * @param invalidConversions Properties that failed to convert to desired data type
  */
case class ModelValidationResult private(success: Option[Model[Constant]] = None,
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
			s"Missing properties: ${missingProperties.map { _.name }.mkString(", ")}"
		else
			s"Couldn't convert: ${invalidConversions.map { case (prop, target) => s"$prop -> $target" }.mkString(", ")}"
	}
}

/**
  * Thrown when model validation fails
  * @param message Message that describes exception cause
  */
class ModelValidationFailedException(message: String) extends Exception(message)

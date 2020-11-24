package utopia.exodus.database.factory.user

import java.time.Instant

import utopia.exodus.database.Tables
import utopia.exodus.database.model.user.EmailValidationModel
import utopia.exodus.model.partial.EmailValidationData
import utopia.exodus.model.stored.EmailValidation
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.model.enumeration.ComparisonOperator.Larger
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading email validation data from the DB
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
object EmailValidationFactory extends FromValidatedRowModelFactory[EmailValidation] with Deprecatable
{
	// ATTRIBUTES	------------------------
	
	/**
	  * A condition that only returns validations which have not been answered
	  */
	lazy val notActualizedCondition = table("actualizedIn").isNull
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Model used in database altering functions and condition formation
	  */
	def model = EmailValidationModel
	
	
	// IMPLEMENTED	------------------------
	
	override protected def fromValidatedModel(model: Model[Constant]) = EmailValidation(model("id"),
		EmailValidationData(model("purposeId"), model("email"), model("key"), model("resendKey"),
			model("expiresIn"), model("created"), model("actualizedIn")))
	
	override def table = Tables.emailValidation
	
	override def nonDeprecatedCondition = model.withExpiration(Instant.now()).toConditionWithOperator(Larger) &&
		notActualizedCondition
}

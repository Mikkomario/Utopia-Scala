package utopia.exodus.database.access.single.auth

import utopia.exodus.database.model.auth.EmailValidationResendModel
import utopia.exodus.model.error.TooManyRequestsException
import utopia.exodus.model.partial.auth.EmailValidationResendData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.vault.sql.{Count, Where}

import scala.util.{Failure, Success}

/**
  * An access point to individual EmailValidationAttempts, based on their id
  * @since 2021-10-25
  */
case class DbSingleEmailValidationAttempt(id: Int) 
	extends UniqueEmailValidationAttemptAccess with SingleIntIdModelAccess[EmailValidationAttempt]
{
	private def resendModel = EmailValidationResendModel
	
	/**
	  * Records a validation message resend for this email validation case
	  * @param maxResends Maximum number of resends allowed per a single case (default = 5)
	  * @param connection DB Connection (implicit)
	  * @return The current number of resends for this email validation attempt. Failure if the maximum number of
	  *         resends would have been exceeded
	  */
	def tryRecordResend(maxResends: Int = 5)(implicit connection: Connection) =
	{
		// Counts the current number of resends first to make sure a new resend is allowed
		val existingResendCount = connection(Count(resendModel.table) +
			Where(resendModel.withValidationId(id))).firstValue.getInt
		if (existingResendCount < maxResends)
		{
			resendModel.insert(EmailValidationResendData(id))
			Success(existingResendCount + 1)
		}
		else
			Failure(new TooManyRequestsException("Maximum number of email validation resends exceeded"))
	}
}
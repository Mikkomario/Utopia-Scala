package utopia.exodus.model.partial

import utopia.flow.time.Now

import java.time.Instant

/**
  * Contains information about an email validation attempt (email sent to the user in order to authorize them)
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
@deprecated("Replaced with EmailValidationAttemptData", "v3.0")
case class EmailValidationData(purposeId: Int, email: String, key: String, resendKey: String, expiration: Instant,
							   ownerId: Option[Int] = None, created: Instant = Now,
							   actualized: Option[Instant] = None)
{
	/**
	  * @return Whether this validation was actually answered
	  */
	def wasActualized = actualized.nonEmpty
}

package utopia.exodus.rest.resource.email

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, NotImplemented}
import utopia.exodus.database.access.many.DbEmailValidations
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.UserCreation
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.{EmailValidator, ExodusContext}
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.rest.Resource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A node used for performing email validations
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1
  */
object EmailsNode extends Resource[AuthorizedContext]
{
	// ATTRIBUTES	---------------------------
	
	private val defaultSupportedMethods = Vector[Method](Post)
	
	override val name = "emails"
	
	override def allowedMethods =
		if (ExodusContext.isEmailValidationSupported) defaultSupportedMethods else Vector()
	
	// POST used for validating new email addresses
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Email validation must be implemented
		ExodusContext.emailValidator match
		{
			case Some(validator) =>
				// Requests must be authorized with an API-key
				context.apiKeyAuthorized { (_, connection) =>
					// The request body must contain either a json object with "email" property,
					// or the email address as a string or value
					context.handleValuePost { body =>
						body.model.flatMap { _("email").string }.orElse(body.string).filterNot { _.isEmpty } match
						{
							case Some(email) =>
								implicit val c: Connection = connection
								implicit val v: EmailValidator = validator
								// Sends an email validation, if possible
								DbEmailValidations.place(email, UserCreation.id) match
								{
									// On success returns the resend key
									case Right(newValidation) => Result.Success(newValidation.resendKey)
									case Left((status, message)) => Result.Failure(status, message)
								}
							case None =>
								Result.Failure(BadRequest,
									"Please provide a json object body with property 'email' or pass the email address as the request body")
						}
					}
				}
			case None => Result.Failure(NotImplemented, "Email validation features are not implemented").toResponse
		}
	}
	
	// TODO: Implement other validation features
	override def follow(path: Path)(implicit context: AuthorizedContext) =
		Error(NotImplemented, Some("Specific email validation nodes haven't been implemented yet"))
}

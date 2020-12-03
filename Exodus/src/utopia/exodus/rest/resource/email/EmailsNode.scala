package utopia.exodus.rest.resource.email

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound, NotImplemented}
import utopia.exodus.database.access.id.DbUserId
import utopia.exodus.database.access.many.{DbEmailValidations, DbUsers}
import utopia.exodus.model.enumeration.StandardEmailValidationPurpose.{PasswordReset, UserCreation}
import utopia.exodus.rest.resource.{CustomAuthorizationResourceFactory, ResourceWithChildren}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.{EmailValidator, ExodusContext}
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object EmailsNode extends CustomAuthorizationResourceFactory[EmailsNode]
{
	override def apply(authorize: (AuthorizedContext, Connection => Result) => Response) = new EmailsNode(authorize)
}

/**
  * A node used for performing email validations
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1
  */
class EmailsNode(authorize: (AuthorizedContext, Connection => Result) => Response)
	extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES	---------------------------
	
	private val defaultSupportedMethods = Vector[Method](Post)
	
	override val name = "emails"
	
	override val children = Vector(EmailResendsNode, PasswordResetNode)
	
	
	// IMPLEMENTED	---------------------------
	
	override def allowedMethods =
		if (ExodusContext.isEmailValidationSupported) defaultSupportedMethods else Vector()
	
	// POST used for validating new email addresses
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		handleRequest { (email, validator, connection) =>
			sendEmailValidationForNewUser(email)(connection, validator)
		}
	}
	
	
	// OTHER	------------------------------
	
	// Parameter function takes in an email address
	private def handleRequest(f: (String, EmailValidator, Connection) => Result)(implicit context: AuthorizedContext) =
	{
		// Email validation must be implemented
		ExodusContext.emailValidator match
		{
			case Some(validator) =>
				// Authorizes the request using specified authorization method
				authorize(context, c => handleEmailUsing(f)(context, c, validator))
			case None => Result.Failure(NotImplemented, "Email validation features are not implemented").toResponse
		}
	}
	
	private def handleEmailUsing(f: (String, EmailValidator, Connection) => Result)
								(implicit context: AuthorizedContext, connection: Connection, validator: EmailValidator) =
	{
		// The request body must contain either a json object with "email" property,
		// or the email address as a string or value
		context.handleValuePost { body =>
			body.model.flatMap { _("email").string }.orElse(body.string)
				.map { _.trim.toLowerCase }.filterNot { _.isEmpty } match
			{
				case Some(email) => f(email, validator, connection)
				case None =>
					Result.Failure(BadRequest,
						"Please provide a json object body with property 'email' or pass the email address as the request body")
			}
		}
	}
	
	private def sendEmailValidationForNewUser(email: String)(implicit connection: Connection, validator: EmailValidator) =
	{
		if (DbUsers.existsUserWithEmail(email))
			Result.Failure(Forbidden, "Specified email address is already in use")
		else
		{
			// Sends an email validation, if possible
			DbEmailValidations.place(email, UserCreation.id) match
			{
				// On success returns the resend key
				case Right(newValidation) => Result.Success(newValidation.resendKey)
				case Left((status, message)) => Result.Failure(status, message)
			}
		}
	}
	
	
	// NESTED	-----------------------------
	
	private object PasswordResetNode extends Resource[AuthorizedContext]
	{
		// ATTRIBUTES	---------------------
		
		override val name = "password-resets"
		
		
		// IMPLEMENTED	---------------------
		
		override def allowedMethods = EmailsNode.this.allowedMethods
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		{
			handleRequest { (email, validator, connection) =>
				sendValidationForPasswordRecovery(email)(connection, validator)
			}
		}
		
		override def follow(path: Path)(implicit context: AuthorizedContext) =
			Error(message = Some(s"$name doesn't have any child nodes"))
		
		
		// OTHER	-------------------------
		
		private def sendValidationForPasswordRecovery(email: String)
													 (implicit connection: Connection, validator: EmailValidator) =
		{
			DbUserId.forEmail(email) match
			{
				case Some(userId) =>
					DbEmailValidations.place(email, PasswordReset.id, Some(userId)) match
					{
						case Right(newValidation) => Result.Success(newValidation.resendKey)
						case Left((status, message)) => Result.Failure(status, message)
					}
				case None => Result.Failure(NotFound, "There doesn't exist any user account for the specified email address")
			}
		}
	}
}

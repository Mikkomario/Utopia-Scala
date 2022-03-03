package utopia.exodus.rest.resource.email

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.access.http.Status.{Accepted, BadRequest, Forbidden, InternalServerError, NotFound, NotImplemented}
import utopia.citadel.database.access.id.single.DbUserId
import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.exodus.model.enumeration.ExodusEmailValidationPurpose.{EmailChange, PasswordReset, UserCreation}
import utopia.exodus.model.enumeration.ExodusScope
import utopia.exodus.model.enumeration.ExodusScope.{ChangeEmail, CreateUser, PersonalActions, ReadGeneralData, ReadPersonalData, ReplaceForgottenPassword, RequestPasswordReset}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.{EmailValidator, ExodusContext}
import utopia.metropolis.util.MetropolisRegex
import utopia.nexus.http.Path
import utopia.nexus.rest.{LeafResource, ResourceWithChildren}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * A node used for performing email validations
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1
  */
object EmailsNode extends ResourceWithChildren[AuthorizedContext]
{
	// ATTRIBUTES	---------------------------
	
	private val defaultSupportedMethods = Vector[Method](Post)
	
	override val name = "emails"
	override val children = Vector(EmailChangeNode, PasswordResetNode)
	
	
	// IMPLEMENTED	---------------------------
	
	override def allowedMethods =
		if (ExodusContext.isEmailValidationSupported) defaultSupportedMethods else Vector()
	
	// POST used for validating new email addresses
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		handleRequest(CreateUser) { (email, validator, token, connection) =>
			sendEmailValidationForNewUser(email, token)(connection, validator, context)
		}
	}
	
	
	// OTHER	------------------------------
	
	// Parameter function takes in an email address
	private def handleRequest(requiredScope: ExodusScope)(f: (String, EmailValidator, Token, Connection) => Result)
	                         (implicit context: AuthorizedContext) =
	{
		// Email validation must be implemented
		ExodusContext.emailValidator match
		{
			case Some(validator) =>
				context.authorizedForScope(requiredScope) { (token, connection) =>
					handleEmailUsing { email => f(email, validator, token, connection) }(context)
				}
			case None => Result.Failure(NotImplemented, "Email validation features are not implemented").toResponse
		}
	}
	
	private def handleEmailUsing(f: String => Result)(implicit context: AuthorizedContext) =
	{
		// The request body must contain either a json object with "email" property,
		// or the email address as a string or value
		context.handleValuePost { body =>
			body("email").string.orElse(body.string) match {
				case Some(rawEmail) =>
					val email = rawEmail.trim.toLowerCase
					if (email.isEmpty)
						Result.Failure(BadRequest, "Email address must not be empty")
					else if (MetropolisRegex.email(email))
						f(email)
					else
						Result.Failure(BadRequest, s"'$email' is not a valid email address")
				case None =>
					Result.Failure(BadRequest,
						"Please either provide property 'email' within the request body, or the email address as a text/plain request body")
			}
		}
	}
	
	private def sendEmailValidationForNewUser(email: String, token: Token)
	                                         (implicit connection: Connection, validator: EmailValidator,
	                                          context: AuthorizedContext) =
	{
		// TODO: This 403 status creates a security problem. Send another email instead.
		if (DbManyUserSettings.containsEmail(email))
			Result.Failure(Forbidden, "Specified email address is already in use")
		else {
			// Sends an email validation, if possible
			validator(email, UserCreation, Set(UserCreation), Set(UserCreation, ReadGeneralData),
				Some(token.id), context.modelStyle.orElse(token.modelStylePreference))
			match {
				case Success(_) => Result.Success(status = Accepted)
				case Failure(error) =>
					ExodusContext.handleError(error, "Failed to send email for user creation")
					Result.Failure(InternalServerError, error.getMessage)
			}
		}
	}
	
	
	// NESTED	-----------------------------
	
	private object PasswordResetNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES	---------------------
		
		override val name = "password-resets"
		
		
		// IMPLEMENTED	---------------------
		
		override def allowedMethods = EmailsNode.this.allowedMethods
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		{
			handleRequest(RequestPasswordReset) { (email, validator, token, connection) =>
				sendValidationForPasswordRecovery(email, token)(connection, validator, context)
			}
		}
		
		
		// OTHER	-------------------------
		
		private def sendValidationForPasswordRecovery(email: String, token: Token)
		                                             (implicit connection: Connection, validator: EmailValidator,
		                                              context: AuthorizedContext) =
		{
			// Will yield a success regardless of whether the email exists or not
			// (so that no personal information is given away)
			// TODO: Either make all failure cases return Accepted or an error status (research)
			// TODO: Should send an email whether there exists an account or not
			DbUserId.forEmail(email) match {
				case Some(userId) =>
					// If, for some reason, the token is tied to a user other than that owning the email address, fails
					if (token.ownerId.forall { _ == userId })
						validator(email, PasswordReset, Set(ReplaceForgottenPassword), Set(), Some(token.id),
							context.modelStyle.orElse(token.modelStylePreference)) match
						{
							case Success(_) => Result.Success(status = Accepted)
							case Failure(error) =>
								ExodusContext.handleError(error, "Failed to send an email for password recovery")
								Result.Failure(InternalServerError, error.getMessage)
						}
					else
						Result.Failure(Forbidden, "This email is not yours")
				case None =>
					Result.Failure(NotFound, "There doesn't exist any user account for the specified email address")
			}
		}
	}
	
	private object EmailChangeNode extends LeafResource[AuthorizedContext]
	{
		// ATTRIBUTES   -------------------
		
		override val name = "changes"
		
		
		// IMPLEMENTED  -------------------
		
		override def allowedMethods = EmailsNode.this.allowedMethods
		
		override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		{
			handleRequest(PersonalActions) { (email, validator, token, connection) =>
				implicit val c: Connection = connection
				// Makes sure the email address is not yet in use
				// TODO: This may raise a security issue
				if (DbManyUserSettings.containsEmail(email))
					Result.Failure(Forbidden, "Specified email address is already in use")
				else
				{
					// Places a new email validation
					validator(email, EmailChange, Set(ChangeEmail),
						Set(ChangeEmail, ReadPersonalData, PersonalActions, ReadGeneralData), Some(token.id),
						context.modelStyle.orElse(token.modelStylePreference)) match
					{
						case Success(_) => Result.Success(status = Accepted)
						case Failure(error) =>
							ExodusContext.handleError(error, "Failed to send email for email change")
							Result.Failure(InternalServerError, error.getMessage)
					}
				}
			}
		}
	}
}

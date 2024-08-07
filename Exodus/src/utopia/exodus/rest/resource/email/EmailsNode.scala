package utopia.exodus.rest.resource.email

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.access.http.Status.{Accepted, BadRequest, InternalServerError, NotImplemented}
import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.citadel.database.access.single.user.DbUserSettings
import utopia.exodus.model.enumeration.ExodusEmailValidationPurpose.{EmailChange, PasswordReset, UserCreation}
import utopia.exodus.model.enumeration.ExodusScope
import utopia.exodus.model.enumeration.ExodusScope.{ChangeEmail, InitiateUserCreation, PersonalActions, ReadGeneralData, ReadPersonalData, ReplaceForgottenPassword, RequestPasswordReset}
import utopia.exodus.model.stored.auth.Token
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.{EmailValidator, ExodusContext}
import utopia.flow.collection.immutable.Empty
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
		if (ExodusContext.isEmailValidationSupported) defaultSupportedMethods else Empty
	
	// POST used for validating new email addresses
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		handleRequest(InitiateUserCreation) { (email, validator, token, connection) =>
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
		if (DbManyUserSettings.containsEmail(email))
			validator.sendWithoutToken(email, UserCreation, "You already have an account") match {
				case Success(_) => Result.Success(status = Accepted)
				case Failure(error) =>
					ExodusContext.logger(error, "Failed to inform an user about their existing account")
					Result.Failure(InternalServerError, error.getMessage)
			}
		else {
			// Sends an email validation, if possible
			validator(email, UserCreation, Set(UserCreation), Set(UserCreation, ReadGeneralData),
				Some(token.id), context.modelStyle.orElse(token.modelStylePreference))
			match {
				case Success(_) => Result.Success(status = Accepted)
				case Failure(error) =>
					ExodusContext.logger(error, "Failed to send email for user creation")
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
			val result = {
				// Will yield a success regardless of whether the email exists or not
				// (so that no personal information is given away)
				if (DbUserSettings.withEmail(email).nonEmpty)
					validator(email, PasswordReset, Set(ReplaceForgottenPassword), Set(), Some(token.id),
						context.modelStyle.orElse(token.modelStylePreference))
				else
					validator.sendWithoutToken(email, PasswordReset,
						"You don't own a user account linked with this email address")
			}
			result match {
				case Success(_) => Result.Success(status = Accepted)
				case Failure(error) =>
					ExodusContext.logger(error, "Password recovery email sending failed")
					Result.Failure(InternalServerError, error.getMessage)
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
				val result = {
					// Makes sure the email address is not yet in use
					if (DbManyUserSettings.containsEmail(email))
						validator.sendWithoutToken(email, EmailChange,
							"You already have an account with this email address")
					else
					{
						// Places a new email validation
						validator(email, EmailChange, Set(ChangeEmail),
							Set(ChangeEmail, ReadPersonalData, PersonalActions, ReadGeneralData), Some(token.id),
							context.modelStyle.orElse(token.modelStylePreference))
					}
				}
				result match
				{
					case Success(_) => Result.Success(status = Accepted)
					case Failure(error) =>
						ExodusContext.logger(error, "Failed to send email for email change")
						Result.Failure(InternalServerError, error.getMessage)
				}
			}
		}
	}
}

package utopia.exodus.rest.resource.email

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.access.http.Status.{InternalServerError, NotImplemented, Unauthorized}
import utopia.exodus.database.access.single.DbEmailValidation
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.{EmailValidator, ExodusContext}
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result

import scala.util.{Failure, Success}

/**
  * This rest node is used for requesting email validation resends using resend tokens
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  */
object EmailResendsNode extends Resource[AuthorizedContext]
{
	// ATTRIBUTES	-----------------------
	
	import utopia.citadel.util.CitadelContext._
	import ExodusContext.isEmailValidationSupported
	import ExodusContext.emailValidator
	import ExodusContext.handleError
	
	override val name = "resends"
	
	private val defaultSupportedMethods = Vector[Method](Post)
	
	
	// IMPLEMENTED	-----------------------
	
	override def allowedMethods = if (isEmailValidationSupported) defaultSupportedMethods else Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Requires email validation to be supported
		emailValidator match
		{
			case Some(validator) =>
				// Retrieves the resend token from the bearer token authentication header
				context.request.headers.bearerAuthorization match
				{
					case Some(resendToken) =>
						connectionPool.tryWith { implicit connection =>
							implicit val v: EmailValidator = validator
							DbEmailValidation.resendWith(resendToken) match
							{
								// Doesn't send any data on success
								case Right(_) => Result.Empty
								case Left((status, message)) => Result.Failure(status, message)
							}
						} match
						{
							case Success(result) => result.toResponse
							case Failure(error) =>
								handleError(error, "Failed to resend an email validation")
								Result.Failure(InternalServerError, error.getMessage).toResponse
						}
					case None => Result.Failure(Unauthorized,
						"Please specify a resend token in the bearer authorization header").toResponse
				}
			case None => Result.Failure(NotImplemented, "Email validation features are not implemented").toResponse
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
		Error(message = Some(s"$name doesn't have any child nodes"))
}

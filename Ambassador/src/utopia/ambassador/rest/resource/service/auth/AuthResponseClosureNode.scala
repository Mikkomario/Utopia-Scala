package utopia.ambassador.rest.resource.service.auth

import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, ServiceUnavailable, Unauthorized}
import utopia.ambassador.controller.implementation.AcquireTokens
import utopia.ambassador.database.access.single.process.DbIncompleteAuth
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.citadel.util.CitadelContext._
import utopia.exodus.model.stored.UserSession
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.async.AsyncExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * Used for closing incomplete authentication attempts
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
case class AuthResponseClosureNode(serviceId: Int, tokenAcquirer: AcquireTokens) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED  -----------------------------
	
	override def name = "close"
	
	override def allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Expects a post model with either token as a raw string or model with a 'token' property
			context.handleValuePost { body =>
				body.model.flatMap { _("token").string }.orElse { body.string } match
				{
					case Some(token) =>
						// Makes sure the token is valid and the case still open
						DbIncompleteAuth.forToken(token) match
						{
							case Some(authCase) =>
								if (DbIncompleteAuth(authCase.id).isClosed)
									Result.Failure(Unauthorized, "This case is already closed")
								else
									// Reads the service settings next
									DbAuthService(serviceId).settings.pull match
									{
										// Case: All data available => closes the case
										case Some(settings) => closeAuthCase(settings, authCase, session)
										case None => Result.Failure(ServiceUnavailable,
											s"Service $serviceId is missing required settings")
									}
							case None => Result.Failure(Unauthorized, "Invalid or expired token")
						}
					case None =>
						Result.Failure(BadRequest,
							"Please specify the authentication token in request body property 'token' or as the request body")
				}
			}
		}
	}
	
	
	// OTHER    ----------------------------------
	
	private def closeAuthCase(settings: ServiceSettings, authCase: IncompleteAuth, session: UserSession)
	                         (implicit context: AuthorizedContext, connection: Connection) =
	{
		// Swaps the authentication code for access token(s). Blocks.
		tokenAcquirer.forCode(session.userId, authCase.code, settings).waitForResult() match
		{
			// Case: Success => Returns list of acquired scopes
			case Success(tokens) =>
				val style = session.modelStyle
				Result.Success(tokens.map { _.scopes }.reduce { _ ++ _ }.toVector.map { _.toModelWith(style) })
			// Case: Failure => Returns error
			case Failure(error) => Result.Failure(Unauthorized, error.getMessage)
		}
	}
}

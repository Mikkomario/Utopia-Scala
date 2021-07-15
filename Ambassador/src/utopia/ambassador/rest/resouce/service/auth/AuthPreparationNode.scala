package utopia.ambassador.rest.resouce.service.auth

import utopia.access.http.Method.Post
import utopia.access.http.Status.BadRequest
import utopia.ambassador.database.access.single.organization.DbTask
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.model.post.NewAuthPreparation
import utopia.ambassador.rest.resouce.service.auth.AuthPreparationNode.maxStateLength
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.util.CollectionExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object AuthPreparationNode
{
	// ATTRIBUTES   --------------------------------
	
	/**
	  * Maximum length of the state attribute
	  */
	val maxStateLength = 2000
}

/**
  * This node is used for preparing the server to redirect the client on the next call
  * (performs authorization for the upcoming call)
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class AuthPreparationNode(serviceId: Int, taskId: Int) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED  --------------------------------
	
	override def name = "preparation"
	
	override def allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Parses post model from request and checks if all completion types (Success & Failure)
			// have been covered by the redirect targets
			context.handlePossibleValuePost { v =>
				val preparation = v.model.map(NewAuthPreparation.from).getOrElse { NewAuthPreparation() }
				// Case: There are proper redirect targets => Prepares the authentication
				if (preparation.coversAllCompletionCases ||
					DbAuthService(serviceId).settings.defaultCompletionUrl.isDefined)
				{
					// Makes sure the specified state is not too long
					if (preparation.state.length <= maxStateLength)
					{
						// Reads the scopes required by the task and those available to the user
						val taskScopes = DbTask(taskId).scopes.forServiceWithId(serviceId)
						val (alternative, required) = taskScopes.divideBy { _.isRequired }
						
						val existingScopeIds = DbUser(session.userId).accessibleScopeIds
						val alternativesAreCovered = alternative.isEmpty || alternative.exists { scope =>
							existingScopeIds.contains(scope.id)
						}
						val remainingRequired = required.filterNot { scope => existingScopeIds.contains(scope.id) }
						val isAlreadyAuthorized = alternativesAreCovered && remainingRequired.isEmpty
						
						// TODO: Inserts the preparation to the database
						???
					}
					else
						Result.Failure(BadRequest,
							s"Maximum length of the state property is $maxStateLength characters. Request proposed a state of ${
								preparation.state.length} characters.")
				}
				// Case: There are no proper redirect targets => Fails
				else
					Result.Failure(BadRequest,
						"You must specify a redirect url or urls that cover both successful and failed authentication cases")
			}
		}
	}
}

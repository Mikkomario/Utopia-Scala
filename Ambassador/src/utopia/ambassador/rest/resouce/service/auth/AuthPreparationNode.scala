package utopia.ambassador.rest.resouce.service.auth

import utopia.access.http.Method.Post
import utopia.access.http.Status.BadRequest
import utopia.ambassador.database.access.single.organization.DbTask
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.database.model.process.{AuthCompletionRedirectTargetModel, AuthPreparationModel}
import utopia.ambassador.database.model.scope.AuthPreparationScopeLinkModel
import utopia.ambassador.model.partial.process.{AuthCompletionRedirectTargetData, AuthPreparationData}
import utopia.ambassador.model.post.NewAuthPreparation
import utopia.ambassador.rest.resouce.service.auth.AuthPreparationNode.maxStateLength
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
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
						
						// Inserts the preparation to the database
						val insertedPreparation = AuthPreparationModel.insert(
							AuthPreparationData(session.userId, uuidGenerator.next(), preparation.state.notEmpty))
						// Determines the scopes to request and records those
						val linkedScopes =
						{
							if (isAlreadyAuthorized)
								Vector()
							else
							{
								// If alternative scopes are required, selects the one with the highest priority
								val chosenAlternative = if (alternativesAreCovered) None else
									Some(alternative.maxBy { _.scope.priority.getOrElse(-1) })
								(remainingRequired ++ chosenAlternative).map { _.scope }
							}
						}
						if (linkedScopes.nonEmpty)
							AuthPreparationScopeLinkModel.insert(linkedScopes.map { insertedPreparation.id -> _.id })
						// Inserts the redirect urls (if specified)
						if (preparation.redirectUrls.nonEmpty)
							AuthCompletionRedirectTargetModel.insert(
								preparation.redirectUrls.map { case (filter, url) =>
									AuthCompletionRedirectTargetData(insertedPreparation.id, url, filter) }
									.toVector.sortBy { _.resultFilter.priorityIndex })
						
						// Returns a summary for the client
						val style = session.modelStyle
						val scopesConstant = Constant("scopes", linkedScopes.map { _.toModelWith(style) })
						val extraConstants = style match
						{
							case Simple => Vector(scopesConstant)
							case Full =>
								Vector(scopesConstant,
									Constant("redirect_urls", Model.withConstants(preparation.redirectUrls
										.map { case (filter, url) => Constant(filter.keyName, url) })))
						}
						val resultModel = (Constant("is_already_authorized", isAlreadyAuthorized) +:
							insertedPreparation.toModelWith(style)) ++ extraConstants
						
						Result.Success(resultModel)
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

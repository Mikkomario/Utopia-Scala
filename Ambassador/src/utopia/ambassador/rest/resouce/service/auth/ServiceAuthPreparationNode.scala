package utopia.ambassador.rest.resouce.service.auth

import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, NotFound}
import utopia.ambassador.database.access.single.organization.DbTask
import utopia.ambassador.database.access.single.service.DbAuthService
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.database.model.process.{AuthCompletionRedirectTargetModel, AuthPreparationModel}
import utopia.ambassador.database.model.scope.AuthPreparationScopeLinkModel
import utopia.ambassador.model.enumeration.AuthCompletionType.Default
import utopia.ambassador.model.partial.process.{AuthCompletionRedirectTargetData, AuthPreparationData}
import utopia.ambassador.model.post.NewAuthPreparation
import utopia.ambassador.rest.resouce.service.auth.ServiceAuthPreparationNode.maxStateLength
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object ServiceAuthPreparationNode
{
	// ATTRIBUTES   --------------------------------
	
	/**
	  * Maximum length of the state attribute
	  */
	val maxStateLength = 2000
}

/**
  * This node is used for preparing the server to redirect the client on the next request
  * (performs authorization for the upcoming request, because redirect requests don't support auth headers etc.)
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class ServiceAuthPreparationNode(serviceId: Int) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED  --------------------------------
	
	override def name = "preparations"
	
	override def allowedMethods = Vector(Post)
	
	// TODO: Add information about a possible previous authentication attempt
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Parses the post model
			context.handlePost(NewAuthPreparation) { preparation =>
				// Makes sure service settings are available
				DbAuthService(serviceId).settings.pull match
				{
					case Some(settings) =>
						// Checks if all completion types (Success & Failure) have been covered by the redirect targets
						// Case: There are proper redirect targets => Prepares the authentication
						if (preparation.coversAllCompletionCases || settings.defaultCompletionUrl.isDefined)
						{
							// Makes sure the specified state is not too long
							if (preparation.state.length <= maxStateLength)
							{
								// Reads the scopes required by the task and those available to the user
								val taskScopes = preparation.taskIds
									.map { taskId => DbTask(taskId).scopes.forServiceWithId(serviceId).toSet }
								// Scopes that must be present
								val requiredScopes = taskScopes.flatMap { _.filter { _.isRequired } }
								// Scope groups where it is enough that one of the scopes is present
								val alternativeGroups = taskScopes.map { _.filterNot { _.isRequired } }
									.filter { _.nonEmpty }
								
								val existingScopeIds = DbUser(session.userId).accessibleScopeIds
								// Required scopes that must still be filled
								val missingRequiredScopes = requiredScopes
									.filterNot { scope => existingScopeIds.contains(scope.id) }
								// Alternative scope groups that must still be filled
								// (in addition to the required scopes)
								val missingAlternativeGroups = alternativeGroups.filterNot { group =>
									group.exists { scope => existingScopeIds.contains(scope.id) } ||
										group.exists { scope => missingRequiredScopes.exists { _.scope.id == scope.id } }
								}
								
								val alternativesAreCovered = missingAlternativeGroups.isEmpty
								val isAlreadyAuthorized = alternativesAreCovered && missingRequiredScopes.isEmpty
								
								// Inserts the preparation to the database
								val insertedPreparation = AuthPreparationModel.insert(
									AuthPreparationData(session.userId, uuidGenerator.next(),
										Now + settings.preparationTokenDuration, preparation.state.notEmpty))
								// Determines the scopes to request
								val linkedScopes =
								{
									if (isAlreadyAuthorized || alternativesAreCovered)
										Set()
									else
									{
										// If alternative scopes are required,
										// selects the ones with the highest priority and overlap
										val allOptions = alternativeGroups.flatten
										val optionsByCount = allOptions.groupBy { scope =>
											alternativeGroups.count { _.exists { _.scope.id == scope.id } }
										}
										val orderedOptions = optionsByCount.keys.toVector.sortBy { -_ }
											.flatMap { count => optionsByCount(count).toVector
												.sortBy { -_.scope.priority.getOrElse(-1) } }
										alternativeGroups.flatMap { group => orderedOptions
											.find { scope => group.exists { _.scope.id == scope.id } }
										}
									}
								}
								// Records the requested scopes (in order)
								if (linkedScopes.nonEmpty)
									AuthPreparationScopeLinkModel.insert(linkedScopes.toVector.map { _.scope.id }
										.sorted.map { insertedPreparation.id -> _ })
								// Inserts the redirect urls (if specified)
								if (preparation.redirectUrls.nonEmpty)
									AuthCompletionRedirectTargetModel.insert(
										preparation.redirectUrls.map { case (filter, url) =>
											AuthCompletionRedirectTargetData(insertedPreparation.id, url, filter) }
											.toVector.sortBy { _.resultFilter.priorityIndex })
								
								// Returns a summary for the client.
								// Summary styling may vary based on client preference.
								val style = session.modelStyle
								// Includes the scopes to request
								val scopesConstant = Constant("scopes",
									linkedScopes.toVector.map { _.toModelWith(style) })
								// May include the redirect urls
								val extraConstants = style match
								{
									case Simple => Vector(scopesConstant)
									case Full =>
										val baseRedirectsModel = Model.withConstants(preparation.redirectUrls
											.map { case (filter, url) => Constant(filter.keyName, url) })
										// Appends the default redirect url if necessary
										val redirectsModel =
										{
											if (preparation.coversAllCompletionCases)
												baseRedirectsModel
											else
												baseRedirectsModel ++ settings.defaultCompletionUrl
													.map { url => Constant(Default.keyName, url) }
										}
										Vector(scopesConstant, Constant("redirect_urls", redirectsModel))
								}
								// Includes is_already_authorized -parameter which is true
								// when the authentication process is unnecessary
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
					case None => Result.Failure(NotFound, s"Service id $serviceId is invalid or not supported at this time")
				}
			}
		}
	}
}

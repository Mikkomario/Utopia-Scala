package utopia.ambassador.rest.resource.service.auth

import utopia.access.http.Method.Post
import utopia.access.http.Status.{BadRequest, NotFound}
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.database.model.process.{AuthCompletionRedirectTargetModel, AuthPreparationModel, AuthPreparationScopeLinkModel}
import utopia.ambassador.model.enumeration.AuthCompletionType.Default
import utopia.ambassador.model.partial.process.{AuthCompletionRedirectTargetData, AuthPreparationData, AuthPreparationScopeLinkData}
import utopia.ambassador.model.post.NewAuthPreparation
import AuthPreparationNode.maxStateLength
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.rest.util.ServiceTarget
import utopia.citadel.database.access.many.organization.DbTasks
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
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
	val maxStateLength = 2048
}

/**
  * This node is used for preparing the server to redirect the client on the next request
  * (performs authorization for the upcoming request, because redirect requests don't support auth headers etc.)
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
class AuthPreparationNode(target: ServiceTarget) extends LeafResource[AuthorizedContext]
{
	// IMPLEMENTED  --------------------------------
	
	override def name = "preparations"
	
	override def allowedMethods = Vector(Post)
	
	// TODO: Add information about a possible previous authentication attempt
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionTokenAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Parses the post model
			context.handlePost(NewAuthPreparation) { preparation =>
				// Makes sure service settings are available
				target.settings match
				{
					case Some(settings) =>
						// Checks if all completion types (Success & Failure) have been covered by the redirect targets
						// Case: There are proper redirect targets => Prepares the authentication
						if (preparation.coversAllCompletionCases || settings.defaultCompletionRedirectUrl.isDefined)
						{
							val state = preparation.state.string.filter { _.nonEmpty }
							// Makes sure the specified state is not too long
							if (state.forall { _.length <= maxStateLength })
							{
								// Reads the scopes required by the task and those available to the user
								val taskScopes = DbTasks(preparation.taskIds).scopes
									.forServiceWithId(settings.serviceId).pull
								// Scopes that must be present
								val requiredScopes = taskScopes.filter { _.isRequired }
								// Scope groups where it is enough that one of the scopes is present
								val alternativesByTaskId = taskScopes.filter { _.isOptional }
									.groupBy { _.taskLink.taskId }
								
								val existingScopeIds = session.userAccess.accessibleScopeIds
								// Required scopes that must still be filled
								val missingRequiredScopes = requiredScopes
									.filterNot { scope => existingScopeIds.contains(scope.id) }
								// Alternative scope groups that must still be filled
								// (in addition to the required scopes)
								val missingAlternativeGroups = alternativesByTaskId.filter { case (_, scopes) =>
									scopes.forall { s => !existingScopeIds.contains(s.id) } }
								
								val alternativesAreCovered = missingAlternativeGroups.isEmpty
								val isAlreadyAuthorized = alternativesAreCovered && missingRequiredScopes.isEmpty
								
								// Inserts the preparation to the database
								val insertedPreparation = AuthPreparationModel.insert(
									AuthPreparationData(session.userId, uuidGenerator.next(),
										Now + settings.preparationTokenDuration, state))
								// Determines the scopes to request
								val linkedScopes =
								{
									if (isAlreadyAuthorized || alternativesAreCovered)
										Set()
									else
									{
										// If alternative scopes are required,
										// selects the ones with the highest priority and overlap
										// Scope id => number of occurrences
										val scopeCounts = alternativesByTaskId.valuesIterator.flatten.toSet
											.map { scope: TaskScope =>
												scope.id -> (missingRequiredScopes.count { _.id == scope.id } +
												alternativesByTaskId
													.map { case (_, scopes) => scopes.count { _.id == scope.id } }.sum)
											}.toMap
										alternativesByTaskId.valuesIterator
											.map { options =>
												val optionsByCount = options.groupBy { s => scopeCounts(s.id) }
												optionsByCount.maxBy { _._1 }._2.maxBy { _.priority.getOrElse(-1) }
											}
											.toSet
									}
								}
								// Records the requested scopes (in order)
								if (linkedScopes.nonEmpty)
									AuthPreparationScopeLinkModel.insert(linkedScopes.toVector.map { _.scope.id }
										.sorted.map { AuthPreparationScopeLinkData(insertedPreparation.id, _) })
								// Inserts the redirect urls (if specified)
								if (preparation.redirectUrls.nonEmpty)
									AuthCompletionRedirectTargetModel.insert(
										preparation.redirectUrls.map { case (filter, url) =>
											AuthCompletionRedirectTargetData(insertedPreparation.id, url,
												filter.successFilter, filter.deniedFilter) }
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
												baseRedirectsModel ++ settings.defaultCompletionRedirectUrl
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
										state.get.length} characters.")
						}
						// Case: There are no proper redirect targets => Fails
						else
							Result.Failure(BadRequest,
								"You must specify a redirect url or urls that cover both successful and failed authentication cases")
					case None => Result.Failure(NotFound, s"$target is invalid or not supported at this time")
				}
			}
		}
	}
}

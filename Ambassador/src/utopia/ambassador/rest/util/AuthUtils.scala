package utopia.ambassador.rest.util

import utopia.access.http.Status.Unauthorized
import utopia.ambassador.database.access.single.process.DbAuthPreparation
import utopia.ambassador.model.combined.scope.TaskScope
import utopia.ambassador.model.enumeration.AuthCompletionType.Default
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.collection.CollectionExtensions._
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Contains utility methods that are used in authentication processes
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthUtils
{
	/**
	 * Tests whether required and accessible scopes match, so that access may be granted
	 * @param taskScopes Task scope requirements (expected to belong to a single task)
	 * @param availableScopeIds Accessible scope ids (call by name)
	 * @return Whether the available scope ids fulfill all specified task requirements
	 */
	def testTaskAccess(taskScopes: Iterable[TaskScope], availableScopeIds: => Iterable[Int]) =
	{
		if (taskScopes.isEmpty)
			true
		else
		{
			val providedScopeIds = availableScopeIds.toSet
			// Makes sure all the required scopes are covered, and at least one alternative scope per service
			val (alternativeScopes, requiredScopes) = taskScopes.divideBy { _.isRequired }
			requiredScopes.forall { scope => providedScopeIds.contains(scope.id) } &&
				(alternativeScopes.isEmpty || alternativeScopes.groupBy { _.serviceId }.values
					.forall { scopes => scopes.exists { scope => providedScopeIds.contains(scope.id) } })
		}
	}
	
	/**
	  * Creates a result for completing an authentication process with a redirect (if possible)
	  * @param settings Service settings to apply
	  * @param preparation An authentication preparation, if one is found
	  * @param errorMessage Failure message. Empty on success (default)
	  * @param deniedAccess Whether the user denied access to some or all of the services (default = false)
	  * @param connection Implicit database connection
	  * @return Result to send
	  */
	def completionRedirect(settings: AuthServiceSettings, preparation: Option[AuthPreparation] = None,
	                       errorMessage: String = "", deniedAccess: Boolean = false)
	                      (implicit connection: Connection) =
	{
		// Reads the redirection target from the preparation, if possible
		preparation
			.flatMap { preparation =>
				DbAuthPreparation(preparation.id).redirectTargets.forResult(errorMessage.isEmpty, deniedAccess)
					.maxByOption { _.resultFilter.priorityIndex }
					.map { target => target.resultFilter -> target.url }
			}
			// Alternatively uses service settings default, if available
			.orElse { settings.defaultCompletionRedirectUrl.map { Default -> _ } } match
		{
			// Case: Redirect url found
			case Some((urlFilter, baseUrl)) =>
				// May add some parameters to describe result state. Less parameters are included in more
				// specific redirect urls
				val stateParams =
				{
					if (urlFilter.deniedFilter)
						Vector()
					else
					{
						val deniedParam =Constant("denied_access", deniedAccess)
						if (urlFilter.successFilter.isDefined)
							Vector(deniedParam)
						else
							Vector(immutable.Constant("was_success", errorMessage.isEmpty), deniedParam)
					}
				}
				// Appends possible error and state parameters
				// State is added as a json value if it can be parsed
				val allParams = Model.withConstants(stateParams) ++
					preparation.flatMap { _.clientState }.map {Constant("state", _) } ++
					errorMessage.notEmpty.map { Constant("error", _) }
				// Redirects the user
				val parametersString = allParams.attributesWithValue
					.map { att => s"${att.name}=${att.value.toJson}" }.mkString("&")
				val finalUrl = if (baseUrl.contains('?')) s"$baseUrl&$parametersString" else s"$baseUrl?$parametersString"
				Result.Redirect(finalUrl)
			// Case: No redirection url specified anywhere => Returns a success or a failure
			case None =>
				// Case: Immediate success
				if (errorMessage.isEmpty)
					Result.Success(Value.empty, description = Some("No authentication required"))
				// Case: Immediate failure
				else
					Result.Failure(Unauthorized, errorMessage)
		}
	}
}

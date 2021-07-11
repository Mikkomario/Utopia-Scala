package utopia.ambassador.rest.resource.feature

import utopia.access.http.Method.Get
import utopia.ambassador.database.access.single.organization.DbTask
import utopia.ambassador.database.AuthDbExtensions._
import utopia.ambassador.database.access.many.scope.DbScopeDescriptions
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A Rest node which simply tests whether the user has acquired access to the specified feature at this time
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class FeatureAccessTestNode(taskId: Int) extends LeafResource[AuthorizedContext]
{
	override def name = "test"
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads the scopes required by the targeted task
			val scopes = DbTask(taskId).scopes.pull
			if (scopes.nonEmpty)
			{
				val (alternative, required) = scopes.divideBy { _.isRequired }
				// Reads the scopes currently accessible for the user
				val readyScopeIds = DbUser(session.userId).accessibleScopeIds
				
				// Checks whether there are any required and provides a description of them for the client
				val remainingRequiredScopes = required.filterNot { scope => readyScopeIds.contains(scope.id) }
				val hasAlternativeScopes = alternative.isEmpty ||
					alternative.exists { scope => readyScopeIds.contains(scope.id) }
				
				if (remainingRequiredScopes.isEmpty && hasAlternativeScopes)
					Result.Success(Model(Vector("is_authorized" -> true)))
				else
				{
					// Reads the descriptions of the missing scopes
					val descriptions = DbScopeDescriptions(
						(remainingRequiredScopes ++ alternative).map { _.id }.toSet)
						.inLanguages(context.languageIdListFor(session.userId))
					// TODO: Read services and form the response
					???
				}
			}
			else
				Result.Success(Model(Vector("is_authorized" -> true)))
		}
	}
}
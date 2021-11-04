package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.access.http.Status.InternalServerError
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

/**
  * Used for accessing item descriptions without requiring authorization
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
trait PublicDescriptionsNode[Item, +Combined <: ModelConvertible with SimplyDescribed]
	extends Resource[AuthorizedContext]
{
	import utopia.citadel.util.CitadelContext._
	import utopia.exodus.util.ExodusContext.handleError
	
	// ABSTRACT	------------------------------------
	
	/**
	 * @return The model style to use on this resource when no styling is specified in the context
	 */
	def defaultModelStyle: ModelStyle
	
	/**
	  * Authorizes the incoming request
	  * @param onAuthorized A function to call if the request is authorized. Produces final result
	  * @param context Request context
	  * @param connection Database connection (implicit)
	  * @return Function result if authorized, otherwise a failure indicating a suitable authorization problem.
	  */
	protected def authorize(onAuthorized: => Result)
	                       (implicit context: AuthorizedContext, connection: Connection): Result
	
	protected def describedItems(implicit connection: Connection, languageIds: LanguageIds): Vector[Combined]
	
	
	// IMPLEMENTED	--------------------------------
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Checks which languages should be used when reading descriptions
		// Order = accept language > authorized user > all
		connectionPool.tryWith { implicit connection =>
			val accepted = context.requestedLanguages
			if (accepted.nonEmpty)
				authorize {
					implicit val languageIds: LanguageIds = LanguageIds(accepted.map { _.id })
					get(context.modelStyle.getOrElse(defaultModelStyle))
				}.toResponse
			else if (context.request.headers.containsAuthorization)
				context.sessionTokenAuthorized { (session, _) =>
					implicit val userLanguages: LanguageIds = DbUser(session.userId).languageIds
					get(session.modelStyle)
				}
			else
				authorize {
					implicit val languageIds: LanguageIds = LanguageIds(Vector())
					get(context.modelStyle.getOrElse(defaultModelStyle))
				}.toResponse
		} match
		{
			case Success(response) => response
			case Failure(error) =>
				handleError(error, "Failed to read language descriptions")
				Result.Failure(InternalServerError, error.getMessage).toResponse
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message = Some(
		s"$name doesn't currently contain any sub-nodes"))
	
	
	// OTHER	---------------------------------
	
	private def get(resultStyle: ModelStyle)(implicit connection: Connection, languageIds: LanguageIds) =
	{
		// Reads all described items. Then returns them in response
		val combined = describedItems
		// Converts the results to correct format
		resultStyle match
		{
			case Full => Result.Success(combined.map { _.toModel })
			case Simple =>
				val roles = DbDescriptionRoles.pull
				Result.Success(combined.map { _.toSimpleModelUsing(roles) })
		}
	}
}

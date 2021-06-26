package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.access.http.Status.InternalServerError
import utopia.citadel.database.access.many.description.DescriptionLinksForManyAccess
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.description.DescriptionLink
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
trait PublicDescriptionsNode[Item, Combined <: ModelConvertible] extends Resource[AuthorizedContext]
{
	import utopia.citadel.util.CitadelContext._
	import utopia.exodus.util.ExodusContext.handleError
	
	// ABSTRACT	------------------------------------
	
	/**
	  * Authorizes the incoming request
	  * @param onAuthorized A function to call if the request is authorized. Produces final result
	  * @param context Request context
	  * @param connection Database connection (implicit)
	  * @return Function result if authorized, otherwise a failure indicating a suitable authorization problem.
	  */
	protected def authorize(onAuthorized: => Result)(implicit context: AuthorizedContext, connection: Connection): Result
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return All returned items
	  */
	protected def items(implicit connection: Connection): Vector[Item]
	
	/**
	  * @return An access point to the descriptions of the returned items
	  */
	protected def descriptionsAccess: DescriptionLinksForManyAccess
	
	/**
	  * @param item An item
	  * @return The item's id
	  */
	protected def idOf(item: Item): Int
	
	/**
	  * Combines items with their descriptions
	  * @param item An item
	  * @param descriptions Descriptions for that item
	  * @return A combined item
	  */
	protected def combine(item: Item, descriptions: Set[DescriptionLink]): Combined
	
	
	// IMPLEMENTED	--------------------------------
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Checks which languages should be used when reading descriptions
		// Order = accept language > authorized user > all
		connectionPool.tryWith { implicit connection =>
			val accepted = context.requestedLanguages
			if (accepted.nonEmpty)
				get(accepted.map { _.id }).toResponse
			else if (context.request.headers.containsAuthorization)
			{
				context.sessionKeyAuthorized { (session, _) =>
					val userLanguages = DbUser(session.userId).languages.withFamiliarityLevels
						.sortBy { _._2.orderIndex }.map { _._1 }
					get(userLanguages)
				}
			}
			else
				get(Vector()).toResponse
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
	
	private def get(languageIds: Seq[Int])(implicit connection: Connection) =
	{
		// Reads all descriptions
		val descriptions =
		{
			if (languageIds.isEmpty)
				descriptionsAccess.all.groupBy { _.targetId }
			else
				descriptionsAccess.inLanguages(languageIds)
		}
		// Reads all items and combines them with descriptions. Then returns them in response
		val combined = items.map { item => combine(item, descriptions.getOrElse(idOf(item), Set()).toSet) }
		Result.Success(combined.map { _.toModel })
	}
}

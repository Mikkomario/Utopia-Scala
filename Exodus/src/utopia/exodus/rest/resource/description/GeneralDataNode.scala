package utopia.exodus.rest.resource.description

import utopia.access.http.Method.Get
import utopia.citadel.database.access.many.description.{DbDescriptionRoles, ManyDescribedAccess}
import utopia.exodus.model.enumeration.ExodusScope.ReadGeneralData
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.{LeafResource, Resource}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object GeneralDataNode
{
	// OTHER    ----------------------------
	
	/**
	  * Creates a restful interface for an access point
	  * @param name Name of this node
	  * @param access An access point to wrap
	  * @tparam A Accessed items, including their descriptions
	  * @return A new rest node wrapping that access point
	  */
	def wrapAccess[A <: ModelConvertible with SimplyDescribed](name: String,
	                                                           access: ManyDescribedAccess[_, A]): GeneralDataNode[A] =
		new AccessWrapperNode[A](name, access)
	
	
	// NESTED   ----------------------------
	
	private class AccessWrapperNode[+A <: ModelConvertible with SimplyDescribed]
	(override val name: String, access: ManyDescribedAccess[_, A])
		extends GeneralDataNode[A] with LeafResource[AuthorizedContext]
	{
		override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
			access.described
	}
}

/**
  * Used for accessing generally used data (read only)
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
trait GeneralDataNode[+A <: ModelConvertible with SimplyDescribed] extends Resource[AuthorizedContext]
{
	// ABSTRACT	------------------------------------
	
	/**
	  * Reads data to return, including their descriptions
	  * @param connection Implicit DB Connection
	  * @param languageIds Implicit language ids
	  * @return This node's data, including descriptions in the specified language(s)
	  */
	protected def describedItems(implicit connection: Connection, languageIds: LanguageIds): Vector[A]
	
	
	// IMPLEMENTED	--------------------------------
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Authorizes the request
		context.authorizedForScope(ReadGeneralData) { (token, connection) =>
			implicit val c: Connection = connection
			implicit val languageIds: LanguageIds = token.languageIds
			// Pulls data in the correct styling
			get(token.modelStyle)
		}
	}
	
	
	// OTHER	---------------------------------
	
	private def get(resultStyle: ModelStyle)(implicit connection: Connection, languageIds: LanguageIds) =
	{
		// Reads all described items. Then returns them in response
		val combined = describedItems
		// Converts the results to correct format
		resultStyle match {
			case Full => Result.Success(combined.map { _.toModel })
			case Simple =>
				val roles = DbDescriptionRoles.pull
				Result.Success(combined.map { _.toSimpleModelUsing(roles) })
		}
	}
}

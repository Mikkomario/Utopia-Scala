package utopia.logos.database.access.single.url.link

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.logos.database.LogosContext
import utopia.logos.database.storable.url.LinkModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

import java.time.Instant

/**
  * A common trait for access points which target individual links or similar items at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueLinkAccessLike[+A] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the targeted internet address, 
	  * including the specific sub-path. None if no link (or value) was found.
	  */
	def requestPathId(implicit connection: Connection) = pullColumn(model.requestPathId.column).int
	
	/**
	  * Specified request parameters in model format. None if no link (or value) was found.
	  */
	def queryParameters(implicit connection: Connection) = {
		pullColumn(model.queryParameters.column).notEmpty match 
		{
			case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel
			case None => Model.empty
		}
	}
	
	/**
	  * Time when this link was added to the database. None if no link (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LinkModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted links
	  * @param newCreated A new created to assign
	  * @return Whether any link was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the query parameterses of the targeted links
	  * @param newQueryParameters A new query parameters to assign
	  * @return Whether any link was affected
	  */
	def queryParameters_=(newQueryParameters: Model)(implicit connection: Connection) = 
		putColumn(model.queryParameters.column, newQueryParameters.notEmpty.map { _.toJson })
	
	/**
	  * Updates the request path ids of the targeted links
	  * @param newRequestPathId A new request path id to assign
	  * @return Whether any link was affected
	  */
	def requestPathId_=(newRequestPathId: Int)(implicit connection: Connection) = 
		putColumn(model.requestPathId.column, newRequestPathId)
}


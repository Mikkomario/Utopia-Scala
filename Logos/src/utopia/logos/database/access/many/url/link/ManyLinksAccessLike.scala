package utopia.logos.database.access.many.url.link

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.logos.database.LogosContext
import utopia.logos.database.storable.url.LinkModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple links or similar instances at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyLinksAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * request path ids of the accessible links
	  */
	def requestPathIds(implicit connection: Connection) = 
		pullColumn(model.requestPathId.column).map { v => v.getInt }
	
	/**
	  * query parameterses of the accessible links
	  */
	def queryParameters(implicit connection: Connection) =
		pullColumn(model.queryParameters.column).map { v =>
			v.notEmpty match {
				case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel
				case None => Model.empty
			}
		}
	
	/**
	  * creation times of the accessible links
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
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
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
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
	def requestPathIds_=(newRequestPathId: Int)(implicit connection: Connection) = 
		putColumn(model.requestPathId.column, newRequestPathId)
	
	/**
	  * @param pathIds Ids of included request paths
	  * @return Access to links to those paths
	  */
	def toPaths(pathIds: Iterable[Int]) = filter(model.requestPathId.column.in(pathIds))
}


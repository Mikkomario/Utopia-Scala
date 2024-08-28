package utopia.logos.database.access.single.url.link

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.logos.database.LogosContext
import utopia.logos.database.storable.url.LinkDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target individual links or similar items at a time
  * @tparam A Type of read (links -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueLinkAccessLike[+A, +Repr] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with FilterableView[Repr] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the targeted internet address, including the specific sub-path. 
	  * None if no link (or value) was found.
	  */
	def pathId(implicit connection: Connection) = pullColumn(model.pathId.column).int
	/**
	  * Specified request parameters in model format. 
	  * None if no link (or value) was found.
	  */
	def queryParameters(implicit connection: Connection) = 
		pullColumn(model.queryParameters.column).notEmpty match {
			 case Some(v) => LogosContext.jsonParser.valueOf(v.getString).getModel; case None => Model.empty }
	/**
	  * Time when this link was added to the database. 
	  * None if no link (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	/**
	  * Unique id of the accessible link. None if no link was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = LinkDbModel
	
	
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
}


package utopia.logos.database.access.single.url.path

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target individual request paths or similar items at a time
  * @tparam A Type of read (request paths -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueRequestPathAccessLike[+A, +Repr] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with FilterableView[Repr] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the domain part of this url. 
	  * None if no request path (or value) was found.
	  */
	def domainId(implicit connection: Connection) = pullColumn(model.domainId.column).int
	/**
	  * Part of this url that comes after the domain part. Doesn't include any query parameters, 
	  * nor the initial forward slash. 
	  * None if no request path (or value) was found.
	  */
	def path(implicit connection: Connection) = pullColumn(model.path.column).getString
	/**
	  * Time when this request path was added to the database. 
	  * None if no request path (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	/**
	  * Unique id of the accessible request path. None if no request path was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = RequestPathDbModel
}


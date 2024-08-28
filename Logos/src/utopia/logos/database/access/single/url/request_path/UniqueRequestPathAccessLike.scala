package utopia.logos.database.access.single.url.request_path

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.storable.url.RequestPathModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

import java.time.Instant

/**
  * A common trait for access points which target individual request paths or similar items at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
trait UniqueRequestPathAccessLike[+A] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the domain part of this url. None if no request path (or value) was found.
	  */
	def domainId(implicit connection: Connection) = pullColumn(model.domainId.column).int
	
	/**
	  * Part of this url that comes after the domain part. Doesn't include any query parameters, 
	  * nor the initial forward slash.. None if no request path (or value) was found.
	  */
	def path(implicit connection: Connection) = pullColumn(model.path.column).getString
	
	/**
	  * Time when this request path was added to the database. None if no request path (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = RequestPathModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted request paths
	  * @param newCreated A new created to assign
	  * @return Whether any request path was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the domain ids of the targeted request paths
	  * @param newDomainId A new domain id to assign
	  * @return Whether any request path was affected
	  */
	def domainId_=(newDomainId: Int)(implicit connection: Connection) = 
		putColumn(model.domainId.column, newDomainId)
	
	/**
	  * Updates the paths of the targeted request paths
	  * @param newPath A new path to assign
	  * @return Whether any request path was affected
	  */
	def path_=(newPath: String)(implicit connection: Connection) = putColumn(model.path.column, newPath)
}


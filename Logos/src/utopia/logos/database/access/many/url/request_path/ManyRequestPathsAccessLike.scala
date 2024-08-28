package utopia.logos.database.access.many.url.request_path

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.RequestPathModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple request paths or similar instances at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, v0.1
  */
@deprecated("Replaced with a new version", "v0.3")
trait ManyRequestPathsAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * domain ids of the accessible request paths
	  */
	def domainIds(implicit connection: Connection) = pullColumn(model.domainId.column).map { v => v.getInt }
	
	/**
	  * paths of the accessible request paths
	  */
	def paths(implicit connection: Connection) = pullColumn(model.path.column).flatMap { _.string }
	
	/**
	  * creation times of the accessible request paths
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
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
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the domain ids of the targeted request paths
	  * @param newDomainId A new domain id to assign
	  * @return Whether any request path was affected
	  */
	def domainIds_=(newDomainId: Int)(implicit connection: Connection) = 
		putColumn(model.domainId.column, newDomainId)
	
	/**
	  * Updates the paths of the targeted request paths
	  * @param newPath A new path to assign
	  * @return Whether any request path was affected
	  */
	def paths_=(newPath: String)(implicit connection: Connection) = putColumn(model.path.column, newPath)
	
	/**
	  * @param domainIds Ids of the targeted domains
	  * @return Access to request paths under those domains
	  */
	def withinDomains(domainIds: Iterable[Int]) = filter(model.domainId.column.in(domainIds))
}


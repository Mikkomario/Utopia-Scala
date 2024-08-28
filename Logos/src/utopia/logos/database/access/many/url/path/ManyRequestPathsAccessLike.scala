package utopia.logos.database.access.many.url.path

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target multiple request paths or similar instances at a time
  * @tparam A Type of read (request paths -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
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
	
	/**
	  * Unique ids of the accessible request paths
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = RequestPathDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param path path to target
	  * @return Copy of this access point that only includes request paths with the specified path
	  */
	def withPath(path: String) = filter(model.path.column <=> path)
	
	/**
	  * @param paths Targeted paths
	  * @return Copy of this access point that only includes request paths where path is within the specified
	  *  value set
	  */
	def withPaths(paths: Iterable[String]) = filter(model.path.column.in(paths))
	
	/**
	  * @param domainId domain id to target
	  * @return Copy of this access point that only includes request paths with the specified domain id
	  */
	def withinDomain(domainId: Int) = filter(model.domainId.column <=> domainId)
	
	/**
	  * @param domainIds Targeted domain ids
	  * @return Copy of this access point that only includes request paths where domain id is within the
	  *  specified value set
	  */
	def withinDomains(domainIds: IterableOnce[Int]) = filter(model.domainId.column.in(IntSet.from(domainIds)))
}


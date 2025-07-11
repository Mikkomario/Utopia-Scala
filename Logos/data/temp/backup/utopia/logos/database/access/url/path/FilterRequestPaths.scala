package utopia.logos.database.access.url.path

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on request path properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterRequestPaths[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines request path database properties
	  */
	def requestPathModel = RequestPathDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param path path to target
	  * @return Copy of this access point that only includes request paths with the specified path
	  */
	def withPath(path: String) = filter(requestPathModel.path.column <=> path)
	
	/**
	  * @param paths Targeted paths
	  * @return Copy of this access point that only includes request paths where path is within the specified 
	  * value set
	  */
	def withPaths(paths: Iterable[String]) = filter(requestPathModel.path.column.in(paths))
	
	/**
	  * @param domainId domain id to target
	  * @return Copy of this access point that only includes request paths with the specified domain id
	  */
	def withinDomain(domainId: Int) = filter(requestPathModel.domainId.column <=> domainId)
	
	/**
	  * @param domainIds Targeted domain ids
	  * @return Copy of this access point that only includes request paths where domain id is within the 
	  * specified value set
	  */
	def withinDomains(domainIds: IterableOnce[Int]) = 
		filter(requestPathModel.domainId.column.in(IntSet.from(domainIds)))
}


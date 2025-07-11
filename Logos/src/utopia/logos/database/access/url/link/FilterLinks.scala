package utopia.logos.database.access.url.link

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.url.LinkDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on link properties
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
trait FilterLinks[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines link database properties
	  */
	def model = LinkDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param pathId path id to target
	  * @return Copy of this access point that only includes links with the specified path id
	  */
	def withPath(pathId: Int) = filter(model.pathId.column <=> pathId)
	
	/**
	  * @param pathIds Targeted path ids
	  * @return Copy of this access point that only includes links where path id is within the specified 
	  * value set
	  */
	def withPaths(pathIds: IterableOnce[Int]) = filter(model.pathId.column.in(IntSet.from(pathIds)))
}


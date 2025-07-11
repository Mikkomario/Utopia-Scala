package utopia.logos.database.access.url.link.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.text.placement.FilterTextPlacements
import utopia.logos.database.storable.url.LinkPlacementDbModel

/**
  * Common trait for access points which may be filtered based on link placement properties
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
trait FilterLinkPlacements[+Repr] extends FilterTextPlacements[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines link placement database properties
	  */
	def model = LinkPlacementDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param linkId link id to target
	  * @return Copy of this access point that only includes link placements with the specified link id
	  */
	def placingLink(linkId: Int) = filter(model.linkId.column <=> linkId)
	
	/**
	  * @param linkIds Targeted link ids
	  * @return Copy of this access point that only includes link placements where link id is within the 
	  * specified value set
	  */
	def placingLinks(linkIds: IterableOnce[Int]) = filter(model.linkId.column.in(IntSet.from(linkIds)))
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes link placements with the specified statement id
	  */
	def withinStatement(statementId: Int) = filter(model.statementId.column <=> statementId)
	
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes link placements where statement id is within the 
	  * specified value set
	  */
	def withinStatements(statementIds: IterableOnce[Int]) = 
		filter(model.statementId.column.in(IntSet.from(statementIds)))
}


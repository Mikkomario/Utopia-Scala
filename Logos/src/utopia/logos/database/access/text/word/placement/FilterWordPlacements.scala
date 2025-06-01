package utopia.logos.database.access.text.word.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.text.placement.FilterTextPlacements
import utopia.logos.database.storable.text.WordPlacementDbModel

/**
  * Common trait for access points which may be filtered based on word placement properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterWordPlacements[+Repr] extends FilterTextPlacements[Repr]
{
	// COMPUTED ------------------------
	
	/**
	 * Model that defines word placement database properties
	 */
	def wordPlacementModel = WordPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def textPlacementModel = wordPlacementModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param wordId word id to target
	  * @return Copy of this access point that only includes word placements with the specified word id
	  */
	def placingWord(wordId: Int) = filter(wordPlacementModel.wordId.column <=> wordId)
	/**
	  * @param wordIds Targeted word ids
	  * @return Copy of this access point that only includes word placements where word id is within the 
	  * specified value set
	  */
	def placingWords(wordIds: IterableOnce[Int]) = 
		filter(wordPlacementModel.wordId.column.in(IntSet.from(wordIds)))
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes word placements with the specified statement id
	  */
	def withinStatement(statementId: Int) = filter(wordPlacementModel.statementId.column <=> statementId)
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes word placements where statement id is within the 
	  * specified value set
	  */
	def withinStatements(statementIds: IterableOnce[Int]) = 
		filter(wordPlacementModel.statementId.column.in(IntSet.from(statementIds)))
}


package utopia.logos.database.access.text.word.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.text.placement.FilterTextPlacements
import utopia.logos.database.storable.text.WordPlacementDbModel

/**
  * Common trait for access points which may be filtered based on word placement properties
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
trait FilterWordPlacements[+Repr] extends FilterTextPlacements[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines word placement database properties
	  */
	def model = WordPlacementDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param wordId word id to target
	  * @return Copy of this access point that only includes word placements with the specified word id
	  */
	def placingWord(wordId: Int) = filter(model.wordId.column <=> wordId)
	
	/**
	  * @param wordIds Targeted word ids
	  * @return Copy of this access point that only includes word placements where word id is within the 
	  * specified value set
	  */
	def placingWords(wordIds: IterableOnce[Int]) = filter(model.wordId.column.in(IntSet.from(wordIds)))
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes word placements with the specified statement id
	  */
	def withinStatement(statementId: Int) = filter(model.statementId.column <=> statementId)
	
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes word placements where statement id is within the 
	  * specified value set
	  */
	def withinStatements(statementIds: IterableOnce[Int]) = 
		filter(model.statementId.column.in(IntSet.from(statementIds)))
}


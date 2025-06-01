package utopia.logos.database.access.text.statement.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.text.placement.FilterTextPlacements
import utopia.logos.database.props.text.StatementPlacementDbProps

/**
  * Common trait for access points which may be filtered based on statement placement properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterStatementPlacements[+Repr] extends FilterTextPlacements[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Model that defines statement placement database properties
	  */
	def statementPlacementModel: StatementPlacementDbProps
	
	
	// IMPLEMENTED	--------------------
	
	override def textPlacementModel = statementPlacementModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes statement placements with the specified 
	  * statement id
	  */
	def placingStatement(statementId: Int) = 
		filter(statementPlacementModel.statementId.column <=> statementId)
	
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes statement placements where statement id is 
	  * within the specified value set
	  */
	def placingStatements(statementIds: IterableOnce[Int]) = 
		filter(statementPlacementModel.statementId.column.in(IntSet.from(statementIds)))
}


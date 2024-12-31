package utopia.logos.database.access.many.text.statement.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.many.text.placement.ManyTextPlacementsAccessLike
import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.vault.database.Connection

/**
  * A common trait for access points which target multiple statement placements or similar 
  * instances at a time
  * @tparam A Type of read (statement placements -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.3.1
  */
trait ManyStatementPlacementsAccessLike[+A, +Repr] extends ManyTextPlacementsAccessLike[A, Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	override protected def model: StatementPlacementDbProps
	
	
	// COMPUTED	--------------------
	
	/**
	  * statement ids of the accessible statement placements
	  */
	def statementIds(implicit connection: Connection) = placedIds
	
	
	// OTHER	--------------------
	
	/**
	  * @param statementId statement id to target
	  * @return Copy of this access point that only includes statement placements with the specified 
	  * statement id
	  */
	def placingStatement(statementId: Int) = filter(model.statementId.column <=> statementId)
	
	/**
	  * @param statementIds Targeted statement ids
	  * @return Copy of this access point that only includes statement placements where statement id is 
	  * within the specified value set
	  */
	def placingStatements(statementIds: IterableOnce[Int]) = 
		filter(model.statementId.column.in(IntSet.from(statementIds)))
}


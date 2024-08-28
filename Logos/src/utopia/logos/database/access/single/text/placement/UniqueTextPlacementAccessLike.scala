package utopia.logos.database.access.single.text.placement

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target individual text placements or similar items at a time
  * @tparam A Type of read (text placements -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueTextPlacementAccessLike[+A, +Repr] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with FilterableView[Repr] 
		with Indexed
{
	// ABSTRACT	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model: TextPlacementDbProps
	
	
	// COMPUTED	--------------------
	
	/**
	  * Id of the text where the placed text appears. 
	  * None if no text placement (or value) was found.
	  */
	def parentId(implicit connection: Connection) = pullColumn(model.parentId.column).int
	
	/**
	  * Id of the text that is placed within the parent text. 
	  * None if no text placement (or value) was found.
	  */
	def placedId(implicit connection: Connection) = pullColumn(model.placedId.column).int
	
	/**
	  * 0-based index that indicates the specific location of the placed text. 
	  * None if no text placement (or value) was found.
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(model.orderIndex.column).int
	
	/**
	  * Unique id of the accessible text placement. None if no text placement was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
}


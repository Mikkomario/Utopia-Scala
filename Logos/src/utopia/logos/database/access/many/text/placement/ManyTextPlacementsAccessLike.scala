package utopia.logos.database.access.many.text.placement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.template.PlacedAccessLike
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target multiple text placements or similar instances at a time
  * @tparam A Type of read (text placements -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyTextPlacementsAccessLike[+A, +Repr] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr] with PlacedAccessLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model: TextPlacementDbProps
	
	
	// COMPUTED	--------------------
	
	/**
	  * parent ids of the accessible text placements
	  */
	def parentIds(implicit connection: Connection) = pullColumn(model.parentId.column).map { v => v.getInt }
	/**
	  * placed ids of the accessible text placements
	  */
	def placedIds(implicit connection: Connection) = pullColumn(model.placedId.column).map { v => v.getInt }
	/**
	  * order indices of the accessible text placements
	  */
	def orderIndices(implicit connection: Connection) =
		pullColumn(model.orderIndex.column).map { v => v.getInt }
	/**
	  * Unique ids of the accessible text placements
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	
	// IMPLEMENTED  ----------------
	
	override protected def orderIndexColumn: Column = model.orderIndex
	
	
	// OTHER	--------------------
	
	/**
	  * @param orderIndices Targeted order indices
	  * @return Copy of this access point that only includes text placements where order index is within the
	  *  specified value set
	  */
	def atIndices(orderIndices: IterableOnce[Int]) = filter(model.orderIndex.column.in(IntSet.from(orderIndices)))
	
	/**
	  * @param placedId placed id to target
	  * @return Copy of this access point that only includes text placements with the specified placed id
	  */
	def placing(placedId: Int) = filter(model.placedId.column <=> placedId)
	/**
	  * @param placedIds Targeted placed ids
	  * @return Copy of this access point that only includes text placements where placed id is within the
	  *  specified value set
	  */
	def placingTexts(placedIds: IterableOnce[Int]) = filter(model.placedId.column.in(IntSet.from(placedIds)))
	
	/**
	  * @param parentId parent id to target
	  * @return Copy of this access point that only includes text placements with the specified parent id
	  */
	def withinText(parentId: Int) = filter(model.parentId.column <=> parentId)
	/**
	  * @param parentIds Targeted parent ids
	  * @return Copy of this access point that only includes text placements where parent id is within the
	  *  specified value set
	  */
	def withinTexts(parentIds: IterableOnce[Int]) = filter(model.parentId.column.in(IntSet.from(parentIds)))
}


package utopia.reflection.container.swing

import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.stack.StackableWrapper
import utopia.reflection.component.swing.{AwtComponentRelated, SwingComponentRelated}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.segmented.{Segmented, SegmentedGroup, SegmentedRowLike}
import utopia.reflection.container.swing.SegmentedRow.RowSegment
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.StackLength

object SegmentedRow
{
	type RowSegment = StackableWrapper with AwtComponentRelated
	
	/**
	  * Creates a new row that becomes a part of the specified group
	  * @param group A group
	  * @param margin Margin between row components
	  * @param cap Cap at each end of the row
	  * @param layout Row layout
	  * @return A new row, already registered to the specified group
	  */
	def partOfGroup[C <: AwtStackable](group: SegmentedGroup, margin: StackLength = StackLength.any,
									   cap: StackLength = StackLength.fixed(0), layout: StackLayout = Fit) =
	{
		val row = new SegmentedRow[C](group, group.direction, margin, cap, layout)
		group.register(row)
		row
	}
	
	/**
	  * Creates a new row with items
	  * @param master The segmented item that determines the proportions of this row
	  * @param items The items for this new row
	  * @param direction Row direction (default = X)
	  * @param margin Margin between row components (default = prefers no margin)
	  * @param cap Cap at each end of the row (default = no cap)
	  * @param layout Row layout (default = Fit)
	  * @return A new row
	  */
	def withItems[C <: AwtStackable](master: Segmented, items: TraversableOnce[C], direction: Axis2D = X,
									 margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixed(0),
									 layout: StackLayout = Fit) =
	{
		val row = new SegmentedRow[C](master, direction, margin, cap, layout)
		row ++= items
		row
	}
	
	/**
	  * Creates a new row that becomes a part of the specified group
	  * @param group A group
	  * @param items The items for the new row
	  * @param margin Margin between row components (default = prefers no margin)
	  * @param cap Cap at each end of the row (default = no cap)
	  * @param layout Row layout (default = Fit)
	  * @return A new row, already registered to the specified group
	  */
	def partOfGroupWithItems[C <: AwtStackable](group: SegmentedGroup, items: TraversableOnce[C], margin: StackLength = StackLength.any,
												cap: StackLength = StackLength.fixed(0), layout: StackLayout = Fit) =
	{
		val row = withItems(group, items, group.direction, margin, cap, layout)
		group.register(row)
		row
	}
}

/**
  * Segmented rows are basically stack panels where each item is a segment and the lengths of the segments can be
  * matched from another segmented source. If you wish to make this row part of that source, use SegmentedGroup and
  * register this row as part of it
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  * @param master The segmented item that determines this row's proportions
  * @param direction The direction of this row (X = horizontal row, Y = vertical column) (default = X)
  * @param margin The margin placed between items (default = prefers no margin)
  * @param cap The cap at each end of this row (default = no cap)
  * @param layout The layout used perpendicular to 'direction' (default = Fit)
  */
class SegmentedRow[C <: AwtStackable](val master: Segmented, override val direction: Axis2D = X,
									  margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixed(0),
									  layout: StackLayout = Fit)
	extends SegmentedRowLike[C, RowSegment] with SwingComponentRelated with CustomDrawableWrapper
{
	// ATTRIBUTES	-----------------
	
	override protected val stack = new Stack[RowSegment](direction, margin, cap, layout)
	
	
	// INITIAL CODE	----------------
	
	addResizeListener(updateLayout())
	startListeningToMasterUpdates()
	
	
	// IMPLEMENTED	-----------------
	
	override def drawable = stack
	
	override protected def addSegmentToStack(segment: Segment) = stack += new AwtComponentSegment(segment)
	
	override protected def removeSegmentFromStack(segment: Segment) = stack.filterNot { _.component == segment.item.component }
	
	override def component = stack.component
	
	
	// NESTED CLASSES	-----------
	
	override def background_=(color: Color) = super[SegmentedRowLike].background_=(color)
	
	private class AwtComponentSegment(val segment: Segment) extends StackableWrapper with AwtComponentRelated
	{
		override def toString = segment.item.toString
		
		override protected def wrapped = segment
		
		override def component = segment.item.component
	}
}
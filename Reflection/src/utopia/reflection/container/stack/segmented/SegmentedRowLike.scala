package utopia.reflection.container.stack.segmented

import utopia.flow.util.CollectionExtensions._
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.template.ComponentWrapper
import utopia.reflection.component.template.layout.stack.{Stackable, StackableWrapper}
import utopia.reflection.container.stack.template.layout.StackLike
import utopia.reflection.container.template.MultiContainer
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.stack.StackLength

/**
  * Segmented rows are basically stacks where each item is a segment and the lengths of the segments can be
  * matched from another segmented source. If you wish to make this row part of that source, use SegmentedGroup and
  * register this row as part of it
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
@deprecated("Segment system updated to Segment and SegmentGroup", "v1.2")
trait SegmentedRowLike[C <: Stackable, C2 <: Stackable] extends MultiContainer[C] with Stackable with Segmented with StackableWrapper
{
	// ATTRIBUTES	-----------------
	
	private val listeningMaster = new VolatileFlag()
	private var segments = Vector[Segment]()
	
	
	// ABSTRACT	---------------------
	
	/**
	  * @return The stack this row uses
	  */
	protected def stack: StackLike[C2]
	
	/**
	  * Adds a segment to the underlying stack
	  * @param segment A segment
	  */
	protected def addSegmentToStack(segment: Segment, index: Int): Unit
	
	/**
	  * Removes a segment from the underlying stack
	  * @param segment A segment
	  */
	protected def removeSegmentFromStack(segment: Segment): Unit
	
	def direction: Axis2D
	
	/**
	  * @return The segmented component that determines the lengths of the segments in this row
	  */
	def master: Segmented
	
	
	// IMPLEMENTED	-----------------
	
	// override def children = super[MultiStackContainer].children
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		super.isAttachedToMainHierarchy_=(newAttachmentStatus)
		// When attached to the main hierarchy, starts listening to updates in master
		if (newAttachmentStatus)
			listeningMaster.runAndSet { master.addSegmentChangedListener(MasterChangeListener) }
		// When removed, stops listening
		else
			listeningMaster.updateIf { s => s } { _ =>
				master.removeSegmentChangedListener(MasterChangeListener)
				false
			}
	}
	
	override protected def wrapped = stack
	
	override def segmentCount = stack.count
	
	override def naturalLengthForSegment(index: Int) = segments.getOption(index).map {
		_.sourceLengthWithMargins }
	
	override def components = segments.map { _.item }
	
	override protected def add(component: C, index: Int) =
	{
		// Wraps the item into segment
		val max = segmentCount
		if (index >= max)
		{
			val segment = new Segment(max, component)
			segments :+= segment
			addSegmentToStack(segment, index)
		}
		else
		{
			// May need to adjust indices of other segments
			segments.drop(index).foreach { _.index += 1 }
			val segment = new Segment(index, component)
			segments = segments.inserted(segment, index)
			addSegmentToStack(segment, index)
		}
	}
	
	override protected def remove(component: C) =
	{
		// Finds the segment first
		segments.indexWhereOption { _.item == component }.foreach { index =>
			val removedSegment = segments(index)
			segments = segments.withoutIndex(index)
			// May update indexing
			segments.drop(index).foreach { _.index -= 1 }
			removeSegmentFromStack(removedSegment)
		}
	}
	
	
	// NESTED CLASSES	-------------
	
	private object MasterChangeListener extends SegmentChangedListener
	{
		override def onSegmentUpdated(source: Segmented) =
		{
			// When another segment is updated, resets component stack sizes
			if (source != SegmentedRowLike.this)
				stack.resetCachedSize()
		}
	}
	
	protected class Segment(var index: Int, val item: C) extends ComponentWrapper with Stackable
	{
		// COMPUTED	-----------------
		
		def isLast = index == segmentCount - 1
		
		def isFirst = index == 0
		
		def sourceLength = item.stackSize.along(direction)
		
		def sourceLengthWithMargins = lengthWithMargins(sourceLength)
		
		
		// IMPLEMENTED	-------------
		
		override def children = item.children
		
		override def stackId = item.stackId
		
		override def updateLayout() = item.updateLayout()
		
		override protected def wrapped = item
		
		override def stackSize =
		{
			// TODO: Maybe add some form of stack size caching?
			
			// Takes default size from master
			// Also applies margin & cap
			val lengthFromMaster = master.naturalLengthForSegment(index).map(lengthWithMargins)
			val source = item.stackSize
			
			lengthFromMaster map { source.withSide(_, direction) } getOrElse source
		}
		
		override def isAttachedToMainHierarchy = item.isAttachedToMainHierarchy
		
		override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) = item.isAttachedToMainHierarchy = newAttachmentStatus
		
		override def stackHierarchyListeners = item.stackHierarchyListeners
		
		override def stackHierarchyListeners_=(newListeners: Vector[StackHierarchyListener]) =
			item.stackHierarchyListeners = newListeners
		
		override def resetCachedSize() =
		{
			// Informs listeners whenever one of rows components gets updated
			informSegmentChanged(SegmentedRowLike.this)
			item.resetCachedSize()
		}
		
		
		// OTHER	----------------
		
		private def lengthWithMargins(base: StackLength) =
		{
			// First and last index apply caps and have margin only on one side
			// 1 item rows have two caps around them
			if (segmentCount == 1)
				base + stack.cap * 2
			else if (isFirst || isLast)
				base + stack.cap + stack.margin / 2
			else
				base + stack.margin
		}
	}
}

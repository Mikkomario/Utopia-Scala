package utopia.reflection.container.stack.segmented

import utopia.flow.async.VolatileFlag
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.stack.{Stackable, StackableWrapper}
import utopia.reflection.component.ComponentWrapper
import utopia.reflection.container.stack.{MultiStackContainer, StackLike}
import utopia.reflection.shape.StackLength

/**
  * Segmented rows are basically stacks where each item is a segment and the lengths of the segments can be
  * matched from another segmented source. If you wish to make this row part of that source, use SegmentedGroup and
  * register this row as part of it
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait SegmentedRowLike[C <: Stackable, C2 <: Stackable] extends MultiStackContainer[C] with Segmented with StackableWrapper
{
	// ATTRIBUTES	-----------------
	
	private val listeningMaster = new VolatileFlag()
	private var lastIndex = -1
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
	protected def addSegmentToStack(segment: Segment)
	
	/**
	  * Removes a segment from the underlying stack
	  * @param segment A segment
	  */
	protected def removeSegmentFromStack(segment: Segment)
	
	def direction: Axis2D
	
	/**
	  * @return The segmented component that determines the lengths of the segments in this row
	  */
	def master: Segmented
	
	
	// IMPLEMENTED	-----------------
	
	override def children = super[MultiStackContainer].children
	
	override protected def wrapped = stack
	
	override def segmentCount = stack.count
	
	override def naturalLengthForSegment(index: Int) = segments.getOption(index).map {
		_.sourceLengthWithMargins }
	
	override def components = segments.map { _.item }
	
	override protected def add(component: C) =
	{
		// Wraps the item first
		val index = lastIndex + 1
		lastIndex = index
		
		val segment = new Segment(index, component)
		
		segments :+= segment
		addSegmentToStack(segment)
	}
	
	override protected def remove(component: C) =
	{
		// Finds the segment first
		segments.find { _.item == component }.foreach
		{
			segment =>
				segments = segments.filterNot { _ == segment }
				// May update indexing
				if (segment.index == lastIndex)
					lastIndex = segments.lastOption.map { _.index } getOrElse -1
				removeSegmentFromStack(segment)
		}
	}
	
	
	// OTHER	---------------------
	
	protected def startListeningToMasterUpdates() = listeningMaster.runAndSet {
		master.addSegmentChangedListener(new MasterChangeListener()) }
	
	
	// NESTED CLASSES	-------------
	
	private class MasterChangeListener extends SegmentChangedListener
	{
		override def onSegmentUpdated(source: Segmented) =
		{
			// When another segment is updated, resets component stack sizes
			if (source != SegmentedRowLike.this)
				stack.resetCachedSize()
		}
	}
	
	protected class Segment(val index: Int, val item: C) extends ComponentWrapper with Stackable
	{
		// COMPUTED	-----------------
		
		def isLast = index == lastIndex
		
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

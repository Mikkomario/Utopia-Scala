package utopia.reflection.container.stack.segmented

import utopia.genesis.shape.Axis2D
import utopia.reflection.shape.StackLength

/**
  * Segmented items can be divided into segments that each have their own length. This trait is used for matching
  * segments between multiple segmented items.
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait Segmented
{
	// ATTRIBUTES	------------------------
	
	private var changeListeners = Vector[SegmentChangedListener]()
	
	
	// ABSTRACT	----------------------------
	
	/**
	  * @return The direction of the segments (X if they're laid out horizontally and Y if vertically)
	  */
	def direction: Axis2D
	
	/**
	  * @return The number of segments in this segmented item
	  */
	def segmentCount: Int
	
	/**
	  * Finds the natrual length of a segment at the specified index
	  * @param index Segment index
	  * @return The natural length of the segment at the specified index. None if this segment doesn't have a segment
	  *         at the index or if the length couldn't be calculated
	  */
	def naturalLengthForSegment(index: Int): Option[StackLength]
	
	
	// OTHER	---------------------------
	
	/**
	  * Informs all listeners that a segment was changed
	  * @param source The source segmented item (default = this)
	  */
	protected def informSegmentChanged(source: Segmented = this) = changeListeners.foreach { _.onSegmentUpdated(source) }
	
	/**
	  * Adds a new listener that will be informed about segment changes
	  * @param listener A listener
	  */
	def addSegmentChangedListener(listener: SegmentChangedListener) = changeListeners :+= listener
	
	/**
	  * Removes a listener
	  * @param listener A listener
	  */
	def removeSegmentChangedListener(listener: Any) = changeListeners = changeListeners.filterNot { _ == listener }
	
	/**
	  * Clears all segment change listeners
	  */
	def clearSegmentChangedListeners() = changeListeners = Vector()
}

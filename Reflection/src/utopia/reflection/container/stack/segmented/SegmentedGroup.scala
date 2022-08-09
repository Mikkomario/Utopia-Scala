package utopia.reflection.container.stack.segmented

import utopia.paradigm.enumeration.Axis.{X, Y}

import scala.math.Ordering.Double.TotalOrdering
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.shape.stack.StackLength

@deprecated("Segment system updated to Segment and SegmentGroup", "v1.2")
object SegmentedGroup
{
	/**
	 * @return A new horizontal segmented group (items have equal widths)
	 */
	def horizontal = new SegmentedGroup(X)
	
	/**
	 * @return A new vertical segmented group (items have equal heights)
	 */
	def vertical = new SegmentedGroup(Y)
}

/**
  * These groups keep track of multiple segmented items in order to match their segment lengths
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
 *  @param direction The direction of the segmented rows in this group
  */
@deprecated("Segment system updated to Segment and SegmentGroup", "v1.2")
class SegmentedGroup(override val direction: Axis2D) extends Segmented
{
	// ATTRIBUTES	-------------------
	
	private var items = Vector[Segmented]()
	
	
	// IMPLEMENTED	-------------------
	
	/**
	  * @return The maximum number of segments in items in this group
	  */
	override def segmentCount =
	{
		if (items.isEmpty) 0 else items.map { _.segmentCount }.max
	}
	
	override def naturalLengthForSegment(index: Int) =
	{
		val lengths = items.flatMap { _.naturalLengthForSegment(index) }
		if (lengths.isEmpty)
			None
		else
		{
			// Combines the stack lengths together with following logic:
			// min = largest min length
			// optimal = largest optimal length (limited by smallest maximum length)
			// max = smallest maximum length
			val min = lengths.map { _.min }.max
			val max = lengths.flatMap { _.max }.minOption
			val rawOptimal = lengths.map { _.optimal }.max
			val optimal = max.map { rawOptimal min _ } getOrElse rawOptimal
			
			Some(StackLength(min, optimal, max, lengths.map { _.priority }.reduce { _ max _ }))
		}
	}
	
	
	// OTHER	-----------------------
	
	/**
	  * Registers a new segmented item to this group
	  * @param item An item to be registered
	  */
	def register(item: Segmented) =
	{
		items :+= item
		item.addSegmentChangedListener(ChangeListener)
	}
	
	/**
	  * Registers multiple new segmented items to this group
	  * @param many New items
	  */
	def register(many: IterableOnce[Segmented]) =
	{
		items ++= many
		items.foreach { _.addSegmentChangedListener(ChangeListener) }
	}
	
	/**
	  * Registers multiple new items to this group
	  * @param first First item
	  * @param second Second item
	  * @param more More items
	  */
	def register(first: Segmented, second: Segmented, more: Segmented*): Unit = register(Vector(first, second) ++ more)
	
	/**
	  * Removes an item from this group
	  * @param item An item to be removed
	  */
	def remove(item: Segmented) =
	{
		item.removeSegmentChangedListener(ChangeListener)
		items = items.filterNot { _ == item }
	}
	
	/**
	  * Removes all items from this group
	  */
	def clear() =
	{
		items.foreach { _.removeSegmentChangedListener(ChangeListener) }
		items = Vector()
	}
	
	
	// NESTED CLASSES	---------------
	
	private object ChangeListener extends SegmentChangedListener
	{
		// Relays segment changed events
		override def onSegmentUpdated(source: Segmented) = informSegmentChanged(source)
	}
}

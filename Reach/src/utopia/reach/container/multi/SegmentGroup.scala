package utopia.reach.container.multi

import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.wrapper.OpenComponent

object SegmentGroup
{
	// COMPUTED -----------------------
	
	/**
	  * @return A segmented group that produces rows
	  */
	def rows = apply(X)
	/**
	  * @return A segmented group that produces columns
	  */
	def columns = apply(Y)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param layouts Layouts to use in row segments (ordered)
	  * @return A new segmented group that produces rows
	  */
	def rowsWithLayouts(layouts: Seq[StackLayout]) = apply(X, layouts)
	/**
	  * @param first First segment layout
	  * @param more More segment layouts
	  * @return A new segmented group that produces rows
	  */
	def rowsWithLayouts(first: StackLayout, more: StackLayout*): SegmentGroup = rowsWithLayouts(first +: more)
	/**
	  * @param layouts Layouts to use in column segments (ordered)
	  * @return A new segmented group that produces columns
	  */
	def columnsWithLayouts(layouts: Seq[StackLayout]) = apply(Y, layouts)
	/**
	  * @param first First segment layout
	  * @param more More segment layouts
	  * @return A new segmented group that produces columns
	  */
	def columnsWithLayouts(first: StackLayout, more: StackLayout*): SegmentGroup =
		columnsWithLayouts(first +: more)
	
	/**
	  * @param rowDirection Direction in which the component rows are laid out.
	  *                     Should match the direction of the segmented stacks.
	  * @param layouts Layouts to use for each segment.
	  *                If a layout is not specified, Fit is used.
	  *                The last layout entry will be used for all the remaining segments.
	  * @return A new segment group
	  */
	def apply(rowDirection: Axis2D, layouts: Seq[StackLayout] = Empty): SegmentGroup =
		new RootSegmentGroup(rowDirection, layouts)
	def apply(rowDirection: Axis2D, firstLayout: StackLayout, moreLayouts: StackLayout*): SegmentGroup =
		apply(rowDirection, firstLayout +: moreLayouts)
		
	
	// NESTED   --------------------
	
	private class RootSegmentGroup(override val rowDirection: Axis2D = X, layouts: Seq[StackLayout] = Empty)
		extends SegmentGroup
	{
		// ATTRIBUTES	---------------------------
		
		private var segments: Seq[Segment] = Empty
		
		private lazy val defaultLayout = layouts.lastOption.getOrElse(Fit)
		
		
		// COMPUTED -------------------------------
		
		override def drop(n: Int): SegmentGroup = if (n <= 0) this else new SegmentSlice(n)
		
		override protected def acquireSegments(count: Int): IterableOnce[(Segment, Int)] =
			ensureSegmentCount(count).zipWithIndex
		
		
		// OTHER    -----------------------------
		
		private def ensureSegmentCount(count: Int) = {
			// Creates more segments, if necessary
			val existingSegmentCount = segments.size
			val missingSegmentCount = count - existingSegmentCount
			if (missingSegmentCount > 0) {
				segments ++= layouts.drop(existingSegmentCount).view.padTo(missingSegmentCount, defaultLayout)
					.map { new Segment(segmentDirection, _) }
				segments.view
			}
			else
				segments.view.take(count)
		}
		
		
		// NESTED   -----------------------------
		
		private class SegmentSlice(start: Int) extends SegmentGroup
		{
			// IMPLEMENTED  ---------------------
			
			override def rowDirection: Axis2D = RootSegmentGroup.this.rowDirection
			
			override def drop(n: Int): SegmentGroup = RootSegmentGroup.this.drop(start + n)
			
			override protected def acquireSegments(count: Int): IterableOnce[(Segment, Int)] =
				ensureSegmentCount(start + count).zipWithIndex.drop(start)
		}
	}
}

/**
  * Used for managing grid-like layouts that consist of rows or columns of segments
  * @author Mikko Hilpinen
  * @since 10.6.2020, v0.1
  */
trait SegmentGroup
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Direction of the component rows in this group.
	  *         E.g. X when used with horizontal stacks.
	  */
	def rowDirection: Axis2D
	
	/**
	  * @param n The number of segments to remove from the beginning of this group
	  * @return A subregion of this segment group, skipping the first 'n' segments in this group
	  */
	def drop(n: Int): SegmentGroup
	
	/**
	  * Acquires n segments for wrapping components
	  * @param count Number of segments to acquire
	  * @return 'count' segments, each accompanied by its index.
	  */
	protected def acquireSegments(count: Int): IterableOnce[(Segment, Int)]
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return Direction in which the segments are align themselves with.
	  *         E.g. if Y, similar segments are placed on top of each other.
	  */
	def segmentDirection: Axis2D = rowDirection.perpendicular
	
	
	// OTHER	-------------------------------
	
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param row Row of hierarchies to host the new components and open components to register & wrap
	  * @return Wrapped components
	  */
	def wrapUnderMany[C <: ReachComponent, R](row: Seq[(ComponentHierarchy, OpenComponent[C, R])]) =
	{
		val parentsIterator = row.iterator.map { _._1 }
		wrap(row.map { _._2 }) { parentsIterator.next() }
	}
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param hierarchy Component hierarchy that will hold the created wrappers
	  * @param row Row of open components to register & wrap
	  * @return Wrapped components
	  */
	def wrapUnderSingle[C <: ReachComponent, R](hierarchy: ComponentHierarchy, row: Seq[OpenComponent[C, R]]) =
		wrap(row)(hierarchy)
	
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param row Row of open components to register & wrap
	  * @param nextHierarchy A function for providing component hierarchies to which the wrapped
	  *                      components will be attached
	  * @return Wrapped components
	  */
	def wrap[C <: ReachComponent, R](row: Seq[OpenComponent[C, R]])(nextHierarchy: => ComponentHierarchy) =
	{
		// Adds each piece of the row into its own segment
		row.view.zip(acquireSegments(row.size))
			.map { case (row, (segment, index)) => segment.wrap(nextHierarchy, row, index) }.toOptimizedSeq
	}
}

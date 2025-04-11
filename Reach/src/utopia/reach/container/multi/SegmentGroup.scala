package utopia.reach.container.multi

import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.wrapper.OpenComponent

object SegmentGroup
{
	/**
	  * @return A segmented group that produces rows
	  */
	def rows = new SegmentGroup()
	/**
	  * @return A segmented group that produces columns
	  */
	def columns = new SegmentGroup(Y)
	
	/**
	  * @param layouts Layouts to use in row segments (ordered)
	  * @return A new segmented group that produces rows
	  */
	def rowsWithLayouts(layouts: Seq[StackLayout]) = new SegmentGroup(layouts = layouts)
	/**
	  * @param first First segment layout
	  * @param second Second segment layout
	  * @param more More segment layouts
	  * @return A new segmented group that produces rows
	  */
	def rowsWithLayouts(first: StackLayout, second: StackLayout, more: StackLayout*): SegmentGroup =
		rowsWithLayouts(Pair(first, second) ++ more)
	/**
	  * @param layouts Layouts to use in column segments (ordered)
	  * @return A new segmented group that produces columns
	  */
	def columnsWithLayouts(layouts: Seq[StackLayout]) = new SegmentGroup(Y, layouts)
	/**
	  * @param first First segment layout
	  * @param second Second segment layout
	  * @param more More segment layouts
	  * @return A new segmented group that produces columns
	  */
	def columnsWithLayouts(first: StackLayout, second: StackLayout, more: StackLayout*): SegmentGroup =
		columnsWithLayouts(Pair(first, second) ++ more)
}

/**
  * Used for managing grid-like layouts that consist of rows or columns of segments
  * @author Mikko Hilpinen
  * @since 10.6.2020, v0.1
  * @param rowDirection Direction of the component rows in this group. Eg. X when used with horizontal stacks.
  *                     Default = X.
  * @param layouts Layouts to be used with different segments, ordered. Default = all use the 'defaultLayout'.
  * @param defaultLayout Layout used for segments not fitting within 'layouts'.
  *                      E.g. If layouts has 2 elements, this layout will be used for the third-nth elements.
  *                      Default = Fit.
  */
class SegmentGroup(val rowDirection: Axis2D = X, layouts: Seq[StackLayout] = Empty, defaultLayout: StackLayout = Fit)
{
	// ATTRIBUTES	---------------------------
	
	private var segments: Seq[Segment] = Empty
	
	
	// COMPUTED -------------------------------
	
	private def segmentDirection = rowDirection.perpendicular
	
	
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
		// Creates more segments, if necessary
		val existingSegmentCount = segments.size
		val missingSegmentCount = row.size - existingSegmentCount
		if (missingSegmentCount > 0)
			segments ++= layouts.drop(existingSegmentCount).view.padTo(missingSegmentCount, defaultLayout)
				.map { new Segment(segmentDirection, _) }
		
		// Adds each piece of the row into its own segment
		row.view.zip(segments).zipWithIndex
			.map { case ((row, segment), index) => segment.wrap(nextHierarchy, row, index) }
			.toOptimizedSeq
	}
	
	// TODO: Implement take, drop & slice. Keep the segments intact.
	/*
	private def copy(rowDirection: Axis2D = rowDirection, layouts: Seq[StackLayout] = layouts,
	         defaultLayout: StackLayout = defaultLayout) =
		new SegmentGroup(rowDirection, layouts, defaultLayout)
		
	 */
}

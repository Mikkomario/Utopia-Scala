package utopia.reflection.container.reach

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.component.reach.wrapper.OpenComponent
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit

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
	def rowsWithLayouts(layouts: Vector[StackLayout]) = new SegmentGroup(layouts = layouts)
	
	/**
	  * @param first First segment layout
	  * @param second Second segment layout
	  * @param more More segment layouts
	  * @return A new segmented group that produces rows
	  */
	def rowsWithLayouts(first: StackLayout, second: StackLayout, more: StackLayout*): SegmentGroup =
		rowsWithLayouts(Vector(first, second) ++ more)
	
	/**
	  * @param layouts Layouts to use in column segments (ordered)
	  * @return A new segmented group that produces columns
	  */
	def columnsWithLayouts(layouts: Vector[StackLayout]) = new SegmentGroup(Y, layouts)
	
	/**
	  * @param first First segment layout
	  * @param second Second segment layout
	  * @param more More segment layouts
	  * @return A new segmented group that produces columns
	  */
	def columnsWithLayouts(first: StackLayout, second: StackLayout, more: StackLayout*): SegmentGroup =
		columnsWithLayouts(Vector(first, second) ++ more)
}

/**
  * Used for managing grid-like layouts that consist of rows or columns of segments
  * @author Mikko Hilpinen
  * @since 10.6.2020, v1.2
  * @param rowDirection Direction of the component rows in this group. Eg. X when used with horizontal stacks.
  *                     Default = X.
  * @param layouts Layouts to be used with different segments, ordered. Default = all use Fit.
  */
class SegmentGroup(val rowDirection: Axis2D = X, layouts: Vector[StackLayout] = Vector())
{
	// ATTRIBUTES	---------------------------
	
	private var segments = Vector[Segment]()
	
	
	// OTHER	-------------------------------
	
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param row Row of hierarchies to host the new components and open components to register & wrap
	  * @return Wrapped components
	  */
	def wrapUnderMany[C <: ReachComponentLike, R](row: Seq[(ComponentHierarchy, OpenComponent[C, R])]) =
	{
		val parentsIterator = row.iterator.map { _._1 }
		wrap(row.map { _._2 }) { parentsIterator.next() }
	}
	
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param parentHierarchy Component hierarchy that will hold the created wrappers
	  * @param row Row of open components to register & wrap
	  * @return Wrapped components
	  */
	def wrapUnderSingle[C <: ReachComponentLike, R](parentHierarchy: ComponentHierarchy, row: Seq[OpenComponent[C, R]]) =
		wrap(row)(parentHierarchy)
	
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param row Row of open components to register & wrap
	  * @param nextHierarchy A function for providing component hierarchies to which the wrapped
	  *                      components will be attached
	  * @return Wrapped components
	  */
	def wrap[C <: ReachComponentLike, R](row: Seq[OpenComponent[C, R]])(nextHierarchy: => ComponentHierarchy) =
	{
		// Adds each piece of the row into its own segment (creates new segments if necessary)
		if (row.size > segments.size)
		{
			val newSegmentLayouts = layouts.drop(segments.size)
			val newSegments = (0 until (row.size - segments.size)).map { idx =>
				new Segment(rowDirection.perpendicular, newSegmentLayouts.getOrElse(idx, Fit))
			}
			segments ++= newSegments
		}
		row.indices.map { i => segments(i).wrap(nextHierarchy, row(i), i + 1) }.toVector
	}
}

package utopia.reflection.container.swing.layout

import utopia.firmament.model.enumeration.StackLayout
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.enumeration.Axis2D
import StackLayout.Fit
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable

/**
  * Used for managing grid-like layouts that consist of rows or columns of segments
  * @author Mikko Hilpinen
  * @since 10.6.2020, v1.2
  * @param rowDirection Direction of the component rows in this group. Eg. X when used with horizontal stacks.
  *                     Default = X.
  * @param layouts Layouts to be used with different segments, ordered. Default = all use Fit.
  */
class SegmentGroup(rowDirection: Axis2D = X, layouts: Vector[StackLayout] = Vector())
{
	// ATTRIBUTES	---------------------------
	
	private var segments = Vector[Segment]()
	
	
	// OTHER	-------------------------------
	
	/**
	  * Registers a new row of items into this group, producing wrapped components
	  * @param row Row to register & wrap
	  * @return Wrapped components
	  */
	def wrap(row: Seq[AwtStackable]) =
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
		segments.zip(row).map { case (segment, component) => segment.wrap(component) }
	}
}

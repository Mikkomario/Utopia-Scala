package utopia.reflection.component.reach

import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel._
import utopia.reflection.component.template.layout.stack.Stackable2

/**
  * A common trait for "Reach" (no-swing) style components
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
trait ReachComponentLike extends Stackable2
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Hierarchy containing all this component's parents
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	override def children: Seq[ReachComponentLike] = Vector()
	
	/**
	  * Paints the contents of this component on some level. This method is called three times in normal component
	  * painting process (once for each draw level)
	  * @param drawer Drawer used for painting this component's area. (0, 0) coordinates should lie at the parent
	  *               component's top-left corner.
	  * @param drawLevel Targeted draw level (background is drawn first, then normal,
	  *                  foreground is drawn above child components)
	  * @param clipZone Limited drawing area. The drawing should be clipped / limited to that area, if specified.
	  */
	protected def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds] = None): Unit
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Window that contains this component
	  */
	def parentWindow = parentHierarchy.parentWindow
	
	/**
	  * @return The absolute (on-screen) position of this component. None if not connected to main component
	  *         hierarchy
	  */
	def absolutePosition = parentHierarchy.absolutePositionModifier.map { position + _ }
	
	/**
	  * @return The position of this component inside the so called top component
	  */
	def positionInTop = position + parentHierarchy.positionToTopModifier
	
	
	// OTHER	-------------------------
	
	/**
	  * Indicates that this component's and its hierarchy's layout should be updated
	  */
	def revalidate() =
	{
		// Resets the cached stack size of this and upper components
		resetCachedSize()
		parentHierarchy.revalidate()
	}
	
	/**
	  * Paints this component and its children
	  * @param drawer Drawer to use for drawing this component. Origin coordinates (0,0) should be located at the
	  *               top-left corner of the parent component.
	  * @param clipZone Bounds where the drawing is limited. None if whole component area should be drawn.
	  */
	def paintWith(drawer: Drawer, clipZone: Option[Bounds] = None): Unit =
	{
		// Paints background and normal levels
		paintContent(drawer, Background, clipZone)
		paintContent(drawer, Normal, clipZone)
		// Paints child components (only those that overlap with the clipping bounds)
		val components = children
		if (components.nonEmpty)
		{
			// Calculates new clipping zone and drawer origin
			val newClipZone = clipZone.map { _ - position }
			val remainingComponents = newClipZone match
			{
				case Some(zone) => components.filter { _.bounds.overlapsWith(zone) }
				case None => components
			}
			if (remainingComponents.nonEmpty)
				drawer.translated(position).disposeAfter { d =>
					remainingComponents.foreach { _.paintWith(d, newClipZone) } }
		}
		// Paints foreground
		paintContent(drawer, Foreground, clipZone)
	}
}

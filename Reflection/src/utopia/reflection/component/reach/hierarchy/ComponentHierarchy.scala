package utopia.reflection.component.reach.hierarchy

import utopia.flow.event.Changing
import utopia.genesis.shape.shape2D.{Bounds, Vector2D}
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.text.Font

import scala.annotation.tailrec

/**
  * Represents a sequence of components that forms a linear hierarchy
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
trait ComponentHierarchy
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The next "block" in this hierarchy (either Left: Canvas at the top or
	  *         Right: an intermediate block + a component associated with that block)
	  */
	def parent: Either[ReachCanvas, (ComponentHierarchy, ReachComponentLike)]
	
	/**
	  * @return A pointer that shows whether this hierarchy is currently active / linked to the top window. Should
	  *         take into account possible parent state.
	  */
	def linkPointer: Changing[Boolean]
	
	/**
	  * @return Whether the link between this component and the parent component should be considered active
	  */
	def isThisLevelLinked: Boolean
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return The canvas at the top of this hierarchy
	  */
	def top: ReachCanvas = parent match
	{
		case Left(canvas) => canvas
		case Right((block, _)) => block.top
	}
	
	/**
	  * @return Whether this hierarchy currently reaches the top component without any broken links
	  */
	def isLinked = linkPointer.value
	
	/**
	  * @return The window that contains this component hierarchy. None if not connected to a window.
	  */
	def parentWindow = top.parentWindow
	
	/**
	  * @return A modifier used when calculating the position of the bottom component (outside this hierarchy)
	  *         relative to hierarchy top
	  */
	def positionToTopModifier: Vector2D = parent match
	{
		case Left(_) => Vector2D.zero
		case Right((block, component)) => block.positionToTopModifier + component.position
	}
	
	/**
	  * @return A modifier used for calculating the absolute position of the bottom component (not on this hierarchy)
	  */
	def absolutePositionModifier = positionToTopModifier + top.absolutePosition
	
	/**
	  * @return A linear component sequence based on this component hierarchy. The higher hierarchy components are
	  *         placed in the beginning and the last element is the first direct parent component. If this hierarchy
	  *         doesn't have parents before the canvas, returns an empty vector.
	  */
	def toVector: Vector[ReachComponentLike] = parent match
	{
		case Right((parentHierarchy, parentComponent)) => parentHierarchy.toVector :+ parentComponent
		case Left(_) => Vector()
	}
	
	
	// OTHER	--------------------------
	
	/**
	  * Revalidates this component hierarchy (provided this part of the hierarchy is currently linked to the main
	  * stack hierarchy)
	  */
	@tailrec
	final def revalidate(layoutUpdateComponents: Seq[ReachComponentLike]): Unit =
	{
		if (isThisLevelLinked)
			parent match
			{
				case Left(canvas) => canvas.revalidate(layoutUpdateComponents)
				case Right((block, component)) =>
					component.resetCachedSize()
					block.revalidate(component +: layoutUpdateComponents)
			}
	}
	
	/**
	  * Revalidates this component hierarchy (provided this part of the hierarchy is currently linked to the main
	  * stack hierarchy), then calls the specified function
	  * @param f A function called once this hierarchy has been updated. Please note that this function might not
	  *          get called at all.
	  */
	@tailrec
	final def revalidateAndThen(layoutUpdateComponents: Seq[ReachComponentLike])(f: => Unit): Unit =
	{
		if (isThisLevelLinked)
			parent match
			{
				case Left(canvas) => canvas.revalidateAndThen(layoutUpdateComponents)(f)
				case Right((block, component)) =>
					component.resetCachedSize()
					block.revalidateAndThen(component +: layoutUpdateComponents)(f)
			}
	}
	
	/**
	  * Repaints the whole component hierarchy (if linked)
	  */
	def repaintAll() = if (isLinked) top.repaint()
	
	/**
	  * Repaints a sub-section of the bottom component (if linked to top)
	  * @param area Area inside the bottom component
	  */
	def repaint(area: => Bounds) =
	{
		if (isLinked)
			top.repaint(area + positionToTopModifier)
	}
	
	/**
	  * Repaints the bottom component
	  */
	def repaintBottom() =
	{
		if (isLinked)
			parent match
			{
				case Left(canvas) => canvas.repaint()
				case Right((block, component)) => block.repaint(component.bounds)
			}
	}
	
	/**
	  * @param font Font to use
	  * @return Metrics for that font
	  */
	// TODO: Refactor this once component class hierarchy has been updated
	def fontMetrics(font: Font) = top.component.getFontMetrics(font.toAwt)
}

package utopia.reflection.component.reach.hierarchy

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.genesis.shape.shape2D.{Bounds, Point, Vector2D}
import utopia.reflection.text.Font

/**
  * Represents a sequence of components that forms a linear hierarchy
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
class ComponentHierarchy(top: HierarchyTop, blocks: Vector[HierarchyBlock])
{
	// ATTRIBUTES	----------------------
	
	private lazy val statusListener = new LinkChangeListener()
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return A pointer that contains whether this hierarchy reaches the top or not
	  */
	def linkPointer = statusListener.pointer.view
	
	/**
	  * @return Whether this hierarchy currently reaches the top component without any broken links
	  */
	def isLinked = linkPointer.value
	
	/**
	  * @return The window that contains this component hierarchy. None if not connected to a window.
	  */
	def parentWindow = top.canvas.parentWindow
	
	/**
	  * @return A modifier used when calculating the position of the bottom component (outside this hierarchy)
	  *         relative to hierarchy top
	  */
	def positionToTopModifier = blocks.foldLeft(Vector2D.zero) { (pos, block) =>
		pos + block.component.position }
	
	/**
	  * @return A modifier used for calculating the absolute position of the bottom component (not on this hierarchy)
	  */
	def absolutePositionModifier = positionToTopModifier + top.canvas.absolutePosition
	
	
	// OTHER	--------------------------
	
	/**
	  * Revalidates this component hierarchy
	  */
	def revalidate() =
	{
		// Won't revalidate non-linked components
		val targetComponents = blocks.reverseIterator.takeWhile { _.isLinked }.toVector
		targetComponents.foreach { _.component.resetCachedSize() }
		if (targetComponents.size == blocks.size)
			top.canvas.revalidate()
	}
	
	/**
	  * Repaints the whole component hierarchy (if linked)
	  */
	def repaintAll() = if (isLinked) top.canvas.repaint()
	
	/**
	  * Repaints a sub-section of the bottom component (if linked to top)
	  * @param area Area inside the bottom component
	  */
	def repaint(area: => Bounds) =
	{
		if (isLinked)
			top.canvas.repaint(area + positionToTopModifier)
	}
	
	/**
	  * Repaints the bottom component
	  */
	def repaintBottom() = blocks.lastOption match
	{
		case Some(block) => repaint(Bounds(Point.origin, block.component.size))
		case None => repaintAll()
	}
	
	/**
	  * @param font Font to use
	  * @return Metrics for that font
	  */
	// TODO: Refactor this once component class hierarchy has been updated
	def fontMetrics(font: Font) = top.canvas.component.getFontMetrics(font.toAwt)
	
	/**
	  * @param block A hierarchy block to be appended to the end of this hierarchy chain
	  * @return A hierarchy with specified block included
	  */
	def +(block: HierarchyBlock) = new ComponentHierarchy(top, blocks :+ block)
	
	
	// NESTED	--------------------------
	
	// Used for updating link status (whether this hierarchy is connected to the top)
	private class LinkChangeListener
	{
		// ATTRIBUTES	------------------
		
		val pointer = new PointerWithEvents(false)
		
		private val blockListeners = blocks.flatMap { _.linkPointer }.map { p =>
			val listener = new BlockListener(p.value)
			p.addListener(listener)
			listener
		}
		
		
		// INITIAL CODE	------------------
		
		top.attachedPointer.addListener { e => updateStatus(e.newValue) }
		updateStatus()
		
		
		// OTHER	----------------------
		
		private def updateStatus(): Unit = updateStatus(top.attachedPointer.value)
		
		private def updateStatus(isTopLinked: Boolean) =
			pointer.value = isTopLinked && blockListeners.forall { _.linked }
		
		
		// NESTED	----------------------
		
		class BlockListener(initialState: Boolean) extends ChangeListener[Boolean]
		{
			var linked = initialState
			
			override def onChangeEvent(event: ChangeEvent[Boolean]) =
			{
				linked = event.newValue
				updateStatus()
			}
		}
	}
}

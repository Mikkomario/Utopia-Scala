package utopia.reflection.component.reach

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener, Changing}
import utopia.genesis.shape.shape2D.Vector2D
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable

/**
  * Represents a sequence of components that forms a linear hierarchy
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
class ComponentHierarchy(topPointer: Changing[Option[AwtStackable]],
						 blocks: Vector[HierarchyBlock])
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
	def parentWindow = topPointer.value.flatMap { _.parentWindow }
	
	/**
	  * @return A modifier used when calculating the position of the bottom component (outside this hierarchy)
	  *         relative to hierarchy top
	  */
	def positionToTopModifier = blocks.foldLeft(Vector2D.zero) { (pos, block) =>
		pos + block.component.position }
	
	/**
	  * @return A modifier used for calculating the absolute position of the bottom component (not on this hierarchy)
	  */
	def absolutePositionModifier = topPointer.value.map { c =>
		c.absolutePosition.toVector + positionToTopModifier }
	
	
	// OTHER	--------------------------
	
	/**
	  * Revalidates this component hierarchy
	  */
	def revalidate() =
	{
		// Resets cached stack sizes
		blocks.reverseIterator.foreach { _.component.resetCachedSize() }
		// Updates top component stack size & layout
		topPointer.value.foreach { c => c.revalidate() }
	}
	
	/**
	  * @param block A hierarchy block to be appended to the end of this hierarchy chain
	  * @return A hierarchy with specified block included
	  */
	def +(block: HierarchyBlock) = new ComponentHierarchy(topPointer, blocks :+ block)
	
	
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
		
		
		// OTHER	----------------------
		
		def updateStatus() = pointer.value = TopListener.hasTop && blockListeners.forall { _.linked }
		
		
		// NESTED	----------------------
		
		object TopListener extends ChangeListener[Option[Any]]
		{
			var hasTop = topPointer.value.isDefined
			
			override def onChangeEvent(event: ChangeEvent[Option[Any]]) =
			{
				hasTop = event.newValue.isDefined
				updateStatus()
			}
		}
		
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

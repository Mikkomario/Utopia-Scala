package utopia.reflection.component.stack

import utopia.genesis.color.Color
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.ComponentLike
import utopia.reflection.component.swing.AwtComponentRelated
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.{StackHierarchyManager, StackLayout}
import utopia.reflection.container.swing.{AlignFrame, Framing, Stack}
import utopia.reflection.shape.{Alignment, StackInsets, StackLength, StackSize}
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.shape2D.Direction2D
import utopia.reflection.shape.Alignment.Center

object Stackable
{
	// AwtComponent stackables can be stacked & framed easily
	implicit class AwtStackable[S <: Stackable with AwtComponentRelated](val s: S) extends AnyVal
	{
		/**
		  * Creates a stack with this item along with some others
		  * @param elements Other elements
		  * @param axis Stack axis
		  * @param margin The margin between items (defaults to any, preferring 0)
		  * @param cap The cap at each end of stack (default = no cap = fixed to 0)
		  * @param layout The stack layout (default = Fit)
		  * @tparam S2 Stack element type
		  * @return A new stack
		  */
		def stackWith[S2 >: S <: Stackable with AwtComponentRelated](elements: Seq[S2], axis: Axis2D,
																	 margin: StackLength = StackLength.any,
																	 cap: StackLength = StackLength.fixed(0),
																	 layout: StackLayout = Fit) =
			Stack.withItems(s +: elements, axis, margin, cap, layout)
		
		/**
		  * Creates a horizontal stack with this item along with some others
		  * @param elements Other elements
		  * @param margin Margin between elements (defaults to any, preferring 0)
		  * @param cap Cap at each end of the stack (default = fixed to 0)
		  * @param layout Stack layout (default = Fit)
		  * @tparam S2 Stack element type
		  * @return A new stack with these items
		  */
		def rowWith[S2 >: S <: Stackable with AwtComponentRelated](elements: Seq[S2],
																   margin: StackLength = StackLength.any,
																   cap: StackLength = StackLength.fixed(0),
																   layout: StackLayout = Fit) =
			s.stackWith(elements, X, margin, cap, layout)
		
		/**
		  * Creates a vertical stack with this item along with some others
		  * @param elements Other elements
		  * @param margin margin between elements (defaults to any, preferring 0)
		  * @param cap Cap at each end of the stack (default = fixed to 0)
		  * @param layout Stack layout (default = Fit)
		  * @tparam S2 Stack element type
		  * @return A new stack with these items
		  */
		def columnWith[S2 >: S <: Stackable with AwtComponentRelated](elements: Seq[S2],
																	  margin: StackLength = StackLength.any,
																	  cap: StackLength = StackLength.fixed(0),
																	  layout: StackLayout = Fit) =
			s.stackWith(elements, Y, margin, cap, layout)
		
		/**
		  * Frames this item
		  * @param margins The symmetric margins placed around this item
		  * @return A framing with this item inside it
		  */
		def framed(margins: StackSize) = Framing.symmetric(s, margins)
		
		/**
		  * Frames this item
		  * @param insets The insets/margins placed around this item
		  * @return A framing with this item inside it
		  */
		def framed(insets: StackInsets) = new Framing(s, insets)
		
		/**
		  * Frames this item
		  * @param margins The symmetric margins placed around this item
		  * @param color Background color of the framing
		  * @return A framing with this item inside it
		  */
		def framed(margins: StackSize, color: Color) =
		{
			val framing = Framing.symmetric(s, margins)
			framing.background = color
			framing
		}
		
		/**
		  * Frames this item
		  * @param insets The insets/margins placed around this item
		  * @param color Background color of the framing
		  * @return A framing with this item inside it
		  */
		def framed(insets: StackInsets, color: Color) =
		{
			val framing = new Framing(s, insets)
			framing.background = color
			framing
		}
		
		/**
		  * @param alignment Target alignment
		  * @param useLowPriorityLength Whether to use low priority for affected direction's length
		  * @return A frame whether this component is aligned according to specified alignment
		  */
		def aligned(alignment: Alignment, useLowPriorityLength: Boolean = false) = AlignFrame(s, alignment, useLowPriorityLength)
		
		/**
		 * @param side Target side
		  * @param useLowPriorityLength Whether to use low priority for affected direction's length
		 * @return This item framed so that it will be placed to specified side of container
		 */
		def alignedToSide(side: Direction2D, useLowPriorityLength: Boolean = false) = aligned(
			Alignment forDirection side, useLowPriorityLength)
		
		/**
		 * @return This item wrapped in a frame that places it at the center
		 */
		def alignedToCenter = aligned(Center)
	}
}

/**
* This trait is inherited by component classes that can be placed in stacks
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait Stackable extends ComponentLike
{
	// ABSTRACT	---------------------
	
	/**
	  * Updates the layout (and other contents) of this stackable instance. This method will be called if the component,
	  * or its child is revalidated. The stack sizes of this component, as well as those of revalidating children
	  * should be reset at this point.
	  */
	def updateLayout(): Unit
	
	/**
	  * The current sizes of this wrapper. Invisible wrappers should always have a stack size of zero.
	  */
	def stackSize: StackSize
	
	/**
	  * Resets cached stackSize, if there is one, so that it will be recalculated when requested next time
	  */
	def resetCachedSize(): Unit
	
	/**
	  * @return A unique identifier for this stackable instance. These id's are used in stack hierarchy to
	  *         distinquish between items. If this stackable simply wraps another item, it should use the same id,
	  *         otherwise the id should be unique (usually it is enough to return hashCode).
	  */
	def stackId: Int
	
	/**
	  * Child components under this stackable instance (all of which should be stackable)
	  */
	override def children: Seq[Stackable] = Vector()
	
	/**
	  * @return Whether this stackable instance is currently attached to the main stack hierarchy
	  */
	def isAttachedToMainHierarchy: Boolean
	/**
	  * Changes whether this stackable instance is currently attached to the main stack hierarchy
	  * (should affect the children as well). This method is called for informing this stackable, not to cause a change
	  * in the main stack hierarchy.
	  * @param newAttachmentStatus Whether this stackable instance is now connected to the main stack hierarchy
	  */
	def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean)
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return Whether this component is now larger than its maximum size
	  */
	def isOverSized = stackSize.maxWidth.exists { _ < width } || stackSize.maxHeight.exists { _ < height }
	
	/**
	  * @return Whether this component is now smaller than its minimum size
	  */
	def isUnderSized = width < stackSize.minWidth || height < stackSize.minHeight
	
	/**
	  * @return A description of this item's (and all its children) stack attachment status (true or false)
	  */
	def attachmentDescription: String =
	{
		val base = s"${getClass.getSimpleName}:$isAttachedToMainHierarchy"
		val c = children
		if (c.isEmpty)
			base
		else if (c.size == 1)
			s"$base -> ${c.head.attachmentDescription}"
		else
			s"$base -> [${c.map { _.attachmentDescription }.mkString(", ")}]"
	}
	
	
	// OTHER	---------------------
	
	/**
	  * Requests a revalidation for this item (only affects this item after connected to the main stack hierarchy)
	  */
	def revalidate() =
	{
		if (isAttachedToMainHierarchy)
			StackHierarchyManager.requestValidationFor(this)
	}
	
	/**
	 * Sets the size of this component to optimal (by stack size)
	 */
	def setToOptimalSize() = size = stackSize.optimal
	
	/**
	 * Sets the size of this component to minimum (by stack size)
	 */
	def setToMinSize() = size = stackSize.min
	
	/**
	  * Detaches this stackable instance from the main stack hierarchy. If this instance was not connected to said
	  * hierarchy, does nothing.
	  */
	def detachFromMainStackHierarchy() =
	{
		if (isAttachedToMainHierarchy)
		{
			StackHierarchyManager.unregister(this)
			isAttachedToMainHierarchy = false
		}
	}
	
	/**
	  * Attaches this instance to a stack hierarchy represented by a parent component. Registers this new
	  * connection only if the parent is already attached to the main stack hierarchy.
	  * @param parent Parent for this stackable to register under.
	  */
	def attachToStackHierarchyUnder(parent: Stackable) =
	{
		if (parent.isAttachedToMainHierarchy)
		{
			StackHierarchyManager.registerConnection(parent, this)
			isAttachedToMainHierarchy = true
			// Revalidates this item upon attachment since it may be that some revalidations were skipped in the meanwhile
			revalidate()
		}
		// If this component was attached to the main stack hierarchy under a different parent, disconnects from that one
		else
			detachFromMainStackHierarchy()
	}
}

package utopia.reflection.component.template.layout.stack

import utopia.paradigm.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.{StackHierarchyManager, StackLayout}
import utopia.reflection.shape.Alignment
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.enumeration.Direction2D
import utopia.genesis.util.Fps
import utopia.reflection.component.context.AnimationContextLike
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.ComponentLike
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.{AlignFrame, AnimatedSizeContainer, Framing}
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.stack.{StackInsets, StackLength, StackSize}
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration

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
		  * Frames this item
		  * @param sideLength Frame side length on each side
		  * @param color Frame background color
		  * @return A new framing with this item inside it
		  */
		def framed(sideLength: StackLength, color: Color): Framing[S] = framed(StackInsets.symmetric(sideLength), color)
		
		/**
		  * Frames this item, using a rounded background shape
		  * @param insets Insets placed round this item
		  * @param color Background color to use
		  * @return A framing that contains this item and draws a rounded background shape
		  */
		def inRoundedFraming(insets: StackInsets, color: Color) =
		{
			val framing = new Framing(s, insets)
			framing.addRoundedBackgroundDrawing(color)
			framing
		}
		
		/**
		  * Frames this item, using a rounded background shape
		  * @param sideLength The length of each side margin
		  * @param color Background color to use
		  * @return A framing that contains this item and draws a rounded background shape
		  */
		def inRoundedFraming(sideLength: StackLength, color: Color): Framing[S] =
			inRoundedFraming(StackInsets.symmetric(sideLength), color)
		
		/**
		  * Frames this item, using a rounded background shape
		  * @param sides The length of each side margin
		  * @param color Background color to use
		  * @return A framing that contains this item and draws a rounded background shape
		  */
		def inRoundedFraming(sides: StackSize, color: Color): Framing[S] =
			inRoundedFraming(StackInsets.symmetric(sides), color)
		
		/**
		  * @param alignment Target alignment
		  * @return A frame whether this component is aligned according to specified alignment
		  */
		def aligned(alignment: Alignment) = AlignFrame(s, alignment)
		
		/**
		 * @param side Target side
		 * @return This item framed so that it will be placed to specified side of container
		 */
		def alignedToSide(side: Direction2D) = aligned(Alignment forDirection side)
		
		/**
		 * @return This item wrapped in a frame that places it at the center
		 */
		def alignedToCenter = aligned(Center)
		
		/**
		  * @return This item wrapped in a frame that places it at the right hand side
		  */
		def alignedToRight = aligned(Alignment.Right)
		
		/**
		  * @return This item wrapped in a frame that places it at the left hand side
		  */
		def alignedToLeft = aligned(Alignment.Left)
		
		/**
		  * @param context Component creation context (implicit)
		  * @return A copy of this component wrapped in a container that animates size changes
		  */
		def withAnimatedSize(implicit context: AnimationContextLike) =
			AnimatedSizeContainer.contextual(s)
		
		/**
		  * @param actorHandler An actor handler to deliver action events
		  * @param transitionDuration Duration of each size transition (defaults to global default)
		  * @param maxRefreshRate Maximum size refresh rate (defaults to global default)
		  * @return This component wrapped in a component that animates its size adjustments
		  */
		def withAnimatedSizeUsing(actorHandler: ActorHandler,
							 transitionDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
							 maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate) =
			AnimatedSizeContainer(s, actorHandler, transitionDuration, maxRefreshRate)
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
	  * in the main stack hierarchy. The component should fire a stack hierarchy change event from inside this method
	  * when/if it recognizes that its state has changed.
	  * @param newAttachmentStatus Whether this stackable instance is now connected to the main stack hierarchy
	  */
	def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean): Unit
	
	/**
	  * @return Stack hierarchy listeners currently registered for this component
	  */
	def stackHierarchyListeners: Vector[StackHierarchyListener]
	/**
	  * Updates the stack hierarchy listeners currently registered for this component
	  * @param newListeners New stack hierarchy listeners to receive events from this component's state changes
	  */
	def stackHierarchyListeners_=(newListeners: Vector[StackHierarchyListener]): Unit
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return Optimal width for this component
	  */
	def optimalWidth = stackSize.width.optimal
	
	/**
	  * @return Optimal height for this component
	  */
	def optimalHeight = stackSize.height.optimal
	
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
	
	/**
	  * Informs all of the current stack hierarchy listeners about this component's new connection state. Should be
	  * called whenever this component's stack hierarchy connection state changes.
	  * @param newAttachmentStatus This component's new stack hierarchy connection state
	  *                            (true=connected, false=disconnected)
	  */
	protected def fireStackHierarchyChangeEvent(newAttachmentStatus: Boolean) =
		stackHierarchyListeners.foreach { _.onComponentAttachmentChanged(newAttachmentStatus) }
	
	/**
	  * Registers a stack hierarchy change listener to be informed about stack hierarchy changes concerning this
	  * component
	  * @param listener A stack hierarchy change listener
	  * @param callIfAttached Whether the specified listener should be called immediately in case this component
	  *                       is already attached to the main stack hierarchy. Default = false.
	  */
	def addStackHierarchyChangeListener(listener: StackHierarchyListener, callIfAttached: Boolean = false) =
	{
		val currentListeners = stackHierarchyListeners
		if (!currentListeners.contains(listener))
		{
			stackHierarchyListeners = currentListeners :+ listener
			// May trigger the listener if this component is already attached to the stack hierarchy
			// (callIfAttached must be enabled, though)
			if (callIfAttached && isAttachedToMainHierarchy)
				listener.onComponentAttachmentChanged(newAttachmentStatus = true)
		}
	}
	
	/**
	  * Removes a stack hierarchy listener from receiving any more events regarding this component's state changes
	  * @param listener A listener to remove from this component
	  */
	def removeStackHierarchyChangeListener(listener: StackHierarchyListener) =
		stackHierarchyListeners = stackHierarchyListeners.filterNot { _ == listener }
	
	/**
	  * Registers the specified function to be called whenever this component is attached to the main stack hierarchy
	  * @param listener A function to be called whenever this component is attached to the main stack hierarchy.
	  *                 The function will be called immediately in case this component is already attached to the
	  *                 main stack hierarchy.
	  * @tparam U Arbitrary result type
	  */
	def addStackHierarchyAttachmentListener[U](listener: => U) =
		addStackHierarchyChangeListener(StackHierarchyListener.attachmentListener(listener), callIfAttached = true)
	
	/**
	  * Registers the specified function to be called whenever this component is detached from the main stack hierarchy
	  * @param listener A function to be called whenever this component is detached from the main stack hierarchy
	  * @tparam U Arbitrary result type
	  */
	def addStackHierarchyDetachmentListener[U](listener: => U) =
		addStackHierarchyChangeListener(StackHierarchyListener.detachmentListener(listener))
	
	/**
	  * Calls the specified function once this component is next time attached to the main stack hierarchy
	  * @param f Function to call when this component gets attached to the main stack hierarchy
	  * @tparam A Function result type
	  * @return A future with eventual function results
	  */
	def onNextStackHierarchyAttachment[A](f: => A) = onNextStackHierarchyChange(targetState = true)(f)
	
	/**
	  * Calls the specified function once this component is next time detached from the main stack hierarchy
	  * @param f Function to call when this component gets detached from the main stack hierarchy
	  * @tparam A Function result type
	  * @return A future with eventual function results
	  */
	def onNextStackHierarchyDetachment[A](f: => A) = onNextStackHierarchyChange(targetState = false)(f)
	
	private def onNextStackHierarchyChange[A](targetState: Boolean)(f: => A) =
	{
		val promise = Promise[A]()
		addStackHierarchyChangeListener(new SingleUseStackHierarchyListener({ newStatus =>
			if (newStatus == targetState)
			{
				promise.trySuccess(f)
				true
			}
			else
				false
		}))
		promise.future
	}
	
	/**
	  * Calls the specified function as soon as this component is attached to the main stack hierarchy. If this
	  * component is already attached, calls the function immediately.
	  * @param f Function to call once this component is attached to the main stack hierarchy
	  * @tparam A Function result type
	  * @return A future with eventual function results (may already be completed)
	  */
	def onceAttachedToStackHierarchy[A](f: => A) = onNextStackHierarchyState(targetState = true)(f)
	
	/**
	  * Calls the specified function as soon as this component is not attached to the main stack hierarchy. If this
	  * component is already detached, calls the function immediately.
	  * @param f Function to call once this component is detached from the main stack hierarchy
	  * @tparam A Function result type
	  * @return A future with eventual function results (may already be completed)
	  */
	def onceDetachedFromStackHierarchy[A](f: => A) = onNextStackHierarchyState(targetState = false)(f)
	
	private def onNextStackHierarchyState[A](targetState: Boolean)(f: => A) =
	{
		if (isAttachedToMainHierarchy == targetState)
			Future.successful[A](f)
		else
			onNextStackHierarchyChange(targetState)(f)
	}
	
	
	// NESTED	------------------------------------
	
	private class SingleUseStackHierarchyListener(f: Boolean => Boolean) extends StackHierarchyListener
	{
		override def onComponentAttachmentChanged(newAttachmentStatus: Boolean) =
		{
			// If the function completes successfully, detaches this listener from the owner component
			if (f(newAttachmentStatus))
				removeStackHierarchyChangeListener(this)
		}
	}
}

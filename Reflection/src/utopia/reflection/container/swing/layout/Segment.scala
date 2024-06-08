package utopia.reflection.container.swing.layout

import utopia.firmament.model.enumeration.StackLayout
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.Panel
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.event.StackHierarchyListener
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Segments are used for aligning multiple components from different stacks / containers so that their lengths
  * along a specific axis match
  * @author Mikko Hilpinen
  * @since 10.6.2020, v1.2
  * @param direction The direction of this segment (whether components are stacked on top of each other (Y)
  *                  or next to each other (X)). The items are aligned along the <b>perpendicular</b> axis
  *                  (Eg. vertical segment components each have same width). Default = Y
  */
class Segment(direction: Axis2D = Y, layout: StackLayout = Fit)
{
	// ATTRIBUTES	-----------------------------
	
	/**
	  * The axis the components in this segment are aligned by (axis on which they have shared length)
	  */
	val alignAxis = direction.perpendicular
	
	private var containers: Seq[SegmentContainer] = Empty
	private val lengthCache = ResettableLazy(calculatedLength)
	
	
	// COMPUTED	--------------------------------
	
	private def calculatedLength: StackLength =
	{
		containers.map { _.wrappedComponent.stackSize(alignAxis) }.reduceOption { (a, b) =>
			val min = a.min max b.min
			val max = (a.max ++ b.max).reduceOption { _ min _ }
			val optimal =
			{
				val baseOptimal = a.optimal max b.optimal
				if (baseOptimal < min)
					min
				else
					max.filter { _ < baseOptimal }.getOrElse(baseOptimal)
			}
			val priority = a.priority max b.priority
			StackLength(min, optimal, max, priority)
		}.getOrElse(StackLength.any)
	}
	
	
	// OTHER	---------------------------------
	
	/**
	  * Wraps a component in a container that aligns with the other elements in this segment
	  * @param component Component to wrap
	  * @return Container that now wraps the specified component
	  */
	def wrap(component: AwtStackable): AwtStackable = new SegmentContainer(component)
	
	private def updateContainers() =
	{
		lengthCache.reset()
		containers.foreach { _.isUpdatingFlag.set() }
		StackHierarchyManager.requestValidationFor(containers)
	}
	
	private def registerContainer(container: SegmentContainer) =
	{
		containers :+= container
		updateContainers()
	}
	
	private def removeContainer(container: Any) =
	{
		containers = containers.filterNot { _ == container }
		updateContainers()
	}
	
	
	// NESTED	---------------------------------
	
	private class SegmentContainer(val wrappedComponent: AwtStackable)
		extends AwtComponentWrapperWrapper with ReflectionStackable
	{
		// ATTRIBUTES	-------------------------
		
		private val panel = new Panel[AwtStackable]
		
		val isUpdatingFlag = new VolatileFlag()
		private var _isAttached = false
		
		override var stackHierarchyListeners: Seq[StackHierarchyListener] = Empty
		
		
		// INITIAL CODE	-------------------------
		
		panel += wrappedComponent
		
		addResizeListener { _ => setContentBounds() }
		
		
		// IMPLEMENTED	-------------------------
		
		override lazy val stackId = hashCode()
		
		override def children = Single(wrappedComponent)
		
		override def resetCachedSize() =
		{
			// If needs to reset cached size while not in update mode, revalidates all segment containers and not
			// just this one
			if (isAttachedToMainHierarchy && isUpdatingFlag.isNotSet)
				updateContainers()
		}
		
		override def stackSize = wrappedComponent.stackSize.withSide(lengthCache.value, alignAxis)
		
		override protected def wrapped = panel
		
		override def updateLayout() =
		{
			// Ends possible update process in updateLayout()
			isUpdatingFlag.reset()
			// Sets component position and size
			setContentBounds()
		}
		
		override def isAttachedToMainHierarchy = _isAttached
		
		override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		{
			// Registers or unregisters this container based on attachment status
			// Also informs the child component
			if (_isAttached != newAttachmentStatus)
			{
				_isAttached = newAttachmentStatus
				fireStackHierarchyChangeEvent(newAttachmentStatus)
				if (newAttachmentStatus)
				{
					wrappedComponent.attachToStackHierarchyUnder(this)
					registerContainer(this)
				}
				else
				{
					wrappedComponent.isAttachedToMainHierarchy = newAttachmentStatus
					removeContainer(this)
				}
			}
		}
		
		
		// OTHER	----------------------------------
		
		private def setContentBounds() =
		{
			val mySize = size
			val myLength = size(alignAxis)
			val contentLength = wrappedComponent.stackSize(alignAxis)
			// May reposition content if it would be scaled above optimal length
			if (layout != Fit && contentLength.optimal < myLength)
			{
				val newContentLength = contentLength.optimal
				val newLocation = if (layout == Leading) 0.0 else myLength - newContentLength
				val myBreadth = size(direction)
				wrappedComponent.bounds = Bounds(Point(newLocation, 0, alignAxis),
					Size(newContentLength, myBreadth, alignAxis))
			}
			else
				wrappedComponent.bounds = Bounds(Point.origin, mySize)
		}
	}
}

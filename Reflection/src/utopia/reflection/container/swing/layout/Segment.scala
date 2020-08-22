package utopia.reflection.container.swing.layout

import utopia.flow.async.VolatileFlag
import utopia.flow.datastructure.mutable.Lazy
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.container.stack.StackLayout.{Fit, Leading}
import utopia.reflection.container.stack.{StackHierarchyManager, StackLayout}
import utopia.reflection.container.swing.Panel
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.shape.StackLength

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
	
	private var containers = Vector[SegmentContainer]()
	private val lengthCache = Lazy(calculatedLength)
	
	
	// COMPUTED	--------------------------------
	
	private def calculatedLength: StackLength =
	{
		containers.map { _.wrappedComponent.stackSize.along(alignAxis) }.reduceOption { (a, b) =>
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
	
	private class SegmentContainer(val wrappedComponent: AwtStackable) extends AwtComponentWrapperWrapper with Stackable
	{
		// ATTRIBUTES	-------------------------
		
		private val panel = new Panel[AwtStackable]
		
		val isUpdatingFlag = new VolatileFlag()
		private var _isAttached = false
		
		
		// INITIAL CODE	-------------------------
		
		panel += wrappedComponent
		
		addResizeListener { _ => setContentBounds() }
		
		
		// IMPLEMENTED	-------------------------
		
		override lazy val stackId = hashCode()
		
		override def children = Vector(wrappedComponent)
		
		override def resetCachedSize() =
		{
			// If needs to reset cached size while not in update mode, revalidates all segment containers and not
			// just this one
			if (isAttachedToMainHierarchy && isUpdatingFlag.notSet)
				updateContainers()
		}
		
		override def stackSize = wrappedComponent.stackSize.withSide(lengthCache.get, alignAxis)
		
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
			val myLength = size.along(alignAxis)
			val contentLength = wrappedComponent.stackSize.along(alignAxis)
			// May reposition content if it would be scaled above optimal length
			if (layout != Fit && contentLength.optimal < myLength)
			{
				val newContentLength = contentLength.optimal
				val newLocation = if (layout == Leading) 0.0 else myLength - newContentLength
				val myBreadth = size.along(direction)
				wrappedComponent.bounds = Bounds(Point(newLocation, 0, alignAxis),
					Size(newContentLength, myBreadth, alignAxis))
			}
			else
				wrappedComponent.bounds = Bounds(Point.origin, mySize)
		}
	}
}

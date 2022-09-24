package utopia.reach.container.multi.stack

import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.genesis.util.Drawer
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.{Fit, Leading}
import utopia.reflection.shape.stack.StackLength

/**
  * Segments are used for aligning multiple components from different stacks / containers so that their lengths
  * along a specific axis match
  * @author Mikko Hilpinen
  * @since 10.6.2020, v0.1
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
	private val lengthCache = ResettableLazy(calculatedLength)
	
	
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
	  * @param parentHierarchy Hierarchy that will contain the segment wrapper
	  * @param component (Open) component to wrap
	  * @param index Index of this segment
	  * @return Container that now wraps the specified component (as a wrap result)
	  */
	def wrap[C <: ReachComponentLike, R](parentHierarchy: ComponentHierarchy, component: OpenComponent[C, R], index: Int) =
	{
		val container: ReachComponent = new SegmentContainer(parentHierarchy, component, index)
		component.attachTo(container)
	}
	
	private def updateContainers() =
	{
		lengthCache.reset()
		containers.foreach { _.isUpdatingFlag.set() }
		containers.foreach { _.revalidate() }
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
	
	private class SegmentContainer(override val parentHierarchy: ComponentHierarchy,
								   val wrappedComponent: ReachComponentLike, index: Int) extends ReachComponent
	{
		// ATTRIBUTES	-------------------------
		
		val isUpdatingFlag = new VolatileFlag()
		
		
		// INITIAL CODE	-------------------------
		
		addHierarchyListener { isAttached =>
			if (isAttached)
				registerContainer(this)
			else
				removeContainer(this)
		}
		
		
		// IMPLEMENTED	-------------------------
		
		override def transparent = true
		
		override def toString = s"Segment($index)"
		
		override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) = ()
		
		override def calculatedStackSize = wrappedComponent.stackSize.withSide(lengthCache.value, alignAxis)
		
		override def children = Vector(wrappedComponent)
		
		override def resetCachedSize() =
		{
			super.resetCachedSize()
			// If needs to reset cached size while not in update mode, revalidates all segment containers and not
			// just this one
			if (parentHierarchy.isLinked && isUpdatingFlag.isNotSet)
				updateContainers()
		}
		
		override def updateLayout() =
		{
			// Ends possible update process in updateLayout()
			isUpdatingFlag.reset()
			// Sets component position and size
			setContentBounds()
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

package utopia.reach.container.multi

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteReachComponent, ReachComponent}
import utopia.reach.component.wrapper.OpenComponent

/**
  * Segments are used for aligning multiple components from different stacks / containers so that their lengths
  * along a specific axis match
  * @author Mikko Hilpinen
  * @since 10.6.2020, v0.1
  * @param direction The direction of this segment (whether components are stacked on top of each other (Y)
  *                  or next to each other (X)). The items are aligned along the <b>perpendicular</b> axis
  *                  (E.g. vertical segment components each have same width). Default = Y
  */
class Segment(direction: Axis2D = Y, layout: StackLayout = Fit)
{
	// ATTRIBUTES	-----------------------------
	
	/**
	  * The axis the components in this segment are aligned by (i.e. axis along which they have shared length)
	  */
	val alignAxis = direction.perpendicular
	
	private val visibleContainersP = Volatile.eventful.emptySeq[SegmentContainer]
	@deprecated
	private var visibleContainers: Seq[SegmentContainer] = Empty
	private val lengthCache = ResettableLazy(calculatedLength)
	
	private val trackedLinkFlagsP = Volatile(Set[Flag]())
	/**
	  * Lists detached containers for each associated (detached) component hierarchy.
	  * Once / if the hierarchy becomes active (i.e. is attached),
	  * that hierarchy and the associated components are removed from this map and attached as visible containers.
	  */
	private val detachedContainersPp = Volatile(Map[Flag, Seq[SegmentContainer]]())
	
	
	// INITIAL CODE ----------------------------
	
	visibleContainersP.addListener { e => updateContainers(e.newValue) }
	
	
	// COMPUTED	--------------------------------
	
	private implicit def log: Logger = ComponentCreationDefaults.componentLogger
	
	private def calculatedLength: StackLength = {
		val containers = visibleContainersP.value
		if (containers.isEmpty)
			StackLength.any
		else
			containers.iterator.map { _.wrappedComponent.stackSize.along(alignAxis) }.reduce { (a, b) =>
				val min = a.min max b.min
				val max = (a.max ++ b.max).reduceOption { _ min _ }
				val optimal = {
					val baseOptimal = a.optimal max b.optimal
					if (baseOptimal < min)
						min
					else
						max.filter { _ < baseOptimal }.getOrElse(baseOptimal)
				}
				val priority = a.priority max b.priority
				StackLength(min, optimal, max, priority)
			}
	}
	
	
	// OTHER	---------------------------------
	
	/**
	  * Wraps a component in a container that aligns with the other elements in this segment
	  * @param hierarchy Hierarchy that will contain the segment wrapper
	  * @param component (Open) component to wrap
	  * @param index Index of this segment
	  * @return Container that now wraps the specified component (as a wrap result)
	  */
	def wrap[C <: ReachComponent, R](hierarchy: ComponentHierarchy, component: OpenComponent[C, R], index: Int) =
	{
		// Creates the wrapping container and attaches the component to it
		val container = new SegmentContainer(hierarchy, component, index)
		val result = component.attachTo[ConcreteReachComponent](container)
		
		// Starts tracking the hierarchy's linked state, unless being tracked already
		val linkedFlag = hierarchy.linkedFlag
		linkedFlag.fixedValue match {
			// Case: Hierarchy attachment status won't change
			//       => Won't add tracking and adds the container, unless this is a forever detached hierarchy
			case Some(fixedLinkState) =>
				if (fixedLinkState)
					markContainerAsVisible(container)
				
			// Case: Hierarchy attachment status may change (default)
			case None =>
				// Remembers that this hierarchy is tracked
				val isNewHierarchy = trackedLinkFlagsP.mutate { tracked =>
					if (tracked.contains(linkedFlag)) false -> tracked else true -> (tracked + linkedFlag)
				}
				// Case: New hierarchy => Starts tracking its linked-state and updating containers accordingly
				if (isNewHierarchy) {
					val tracker = new TrackHierarchyListener(linkedFlag)
					linkedFlag.addListener(tracker)
					linkedFlag.addChangingStoppedListener(tracker)
					
					// Adds this new container to either the visible or the detached containers -list
					if (linkedFlag.value)
						markContainerAsVisible(container)
					else
						detachedContainersPp.update { _ + (linkedFlag -> Single(container)) }
				}
				// Case: Previous hierarchy that's currently linked => Adds to visible containers
				else if (linkedFlag.value)
					markContainerAsVisible(container)
				// Case: Previous now detached hierarchy => Adds to detached containers
				else
					detachedContainersPp.update { _.updatedWith(linkedFlag) {
						case Some(otherDetached) => Some(otherDetached :+ container)
						case None => Some(Single(container))
					} }
		}
		
		result
	}
	
	private def markContainerAsVisible(container: SegmentContainer) = visibleContainersP.update { _ :+ container }
	
	private def updateContainers(containers: Seq[SegmentContainer] = visibleContainersP.value): Unit = {
		lengthCache.reset()
		if (containers.nonEmpty) {
			containers.foreach { _.prepareForUpdate() }
			containers.foreach { _.resetCachedSizeLocally() }
			containers.groupBy { _.hierarchy }
				.foreach { case (hierarchy, containers) => hierarchy.revalidate(containers) }
		}
	}
	
	
	// NESTED	---------------------------------
	
	private class TrackHierarchyListener(linkedFlag: Changing[Boolean])
		extends ChangeListener[Boolean] with ChangingStoppedListener
	{
		override def onChangeEvent(event: ChangeEvent[Boolean]): ChangeResponse = {
			// Case: Hierarchy got attached
			//       => Makes the associated containers visible and removes them from the detached list
			if (event.newValue) {
				val attachedContainers = detachedContainersPp.mutate { detached =>
					detached.get(linkedFlag) match {
						case Some(containers) => containers -> (detached - linkedFlag)
						case None => Empty -> detached
					}
				}
				if (attachedContainers.nonEmpty)
					visibleContainersP.update { _ ++ attachedContainers }
			}
			// Case: Hierarchy got detached => Removes the associated containers from the visible list
			//                                 and stores them in the detached list instead
			else {
				val detachedContainers = visibleContainersP
					.mutate { _.divideBy { _.hierarchy.linkedFlag != linkedFlag }.toTuple }
				if (detachedContainers.nonEmpty)
					detachedContainersPp.update { _.updatedWith(linkedFlag) {
						case Some(previouslyDetached) => Some(previouslyDetached ++ detachedContainers)
						case None => Some(detachedContainers)
					} }
			}
			Continue
		}
		
		// If the hierarchy becomes permanently attached or detached,
		// won't need to track the detached components anymore (since they can never become visible anymore)
		override def onChangingStopped(): Unit = {
			detachedContainersPp.update { _ - linkedFlag }
			trackedLinkFlagsP.update { _ - linkedFlag }
		}
	}
	
	private class SegmentContainer(override val hierarchy: ComponentHierarchy,
	                               val wrappedComponent: ReachComponent, index: Int)
		extends ConcreteReachComponent
	{
		// ATTRIBUTES	-------------------------
		
		private val updatingFlag = Volatile.switch
		
		override val children = Single(wrappedComponent)
		override val transparent = true
		
		
		// IMPLEMENTED	-------------------------
		
		override def calculatedStackSize = wrappedComponent.stackSize.withSide(lengthCache.value, alignAxis)
		
		override def toString = s"Segment(${ index + 1})"
		
		override def resetCachedSize() = {
			super.resetCachedSize()
			// If needs to reset cached size while not in update mode, revalidates all segment containers and not
			// just this one
			if (hierarchy.isLinked && updatingFlag.isNotSet)
				updateContainers()
		}
		override def updateLayout() = {
			// Ends possible update process in updateLayout()
			updatingFlag.reset()
			// Sets component position and size
			setContentBounds()
		}
		
		override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) = ()
		
		
		// OTHER	----------------------------------
		
		def prepareForUpdate() = updatingFlag.set()
		
		def resetCachedSizeLocally() = super.resetCachedSize()
		
		private def setContentBounds() = {
			val mySize = size
			val myLength = size(alignAxis)
			val contentLength = wrappedComponent.stackSize(alignAxis)
			// May reposition content if it would be scaled above optimal length
			if (layout != Fit && contentLength.optimal < myLength) {
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

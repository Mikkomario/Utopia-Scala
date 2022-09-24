package utopia.reflection.container.stack.template.layout

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.CollectionExtensions._
import utopia.reflection.component.template.ComponentLike
import utopia.reflection.component.template.layout.stack.{Stackable, StackableWrapper}
import utopia.reflection.container.template.{ContainerLike, MappingContainer, MultiContainer}

import scala.math.Ordering.Double.TotalOrdering

/**
 * A common trait for containers that place their contents inside separate "collections"
  * (Eg. rows & columns or boxes inside rows etc.)
 * @author Mikko Hilpinen
 * @since 21.4.2020, v1.2
  * @tparam C Type of component managed by this container
  * @tparam Collection Type of collections that combine multiple components
  * @tparam Container Type of container that holds the collections
 */
trait CollectionViewLike[C <: ComponentLike, Collection <: MultiContainer[C], Container <: MultiContainer[Collection] with Stackable]
	extends MappingContainer[C, Collection] with MultiContainer[C] with StackableWrapper
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return The container wrapped by this view
	  */
	protected def container: Container
	
	/**
	  * @return Maximum capacity (of arbitrary unit) of each collection in this view (by default)
	  */
	protected def collectionMaxCapacity: Double
	
	/**
	  * @param component A component
	  * @return The space that is/would be taken by this component in a collection (in same unit as collectionMaxCapacity)
	  */
	protected def spaceOf(component: C): Double
	
	/**
	  * @return Space that is required between components per component (in same unit as collectionMaxCapacity)
	  */
	protected def betweenComponentsSpace: Double
	
	/**
	  * @return A new empty collection
	  */
	protected def newCollection(): Collection
	
	
	// COMPUTED	---------------------------
	
	private def collections = container.components
	
	
	// IMPLEMENTED	-----------------------
	
	override def updateLayout() =
	{
		// Goes through the collections in order and makes sure that...
		// a) Each one contains as many components as possible, and that...
		// b) Each of them is filled up to or below the maximum
		val maxCapacity = components.map(spaceOf).maxOption match {
			case Some(largestComponentSpace) => largestComponentSpace max collectionMaxCapacity
			case None => collectionMaxCapacity
		}
		collections.paired.foreach { case Pair(coll, nextColl) =>
			// Also, empty collections are removed
			if (coll.isEmpty)
				container -= coll
			else
			{
				// Takes items from the next collection as long as there is space
				var usedCapacity = capacityUsedIn(coll)
				var canAdd = usedCapacity < maxCapacity
				while (canAdd) {
					nextColl.components.headOption match {
						case Some(nextComponent) =>
							val nextComponentSpaceRequirement = spaceOf(nextComponent) + betweenComponentsSpace
							if (usedCapacity + nextComponentSpaceRequirement < maxCapacity) {
								usedCapacity += nextComponentSpaceRequirement
								nextColl -= nextComponent
								coll += nextComponent
							}
							else
								canAdd = false
						case None => canAdd = false
					}
				}
				// Conversely, pushes items from this collection as long as it's too big
				while (usedCapacity > maxCapacity && coll.count > 1) {
					coll.components.lastOption.foreach { pushedComponent =>
						coll -= pushedComponent
						nextColl.insert(pushedComponent, 0)
						usedCapacity -= spaceOf(pushedComponent) + betweenComponentsSpace
					}
				}
			}
		}
		
		// The last collection may be kept, removed or split into 2 or more collections
		collections.lastOption.foreach { handleLastCollection(_, maxCapacity) }
		
		super.updateLayout()
	}
	
	override protected def componentsOf(wrapper: Collection) = wrapper.components
	
	override protected def numberOfComponentsIn(wrapper: Collection) = wrapper.count
	
	override protected def insertToMiddle(component: C, wrapper: Collection, index: Int) =
	{
		wrapper.insert(component, index)
		// Layout update may be required after insert
		if (capacityUsedIn(wrapper) > collectionMaxCapacity)
			updateLayout()
	}
	
	override protected def addBetween(component: C, firstWrapper: Collection, nextWrapper: Option[Collection]) =
	{
		// Checks whether the component fits to the first stack
		if (capacityUsedIn(firstWrapper) + spaceOf(component) + betweenComponentsSpace <= collectionMaxCapacity)
			firstWrapper += component
		else
		{
			// May move it to the second collection if available and if there's space
			nextWrapper match
			{
				case Some(nextCollection) =>
					nextCollection.insert(component, 0)
					// May require layout update afterwards
					if (capacityUsedIn(nextCollection) > collectionMaxCapacity)
						updateLayout()
				case None =>
					// May create the new collection too
					val newColl = newCollection()
					newColl += component
					container += newColl
			}
		}
	}
	
	override protected def insertToBeginning(component: C, firstWrapper: Option[Collection]) =
	{
		firstWrapper match
		{
			case Some(collection) =>
				collection.insert(component, 0)
				// May require a layout update afterwards
				if (capacityUsedIn(collection) > collectionMaxCapacity)
					updateLayout()
			// May create a new collection
			case None =>
				val newColl = newCollection()
				newColl += component
				container += newColl
		}
	}
	
	override protected def removeComponentFromWrapper(component: C, wrapper: Collection) = wrapper -= component
	
	override protected def wrapped = container
	
	override protected def wrappers = container.components
	
	
	// OTHER	---------------------------
	
	private def capacityUsedIn(collection: ContainerLike[C]) = collection.components.map(spaceOf).sum + (collection.components.size - 1) * betweenComponentsSpace
	
	@scala.annotation.tailrec
	private def handleLastCollection(collection: Collection, maxCapacity: Double): Unit =
	{
		// If the last row is empty, it's removed
		if (collection.isEmpty)
			container -= collection
		// Otherwise may move components to a new row
		else if (collection.count > 1)
		{
			var usedCapacity = capacityUsedIn(collection)
			if (usedCapacity > maxCapacity)
			{
				// Pushes components until this collection is within max capacity
				val newColl = newCollection()
				do
				{
					collection.components.lastOption.foreach { pushedComponent =>
						collection -= pushedComponent
						newColl.insert(pushedComponent, 0)
						usedCapacity -= spaceOf(pushedComponent) - betweenComponentsSpace
					}
				}
				while (usedCapacity > maxCapacity && collection.count > 1)
				
				// Registers the new collection and makes sure it has enough capacity
				container += newColl
				handleLastCollection(newColl, maxCapacity)
			}
		}
	}
}

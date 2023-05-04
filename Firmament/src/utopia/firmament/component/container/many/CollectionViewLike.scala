package utopia.firmament.component.container.many

import utopia.firmament.component.Component
import utopia.firmament.component.stack.{Stackable, StackableWrapper}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair

import scala.math.Ordering.Double.TotalOrdering

/**
 * A common trait for containers that place their contents inside separate "collections"
  * (Eg. rows & columns or boxes inside rows etc.)
 * @author Mikko Hilpinen
 * @since 21.4.2020, Reflection v1.2
  * @tparam C Type of component managed by this container
  * @tparam Collection Type of collections that combine multiple components
  * @tparam Container Type of container that holds the collections
 */
trait CollectionViewLike[C <: Component, Collection <: MutableMultiContainer[C, C],
	Container <: MutableMultiContainer[Collection, Collection] with Stackable]
	extends MultiContainer[C] with StackableWrapper
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
	
	override def children = super[StackableWrapper].children
	
	override def components = container.components.flatMap { _.components }
	
	override def updateLayout() =
	{
		// Goes through the collections in order and makes sure that...
		// a) Each one contains as many components as possible, and that...
		// b) Each of them is filled up to or below the maximum
		val maxCapacity = components.map(spaceOf).maxOption.map { _ max collectionMaxCapacity }
			.getOrElse(collectionMaxCapacity)
		collections.paired.foreach { case Pair(coll, nextColl) =>
			// Also, empty collections are removed
			if (coll.isEmpty)
				container -= coll
			else {
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
	
	override protected def wrapped = container
	
	
	// OTHER	---------------------------
	
	private def capacityUsedIn(collection: MultiContainer[C]) =
		collection.components.map(spaceOf).sum + (collection.components.size - 1) * betweenComponentsSpace
	
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

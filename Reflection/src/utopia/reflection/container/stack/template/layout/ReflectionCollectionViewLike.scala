package utopia.reflection.container.stack.template.layout

import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.component.template.layout.stack.{ReflectionStackable, ReflectionStackableWrapper}
import utopia.reflection.container.template.MappingContainer
import utopia.reflection.container.template.mutable.MutableMultiContainer2

/**
 * A common trait for containers that place their contents inside separate "collections"
  * (Eg. rows & columns or boxes inside rows etc.)
 * @author Mikko Hilpinen
 * @since 21.4.2020, v1.2
  * @tparam C Type of component managed by this container
  * @tparam Collection Type of collections that combine multiple components
  * @tparam Container Type of container that holds the collections
 */
trait ReflectionCollectionViewLike[C <: ReflectionComponentLike, Collection <: MutableMultiContainer2[C, C],
	Container <: MutableMultiContainer2[Collection, Collection] with ReflectionStackable]
	extends CollectionViewLike2[C, Collection, Container] with MappingContainer[C, Collection]
		with ReflectionStackableWrapper
{
	// IMPLEMENTED	-----------------------
	
	override protected def componentsOf(wrapper: Collection) = wrapper.components
	
	override protected def numberOfComponentsIn(wrapper: Collection) = wrapper.count
	
	override protected def add(components: IterableOnce[C], index: Int): Unit =
		Vector.from(components).reverseIterator.foreach { add(_, index) }
	
	override protected def remove(components: IterableOnce[C]): Unit = components.iterator.foreach(remove)
	
	override def addBack(component: C, index: Int): Unit = add(component, index)
	
	override def addBack(components: IterableOnce[C], index: Int): Unit = add(components, index)
	
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
		else {
			// May move it to the second collection if available and if there's space
			nextWrapper match {
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
		firstWrapper match {
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
	
	override protected def wrappers = Vector.from(container.components)
	
	
	// OTHER	---------------------------
	
	private def capacityUsedIn(collection: MutableMultiContainer2[C, C]) =
		collection.components.map(spaceOf).sum + (collection.components.size - 1) * betweenComponentsSpace
}

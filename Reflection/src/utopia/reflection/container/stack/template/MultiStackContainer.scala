package utopia.reflection.container.stack.template

import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.stack.StackHierarchyManager

import scala.collection.immutable.VectorBuilder

/**
  * Stack containers hold stackable items, which means that they might update their content when content changes
  * @tparam C The type of content inside this container
  * @author Mikko Hilpinen
  * @since 15.4.2019, v0.1+
  */
trait MultiStackContainer[C <: ReflectionStackable] extends MutableMultiContainer[C, C] with StackContainerLike[C]
{
	// ABSTRACT -------------------------
	
	/**
	  * Adds the specified component to this container.
	  * The component will then be added to the stack hierarchy and this container will be revalidated.
	  * @param component Component to add to this container
	  * @param index Index where the component should be added
	  */
	protected def addToContainer(component: C, index: Int): Unit
	/*
	  * Adds the specified component(s) to this container.
	  * The components will then be added to the stack hierarchy and this container will be revalidated.
	  * @param components Component to add to this container
	  * @param index     Index where the component should be added
	  */
	// protected def addManyToContainer(components: IterableOnce[C], index: Int): Unit
	/**
	  * Removes the specified component from this container.
	  * The component has already been removed from the stack hierarchy at this point, and this component will be
	  * revalidated automatically afterwards.
	  * @param component Component to remove from this container
	  */
	protected def removeFromContainer(component: C): Unit
	
	
	// IMPLEMENTED	---------------------
	
	override protected def add(component: C, index: Int): Unit = {
		addToContainer(component, index)
		component.attachToStackHierarchyUnder(this)
		revalidate()
	}
	override protected def add(components: IterableOnce[C], index: Int): Unit = {
		val items = IndexedSeq.from(components)
		if (items.nonEmpty) {
			items.reverseIterator.foreach { add(_, index) }
			items.foreach { _.attachToStackHierarchyUnder(this) }
			revalidate()
		}
	}
	
	override protected def remove(component: C): Unit = {
		component.detachFromMainStackHierarchy()
		removeFromContainer(component)
		revalidate()
	}
	override protected def remove(components: IterableOnce[C]): Unit = {
		val comps = Iterable.from(components)
		if (comps.nonEmpty) {
			comps.foreach { c =>
				c.detachFromMainStackHierarchy()
				removeFromContainer(c)
			}
			revalidate()
		}
	}
	
	// At this time, Reflection components don't support the add back -feature
	override def addBack(component: C, index: Int): Unit = add(component, index)
	override def addBack(components: IterableOnce[C], index: Int): Unit = add(components, index)
	
	
	// OTHER	-------------------------
	
	/**
	 * Adds a component to this container but doesn't revalidate this container. This container may not display the
	 * items properly before revalidated, however
	 * @param component A new component for this container
	 */
	@deprecated("Please use insertMany instead", "v2.1")
	def addWithoutRevalidating(component: C, index: Int) = {
		addToContainer(component, index)
		component.attachToStackHierarchyUnder(this)
	}
	/**
	 * Removes a component from this container but doesn't revalidate this container. This container may not display the
	 * items properly before revalidated, however
	 * @param component A component to be removed
	 */
	@deprecated("Please use --= instead", "v2.1")
	def removeWithoutRevalidating(component: C) = {
		component.detachFromMainStackHierarchy()
		removeFromContainer(component)
	}
	
	/**
	 * Replaces current components with new ones
	 * @param newComponents New components for this container
	 */
	def replace(newComponents: Iterable[C]) = {
		replaceWithoutRevalidating(newComponents)
		
		// Finally revalidates the component hierarchy
		revalidate()
	}
	
	/**
	 * Replaces current components with new ones but doesn't revalidate this container. This container may not display the
	 * items properly before revalidated, however
	 * @param newComponents New components for this container
	 */
	def replaceWithoutRevalidating(newComponents: Iterable[C]) = {
		// Removes all existing components, unregisters only those not present in new set
		val remainingComponentsBuilder = new VectorBuilder[C]
		components.foreach { c =>
			super.-=(c)
			if (newComponents.exists { _ == c })
				remainingComponentsBuilder += c
			else
				c.detachFromMainStackHierarchy()
		}
		val remainingComponents = remainingComponentsBuilder.result()
		
		// Adds the new components and registers missing links
		newComponents.zipWithIndex.foreach { case (c, i) =>
			if (!remainingComponents.contains(c))
				c.attachToStackHierarchyUnder(this)
			super.insert(c, i)
		}
	}
}

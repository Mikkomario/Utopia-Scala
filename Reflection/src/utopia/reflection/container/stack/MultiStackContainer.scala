package utopia.reflection.container.stack

import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.MultiContainer

import scala.collection.immutable.VectorBuilder

/**
  * Stack containers hold stackable items, which means that they might update their content when content changes
  * @tparam C The type of content inside this container
  * @author Mikko Hilpinen
  * @since 15.4.2019, v0.1+
  */
trait MultiStackContainer[C <: Stackable] extends MultiContainer[C] with StackContainerLike[C]
{
	// IMPLEMENTED	---------------------
	
	override def +=(component: C) =
	{
		// Adds the component, but also registers it to stack hierarchy manager
		addWithoutRevalidating(component)
		
		// Revalidates the component hierarchy
		revalidate()
	}
	
	override def -=(component: C) =
	{
		// Removes the component, but also unregisters it from stack hierarchy manager
		removeWithoutRevalidating(component)
		
		// Revalidates component hierarchy
		revalidate()
	}
	
	
	// OTHER	-------------------------
	
	/**
	 * Adds a component to this container but doesn't revalidate this container. This container may not display the
	 * items properly before revalidated, however
	 * @param component A new component for this container
	 */
	def addWithoutRevalidating(component: C) =
	{
		super.+=(component)
		component.attachToStackHierarchyUnder(this)
	}
	
	/**
	 * Removes a component from this container but doesn't revalidate this container. This container may not display the
	 * items properly before revalidated, however
	 * @param component A component to be removed
	 */
	def removeWithoutRevalidating(component: C) =
	{
		super.-=(component)
		component.detachFromMainStackHierarchy()
	}
	
	/**
	 * Replaces current components with new ones
	 * @param newComponents New components for this container
	 */
	def replace(newComponents: Traversable[C]) =
	{
		replaceWithoutRevalidating(newComponents)
		
		// Finally revalidates the component hierarchy
		revalidate()
	}
	
	/**
	 * Replaces current components with new ones but doesn't revalidate this container. This container may not display the
	 * items properly before revalidated, however
	 * @param newComponents New components for this container
	 */
	def replaceWithoutRevalidating(newComponents: Traversable[C]) =
	{
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
		newComponents.foreach { c =>
			if (!remainingComponents.contains(c))
				c.attachToStackHierarchyUnder(this)
			super.+=(c)
		}
	}
}

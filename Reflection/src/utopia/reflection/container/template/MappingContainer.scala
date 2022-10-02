package utopia.reflection.container.template

import utopia.flow.collection.CollectionExtensions._
import utopia.reflection.component.template.ComponentLike

/**
  * This container holds its tracked components inside wrappers that may contain one or more components
  * @author Mikko Hilpinen
  * @since 21.4.2020, v1.2
  * @tparam C Type of component held within this container
  * @tparam Wrap Type of component wrapper
  */
trait MappingContainer[C <: ComponentLike, Wrap] extends Container[C]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Currently used wrappers
	  */
	protected def wrappers: Vector[Wrap]
	
	/**
	  * @param wrapper A wrapper component
	  * @return Components within the specified wrapper
	  */
	protected def componentsOf(wrapper: Wrap): IterableOnce[C]
	
	/**
	  * @param wrapper A wrapper
	  * @return The number of components within the specified wrapper
	  */
	protected def numberOfComponentsIn(wrapper: Wrap): Int
	
	/**
	  * Inserts a component to the middle of a wrapper
	  * @param component Component to insert
	  * @param wrapper Wrapper to which the component is added
	  * @param index Index within the wrapper where the component is inserted
	  */
	protected def insertToMiddle(component: C, wrapper: Wrap, index: Int): Unit
	
	/**
	  * Adds a component between two wrappers
	  * @param component Component to insert
	  * @param firstWrapper The first wrapper that would hold the component as its last item
	  * @param nextWrapper The next wrapper that would hold the component as its first item. None if no such wrapper
	  *                    exists yet.
	  */
	protected def addBetween(component: C, firstWrapper: Wrap, nextWrapper: Option[Wrap]): Unit
	
	/**
	  * Inserts a component as the first component
	  * @param component Component to insert
	  * @param firstWrapper The current first wrapper (None if there are no wrappers available)
	  */
	protected def insertToBeginning(component: C, firstWrapper: Option[Wrap]): Unit
	
	/**
	  * Removes a component from the specified wrapper
	  * @param component Component to remove
	  * @param wrapper Wrapper from which the component is removed
	  */
	protected def removeComponentFromWrapper(component: C, wrapper: Wrap): Unit
	
	
	// IMPLEMENTED	-------------------
	
	override protected def add(component: C, index: Int) =
	{
		// Checks which wrapper the component should be inserted to
		val wrappers = this.wrappers
		val wrappersCount = wrappers.size
		
		// Finds the place where to insert the new component. If no wrappers have been created yet, simply creates one.
		if (wrappersCount == 0 || index <= 0)
			insertToBeginning(component, wrappers.headOption)
		else
		{
			var nextWrapperIndex = 0
			var passedIndices = 0
			var foundSpot = false
			while (!foundSpot && nextWrapperIndex < wrappersCount)
			{
				val nextWrapper = wrappers(nextWrapperIndex)
				val currentWrapperLength = numberOfComponentsIn(nextWrapper)
				// The end index of the wrapper (exclusive)
				val wrapperEndIndex = passedIndices + currentWrapperLength
				
				// Case: Inserting in the beginning or middle of the wrapper
				if (wrapperEndIndex > index)
				{
					insertToMiddle(component, nextWrapper, index - passedIndices)
					foundSpot = true
				}
				// Case: Inserting right after the wrapper
				else if (index == wrapperEndIndex)
				{
					addBetween(component, nextWrapper, wrappers.getOption(nextWrapperIndex + 1))
					foundSpot = true
				}
				// Case: Inserting later
				else
				{
					passedIndices += currentWrapperLength
					nextWrapperIndex += 1
				}
			}
			
			// If too high an index was provided, inserts to the end of the last wrapper
			if (!foundSpot)
				addBetween(component, wrappers.last, None)
		}
	}
	
	override protected def remove(component: C) =
	{
		// Finds the wrapper that holds the specified component and then removes it from it
		wrappers.find { componentsOf(_).iterator.exists { _ == component } }.foreach { removeComponentFromWrapper(component, _) }
	}
	
	override def components = wrappers.flatMap(componentsOf)
}

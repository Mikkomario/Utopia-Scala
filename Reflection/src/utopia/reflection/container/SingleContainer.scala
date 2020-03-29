package utopia.reflection.container

import utopia.reflection.component.ComponentLike

/**
  * This container contains only a single component at a time
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
trait SingleContainer[C <: ComponentLike] extends Container[C]
{
	// COMPUTED	-----------------
	
	/**
	  * @return The current content in this container
	  */
	def content = components.headOption
	
	
	// OTHER	----------------
	
	/**
	  * Changes the component inside this container
	  * @param content The new content for this container
	  */
	def set(content: C) =
	{
		if (!this.content.contains(content))
		{
			// Removes any previous content first
			components.foreach(remove)
			// Then adds the new content
			add(content)
		}
	}
}

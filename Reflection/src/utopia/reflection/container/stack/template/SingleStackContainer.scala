package utopia.reflection.container.stack.template

import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.template.SingleContainer2

/**
  * This single item container holds a stackable component
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
trait SingleStackContainer[C <: ReflectionStackable] extends SingleContainer2[C] with StackContainerLike[C]
{
	/**
	  * Specifies the contents of this container.
	  * Stack hierarchy management has already been taken care of.
	  * @param content New content to assign
	  */
	protected def _set(content: C): Unit
	
	/**
	  * Updates the contents of this container
	  * @param content New content to assign
	  */
	def set(content: C) = {
		if (this.content != content) {
			// Removes old component from stack hierarchy first (if they were attached)
			components.foreach { _.detachFromMainStackHierarchy() }
			
			_set(content)
			
			// Adds new connection to stack hierarchy
			content.attachToStackHierarchyUnder(this)
			
			// Revalidates the hierarchy
			revalidate()
		}
	}
}

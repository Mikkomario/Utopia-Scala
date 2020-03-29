package utopia.reflection.container.stack

import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.SingleContainer

/**
  * This single item container holds a stackable component
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
trait SingleStackContainer[C <: Stackable] extends SingleContainer[C] with StackContainerLike[C]
{
	override def set(content: C) =
	{
		if (!this.content.contains(content))
		{
			// Removes old component from stack hierarchy first (if they were attached)
			components.foreach { _.detachFromMainStackHierarchy() }
			
			super.set(content)
			
			// Adds new connection to stack hierarchy
			content.attachToStackHierarchyUnder(this)
			
			// Revalidates the hierarchy
			revalidate()
		}
	}
}

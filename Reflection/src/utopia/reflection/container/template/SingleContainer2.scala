package utopia.reflection.container.template

import utopia.reflection.component.template.ComponentLike2

/**
  * A common trait for containers that always contain a single component
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
trait SingleContainer2[+C <: ComponentLike2] extends ComponentLike2
{
	// ABSTRACT	---------------------
	
	/**
	  * @return The component within this container
	  */
	protected def content: C
	
	
	// IMPLEMENTED	-----------------
	
	override def children = Vector(content)
}

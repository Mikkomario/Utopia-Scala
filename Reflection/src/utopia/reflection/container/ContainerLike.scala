package utopia.reflection.container

import utopia.reflection.component.ComponentLike

/**
  * A common trait for both mutable and immutable component containers / hierarchies
  * @author Mikko Hilpinen
  * @since 13.3.2020, v1
  */
trait ContainerLike[C <: ComponentLike] extends ComponentLike
{
	// ABSTRACT    ----------------
	
	/**
	  * The current components in this container
	  */
	def components: Vector[C]
	
	
	// COMPUTED    ----------------
	
	/**
	  * The number of items in this container
	  */
	def count = components.size
	
	/**
	  * Whether this container is currently empty
	  */
	def isEmpty = components.isEmpty
	
	
	// IMPLEMENTED	----------------
	
	override def toString = s"${getClass.getSimpleName}(${ components.mkString(", ") })"
	
	override def children: Seq[ComponentLike] = components
}

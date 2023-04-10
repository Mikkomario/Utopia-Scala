package utopia.reflection.container.template

import utopia.reflection.component.template.ReflectionComponentLike

/**
  * A common trait for both mutable and immutable component containers / hierarchies
  * @author Mikko Hilpinen
  * @since 13.3.2020, v1
  */
trait ContainerLike[+C <: ReflectionComponentLike] extends ReflectionComponentLike
{
	// ABSTRACT    ----------------
	
	/**
	  * The current components in this container
	  */
	def components: Vector[C]
	
	
	// IMPLEMENTED	----------------
	
	override def toString = s"${getClass.getSimpleName}(${ components.mkString(", ") })"
}

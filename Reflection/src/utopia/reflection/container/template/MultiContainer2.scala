package utopia.reflection.container.template

import utopia.reflection.component.template.ComponentLike2

/**
  * A common trait for both mutable and immutable component containers / hierarchies
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
trait MultiContainer2[+C <: ComponentLike2] extends ComponentLike2
{
	// ABSTRACT	-------------------
	
	/**
	  * @return Components within this container
	  */
	def components: Seq[C]
	
	
	// COMPUTED    ----------------
	
	/**
	  * The number of items in this container
	  */
	def count = components.size
	
	/**
	  * Whether this container is currently empty
	  */
	def isEmpty = components.isEmpty
	
	/**
	  * @return Whether this container holds some components
	  */
	def nonEmpty = !isEmpty
	
	
	// IMPLEMENTED	----------------
	
	override def children: Seq[ComponentLike2] = components
	
	override def toString = s"${getClass.getSimpleName}(${ components.mkString(", ") })"
}

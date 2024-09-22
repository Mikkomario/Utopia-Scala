package utopia.reflection.container.template

import utopia.flow.collection.CollectionExtensions._
import utopia.reflection.component.template.ReflectionComponentLike

/**
  * This container holds its managed components within wrappers that hold exactly one component each.
  * @author Mikko Hilpinen
  * @since 20.4.2020, v1.2
  */
trait WrappingContainer[C <: ReflectionComponentLike, Wrap] extends Container[C]
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return Wrappers managed by this container
	  */
	protected def wrappers: Seq[Wrap]
	
	/**
	  * @param wrapper A wrapper
	  * @return Component inside the wrapper
	  */
	protected def unwrap(wrapper: Wrap): C
	
	/**
	  * @param wrapper Wrapper to remove
	  * @param index The index where the wrapper is being removed
	  */
	protected def removeWrapper(wrapper: Wrap, index: Int): Unit
	
	
	// IMPLEMENTED	----------------------------
	
	override def components = wrappers.map(unwrap)
	
	override protected def remove(component: C) = {
		val wrappers = this.wrappers
		wrappers.findIndexWhere { unwrap(_) == component }.foreach { index => removeWrapper(wrappers(index), index) }
	}
}

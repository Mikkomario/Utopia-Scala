package utopia.reflection.container.template.mutable

import utopia.reflection.component.template.ComponentLike2

/**
  * A Reflection implementation of the mutable multi-container trait.
  * Makes no difference between the added and the held items.
  * @author Mikko Hilpinen
  * @since 10.4.2023, v2.0
  */
trait ReflectionMultiContainer[C <: ComponentLike2] extends MutableMultiContainer2[C, C]
{
	override protected def add(components: IterableOnce[C], index: Int): Unit =
		Vector.from(components).reverseIterator.foreach { add(_, index) }
	
	override protected def remove(components: IterableOnce[C]): Unit = components.iterator.foreach(remove)
	
	override def addBack(component: C, index: Int): Unit = add(component, index)
	
	override def addBack(components: IterableOnce[C], index: Int): Unit = add(components, index)
}

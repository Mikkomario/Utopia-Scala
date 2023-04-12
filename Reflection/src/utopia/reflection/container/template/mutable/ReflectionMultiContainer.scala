package utopia.reflection.container.template.mutable

import utopia.firmament.component.Component
import utopia.firmament.component.container.many.MutableMultiContainer

/**
  * A Reflection implementation of the mutable multi-container trait.
  * Makes no difference between the added and the held items.
  * @author Mikko Hilpinen
  * @since 10.4.2023, v2.0
  */
trait ReflectionMultiContainer[C <: Component] extends MutableMultiContainer[C, C]
{
	override protected def add(components: IterableOnce[C], index: Int): Unit =
		Vector.from(components).reverseIterator.foreach { add(_, index) }
	
	override protected def remove(components: IterableOnce[C]): Unit = components.iterator.foreach(remove)
	
	override def addBack(component: C, index: Int): Unit = add(component, index)
	
	override def addBack(components: IterableOnce[C], index: Int): Unit = add(components, index)
}

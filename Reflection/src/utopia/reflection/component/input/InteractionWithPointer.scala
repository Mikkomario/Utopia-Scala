package utopia.reflection.component.input

import utopia.flow.datastructure.mutable.PointerWithEvents

/**
  * This interaction uses a pointer to store its current value
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait InteractionWithPointer[A] extends Interaction[A] with InputWithPointer[A, PointerWithEvents[A]]
{
	override def value_=(newValue: A) = valuePointer.set(newValue)
}
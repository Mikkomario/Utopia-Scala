package utopia.reflection.component.template.input

import utopia.flow.view.mutable.eventful.PointerWithEvents

/**
  * This interaction uses a pointer to store its current value
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait InteractionWithPointer[A] extends Interaction[A] with InputWithPointer[A, PointerWithEvents[A]]
{
	override def value_=(newValue: A) = valuePointer.value = newValue
}
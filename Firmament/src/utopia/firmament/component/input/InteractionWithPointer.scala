package utopia.firmament.component.input

import utopia.flow.view.mutable.eventful.EventfulPointer

/**
  * This interaction uses a pointer to store its current value
  * @author Mikko Hilpinen
  * @since 29.6.2019, Reflection v1+
  */
// TODO: Add a new trait specifically for Flag-based interactions
// TODO: After v1.6.2 has been released, replace Interaction extension with Pointer
trait InteractionWithPointer[A] extends Interaction[A] with InputWithPointer[A, EventfulPointer[A]]
{
	override def value_=(newValue: A) = valuePointer.value = newValue
}
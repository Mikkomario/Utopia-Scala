package utopia.flow.view.mutable

/**
  * @author Mikko Hilpinen
  * @since 26.7.2023, v2.2
  */
package object eventful
{
	@deprecated("Please use EventfulPointer instead", "v2.2")
	type PointerWithEvents[A] = EventfulPointer[A]
	
	@deprecated("Please use AssignableOnce instead", "v2.6")
	type SettableOnce[A] = AssignableOnce[A]
}

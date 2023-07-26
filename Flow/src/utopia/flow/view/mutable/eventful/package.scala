package utopia.flow.view.mutable

/**
  * @author Mikko Hilpinen
  * @since 26.7.2023, v2.2
  */
package object eventful
{
	@deprecated("Please convert to using EventfulPointer instead", "v2.2")
	type PointerWithEvents[A] = EventfulPointer[A]
}

package utopia.firmament.component.display

import utopia.flow.view.mutable.eventful.PointerWithEvents

/**
  * This refreshable implementation uses pointers for storing its contents
  * @author Mikko Hilpinen
  * @since 29.6.2019, Reflection v1+
  */
trait RefreshableWithPointer[A] extends PoolWithPointer[A, PointerWithEvents[A]] with Refreshable[A]
{
	// IMPLEMENTED	--------------
	
	override def content_=(newContent: A) = contentPointer.value = newContent
}

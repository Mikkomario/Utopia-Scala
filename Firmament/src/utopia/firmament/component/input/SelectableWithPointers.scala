package utopia.firmament.component.input

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.flow.view.mutable.eventful.EventfulPointer

/**
  * This selectable implementation uses pointers
  * @author Mikko Hilpinen
  * @since 29.6.2019, Reflection v1+
  */
trait SelectableWithPointers[S, C] extends Selectable[S, C]
	with SelectionWithPointers[S, EventfulPointer[S], C, EventfulPointer[C]] with InteractionWithPointer[S]
	with RefreshableWithPointer[C]

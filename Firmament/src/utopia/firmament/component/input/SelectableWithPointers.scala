package utopia.firmament.component.input

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.flow.view.mutable.eventful.PointerWithEvents

/**
  * This selectable implementation uses pointers
  * @author Mikko Hilpinen
  * @since 29.6.2019, Reflection v1+
  */
trait SelectableWithPointers[S, C] extends Selectable[S, C]
	with SelectionWithPointers[S, PointerWithEvents[S], C, PointerWithEvents[C]] with InteractionWithPointer[S]
	with RefreshableWithPointer[C]

package utopia.reflection.component.template.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.component.template.display.RefreshableWithPointer

/**
  * This selectable implementation uses pointers
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait SelectableWithPointers[S, C] extends Selectable[S, C]
	with SelectionWithPointers[S, PointerWithEvents[S], C, PointerWithEvents[C]] with InteractionWithPointer[S]
	with RefreshableWithPointer[C]

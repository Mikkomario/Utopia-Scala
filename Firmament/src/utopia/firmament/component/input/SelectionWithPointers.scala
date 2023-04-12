package utopia.firmament.component.input

import utopia.firmament.component.display.PoolWithPointer
import utopia.flow.view.template.eventful.Changing

/**
  * This selection implementation uses pointers
  * @author Mikko Hilpinen
  * @since 29.6.2019, Reflection v1+
  * @tparam S The type of selected value
  * @tparam PS The pointer type to selected value
  * @tparam C The type of selection pool
  * @tparam PC The pointer type to selection pool
  */
trait SelectionWithPointers[+S, +PS <: Changing[S], +C, +PC <: Changing[C]] extends Selection[S, C]
	with InputWithPointer[S, PS] with PoolWithPointer[C, PC]

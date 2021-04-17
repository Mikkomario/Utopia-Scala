package utopia.reflection.component.template.input

import utopia.flow.event.ChangingLike
import utopia.reflection.component.template.display.PoolWithPointer

/**
  * This selection implementation uses pointers
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  * @tparam S The type of selected value
  * @tparam PS The pointer type to selected value
  * @tparam C The type of selection pool
  * @tparam PC The pointer type to selection pool
  */
trait SelectionWithPointers[+S, +PS <: ChangingLike[S], +C, +PC <: ChangingLike[C]] extends Selection[S, C]
	with InputWithPointer[S, PS] with PoolWithPointer[C, PC]

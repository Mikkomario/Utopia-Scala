package utopia.firmament.component.input

import utopia.flow.view.immutable.View

/**
  * Inputs can be used for reading user input
 *
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
@deprecated("Deprecated for removal. Replaced by utopia.flow.view.immutable.View", "v1.6")
trait Input[+A] extends View[A]

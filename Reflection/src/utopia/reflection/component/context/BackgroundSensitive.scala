package utopia.reflection.component.context

import utopia.reflection.color.ComponentColor

/**
  * A common trait for background sensitive concrete context implementations. Background sensitivity means that
  * the context behaves differently in components with different background colors.
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
@deprecated("Deprecated for removal", "v2.0")
trait BackgroundSensitive[+Repr]
{
	/**
	  * @param color New background color
	  * @return A copy of this instance in the specified background color context
	  */
	def inContextWithBackground(color: ComponentColor): Repr
}

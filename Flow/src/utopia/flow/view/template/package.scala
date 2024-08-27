package utopia.flow.view

import utopia.flow.view.template.eventful.Flag

/**
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
package object template
{
	/**
	  * Type alias for Flag for backwards-compatibility (renamed in v2.5)
	  */
	@deprecated("Renamed to Flag", "v2.5")
	type FlagLike = Flag
}

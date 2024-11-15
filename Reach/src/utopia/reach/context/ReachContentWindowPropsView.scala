package utopia.reach.context

import utopia.firmament.context.text.TextContextPropsView

/**
  * Common trait for context instances that provide read access to context properties.
  * Doesn't limit the implementation to either a static or a variable approach.
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.5
  */
trait ReachContentWindowPropsView extends ReachWindowContextPropsView with TextContextPropsView

package utopia.scribe.core.controller.logging

/**
  * Common trait for ScribeLike implementations where 'Repr' is hidden.
  * Useful for situations where one simply needs some ScribeLike implementation.
  * @author Mikko Hilpinen
  * @since 29.6.2023, v1.0
  */
trait Scribe extends ScribeLike[Scribe]

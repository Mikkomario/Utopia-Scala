package utopia.logos.database.access.many.text.statement

import utopia.logos.database.access.many.text.placement.ManyPlacedTextAccessLike

/**
  * Common trait for access point which return multiple placed statements
  * (i.e. statements with placement links) at a time
  * @tparam A Type of pulled statements
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 28.08.2024, v0.3
  */
trait ManyPlacedStatementsAccessLike[+A, +Repr]
	extends ManyStatementsAccessLike[A, Repr] with ManyPlacedTextAccessLike[A, Repr]
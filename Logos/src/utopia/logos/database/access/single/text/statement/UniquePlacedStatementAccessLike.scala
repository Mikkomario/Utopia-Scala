package utopia.logos.database.access.single.text.statement

import utopia.logos.database.access.single.text.placement.UniquePlacedTextAccessLike

/**
  * Common trait for access points which yield individual statements attached to individual placement links
  * @tparam A Type of pulled statements
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 28.08.2024, v0.3
  */
trait UniquePlacedStatementAccessLike[+A, +Repr]
	extends UniqueStatementAccessLike[A, Repr] with UniquePlacedTextAccessLike[A, Repr]

package utopia.vault.nosql.view

import utopia.vault.nosql.template.IntIndexFilterable

/**
 * Common trait for views that use an integer-based index for in & excluding conditions
 * @author Mikko Hilpinen
 * @since 29.10.2025, v2.0
 */
@deprecated("Replaced with IntIndexFilterable", "v2.1")
trait IntIndexFilterableView[+Repr] extends IntIndexFilterable[Repr] with FilterableView[Repr]

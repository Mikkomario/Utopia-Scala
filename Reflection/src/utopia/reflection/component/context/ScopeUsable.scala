package utopia.reflection.component.context

import utopia.flow.operator.ScopeUsable

/**
  * A common trait for context items that are usable in a specific scope
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
@deprecated("This class was copied to Flow and should be used from there instead", "v2.0")
trait ScopeUsable[+Repr] extends ScopeUsable[Repr]
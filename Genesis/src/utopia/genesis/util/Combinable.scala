package utopia.genesis.util

/**
  * These items can be combined with each other
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1
  */
@deprecated("Please use Combinable from Flow instead", "v2.6")
trait Combinable[-N, +Repr] extends utopia.flow.operator.Combinable[Repr, N]
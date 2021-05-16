package utopia.flow.event

/**
  * A common trait for listeners that are interested in value generation and reset events
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ResettableLazyListener[-A] extends LazyListener[A] with LazyResetListener[A]

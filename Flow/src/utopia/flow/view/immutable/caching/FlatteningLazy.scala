package utopia.flow.view.immutable.caching

/**
  * A lazy container that wraps a lazily acquired lazy container
  * @author Mikko Hilpinen
  * @since 29.07.2025, v2.7
  */
class FlatteningLazy[+A](wrapped: Lazy[Lazy[A]]) extends Lazy[A]
{
	// IMPLEMENTED  ---------------------
	
	override def current: Option[A] = wrapped.current.flatMap { _.current }
	override def value: A = wrapped.value.value
}

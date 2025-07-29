package utopia.flow.view.immutable.caching

/**
  * Common trait for classes that implement [[Lazy]] by wrapping one
  * @author Mikko Hilpinen
  * @since 29.07.2025, v2.7
  */
trait LazyWrapper[+A] extends Lazy[A]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The wrapped [[Lazy]] instance
	  */
	protected def wrapped: Lazy[A]
	
	
	// IMPLEMENTED  ----------------------
	
	override def current: Option[A] = wrapped.current
	override def value: A = wrapped.value
}

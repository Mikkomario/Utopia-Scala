package utopia.firmament.localization

object LocalStringFactoryWrapper
{
	// OTHER    -----------------------
	
	/**
	  * @param original Another local string factory
	  * @param f A wrapper function applied to factory's results
	  * @tparam A Type of originally constructed items
	  * @tparam B Type of mapping results
	  * @return A factory that wraps the specified factory and applies the specified mapping function
	  */
	def apply[A, B](original: LocalStringFactory[A])(f: A => B): LocalStringFactory[B] =
		new _LocalStringFactoryWrapper[A, B](original)(f)
	
	
	// NESTED   -----------------------
	
	private class _LocalStringFactoryWrapper[A, +B](override val wrapped: LocalStringFactory[A])(f: A => B)
		extends LocalStringFactoryWrapper[A, B]
	{
		override protected def wrap(string: A): B = f(string)
	}
}

/**
  * Common trait for classes that implement [[LocalStringFactory]] by wrapping one.
  * @author Mikko Hilpinen
  * @since 11.05.2025, v1.5
  */
trait LocalStringFactoryWrapper[A, +B] extends LocalStringFactory[B]
{
	// ABSTRACT ------------------------
	
	protected def wrapped: LocalStringFactory[A]
	
	protected def wrap(string: A): B
	
	
	// IMPLEMENTED  --------------------
	
	override def apply(string: String) = wrap(wrapped(string))
	override def apply(string: String, language: Language): B = wrap(wrapped(string, language))
	
	override def from(string: LocalString): B = wrap(wrapped.from(string))
	
	override def interpolate(string: String, params: Seq[Any]) = wrap(wrapped.interpolate(string, params))
	override def interpolate(string: String, params: Map[String, Any]) = wrap(wrapped.interpolate(string, params))
}

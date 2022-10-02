package utopia.flow.view.immutable.caching

/**
  * A value wrapper that conforms to the LazyLike trait. Useful when in some cases, you simply want to wrap a
  * pre-generated value which in other contexts would be lazily acquired.
  * @author Mikko Hilpinen
  * @since 12.11.2020, v1.9
  */
case class PreInitializedLazy[+A](value: A) extends Lazy[A]
{
	override def current = Some(value)
}

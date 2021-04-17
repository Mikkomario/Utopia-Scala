package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.LazyLike

/**
  * A value wrapper that conforms to the LazyLike trait. Useful when in some cases, you simply want to wrap a
  * pre-generated value which in other contexts would be lazily acquired.
  * @author Mikko Hilpinen
  * @since 12.11.2020, v1.9
  */
case class LazyWrapper[+A](value: A) extends LazyLike[A]
{
	override def current = Some(value)
}

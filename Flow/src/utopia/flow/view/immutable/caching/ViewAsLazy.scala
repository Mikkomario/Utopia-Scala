package utopia.flow.view.immutable.caching

import utopia.flow.view.immutable.View

object ViewAsLazy
{
	def wrap[A](view: View[A]) = view match {
		case l: Lazy[A] => l
		case v => new ViewAsLazy[A](v)
	}
	
	def apply[A](f: => A) = new ViewAsLazy[A](View(f))
}

/**
 * A lazy implementation that wraps a view and doesn't cache the generated item.
 * @author Mikko Hilpinen
 * @since 03.03.2026, v2.8
 */
class ViewAsLazy[+A](view: View[A]) extends Lazy[A]
{
	// ATTRIBUTES   --------------------
	
	override val isInitialized = false
	override val current: Option[A] = None
	
	
	// IMPLEMENTED  --------------------
	
	override def value: A = view.value
	
	override def map[B](f: A => B) = ViewAsLazy.wrap(view.mapValue(f))
	override def lightMap[B](f: A => B) = view match {
		case l: Lazy[A] => l.lightMap(f)
		case _ => map(f)
	}
}

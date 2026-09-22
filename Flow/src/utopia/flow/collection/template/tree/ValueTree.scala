package utopia.flow.collection.template.tree

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template.PathNavigator
import utopia.flow.operator.equality.EqualsFunction

object ValueTree
{
	// OTHER    ----------------------
	
	/**
	 * @param value Value to wrap
	 * @param children Child nodes to include
	 * @tparam A Type of the wrapped value
	 * @return A new tree wrapping the specified value
	 */
	def apply[A](value: A, children: Seq[ValueTree[A]] = Empty): ValueTree[A] = new _ValueTree[A](value, children)
	
	
	// NESTED   ----------------------
	
	private class _ValueTree[+A](override val value: A, override val children: Seq[ValueTree[A]]) extends ValueTree[A]
	{
		override def self: ValueTree[A] = this
		
		override def navigateUsing[N >: A](equals: EqualsFunction[N]): PathNavigator[N, ValueTree[N]] =
			NavigateUsingValues.from[N, ValueTree[N]](this).apply { ValueTree(_) }(equals)
	}
}

/**
 * Common trait for trees that wrap a value. Removes the generic Repr type from [[ValueTreeLike]].
 * @author Mikko Hilpinen
 * @since 05.06.2026, v2.9
 */
trait ValueTree[+A] extends Tree with ValueTreeLike[A, ValueTree, ValueTree[A]]
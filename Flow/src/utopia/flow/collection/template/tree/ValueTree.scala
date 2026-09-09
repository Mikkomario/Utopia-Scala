package utopia.flow.collection.template.tree

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template.tree.ValueTree.NavigateUsingValues
import utopia.flow.operator.equality.EqualsFunction

import scala.language.implicitConversions

object ValueTree
{
	// IMPLICIT ----------------------
	
	/**
	 * Implicitly accesses a value tree as a navigator
	 * @param tree A tree to navigate
	 * @param eq Implicit equals function to apply
	 * @tparam A Type of the navigation elements / wrapped values
	 * @return A navigator for the specified tree
	 */
	implicit def asNavigator[A](tree: ValueTree[A])
	                           (implicit eq: EqualsFunction[A] = EqualsFunction.default): TreeNavigator[A, ValueTree[A]] =
		tree.navigateUsing(eq)
	
	
	// OTHER    ----------------------
	
	/**
	 * @param value Value to wrap
	 * @param children Child nodes to include
	 * @tparam A Type of the wrapped value
	 * @return A new tree wrapping the specified value
	 */
	def apply[A](value: A, children: Seq[ValueTree[A]] = Empty): ValueTree[A] = new _ValueTree[A](value, children)
	
	
	// NESTED   ----------------------
	
	private class NavigateUsingValues[A](wrapped: ValueTree[A])(implicit eq: EqualsFunction[A])
		extends TreeNavigator[A, ValueTree[A]]
	{
		override protected def current: ValueTree[A] = wrapped
		
		override protected def findUnder(parent: ValueTree[A], nav: A): Option[ValueTree[A]] =
			parent.children.find { node => eq(node.value, nav) }
		
		override protected def nodeFor(nav: A): ValueTree[A] = ValueTree(nav)
	}
	
	private class _ValueTree[+A](override val value: A, override val children: Seq[ValueTree[A]]) extends ValueTree[A]
	{
		override def self: ValueTree[A] = this
	}
}

/**
 * Common trait for trees that wrap a value. Removes the generic Repr type from [[ValueTreeLike]].
 * @author Mikko Hilpinen
 * @since 05.06.2026, v2.9
 */
trait ValueTree[+A] extends ValueTreeLike[A, ValueTree[A]]
{
	// TODO: We must move these to ValueTreeLike, so that we may yield Repr instead of ValueTree
	/**
	 * Creates an interface for navigating this tree, based on the wrapped values
	 * @param eq Implicit equality function to apply. Default = `==`.
	 * @tparam N Type of the compared items
	 * @return A new navigator interface
	 */
	def navigate[N >: A](implicit eq: EqualsFunction[N] = EqualsFunction.default) =
		navigateUsing[N](eq)
	/**
	 * Creates an interface for navigating this tree, based on the wrapped values
	 * @param equals Equality function to apply
	 * @tparam N Type of the compared items
	 * @return A new navigator interface
	 */
	def navigateUsing[N >: A](equals: EqualsFunction[N]): TreeNavigator[N, ValueTree[N]] =
		new NavigateUsingValues[N](this)(equals)
}
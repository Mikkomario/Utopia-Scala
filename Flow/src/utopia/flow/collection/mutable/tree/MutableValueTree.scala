package utopia.flow.collection.mutable.tree

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template
import utopia.flow.collection.template.tree.{NavigateUsingValues, TreeNavigator, ValueTree, ValueTreeLike}
import utopia.flow.operator.equality.EqualsFunction

object MutableValueTree
{
	/**
	 * @param value Value to wrap by the root node
	 * @param children Child nodes to place under the root node
	 * @tparam A Type of the values wrapped by this tree
	 * @return A new tree
	 */
	def apply[A](value: A, children: Seq[template.tree.ValueTree[A]] = Empty): MutableValueTree[A] =
		new MutableValueTree(value, children.map(from))
	
	/**
	 * @param tree A tree
	 * @tparam A Type of the values in the specified tree
	 * @return If 'tree' is already of the correct type, yields it;
	 *         Otherwise yields a new tree wrapping that tree's current state.
	 */
	def from[A](tree: template.tree.ValueTree[A]): MutableValueTree[A] = tree match {
		case v: MutableValueTree[A] => v
		case v => apply(v.value, v.children)
	}
}

/**
 * A mutable tree implementation where each node wraps a value
 * @tparam A Type of the wrapped values
 * @author Mikko Hilpinen
 * @since 08.09.2026, v2.9
 */
class MutableValueTree[A](override val value: A, initialChildren: Seq[MutableValueTree[A]] = Empty)
	extends ValueTree[A] with ValueTreeLike[A, MutableValueTree, MutableValueTree[A]]
		with MutableTreeLike2[template.tree.ValueTree[A], MutableValueTree[A]]
{
	// ATTRIBUTES  --------------------------
	
	private var _children = initialChildren
	
	
	// IMPLEMENTED  -------------------------
	
	override def self: MutableValueTree[A] = this
	override def children: Seq[MutableValueTree[A]] = _children
	
	override def +=(child: ValueTree[A]): Unit = _children :+= MutableValueTree.from(child)
	override def ++=(children: IterableOnce[ValueTree[A]]): Unit =
		_children ++= children.iterator.map(MutableValueTree.from)
	
	override def filterDirect(f: MutableValueTree[A] => Boolean): Unit = _children = _children.filter(f)
	override def filter(f: MutableValueTree[A] => Boolean): Unit = {
		filterDirect(f)
		_children.foreach { _.filter(f) }
	}
	
	override def clear(): Unit = _children = Empty
	
	override def navigateUsing[N >: A](equals: EqualsFunction[N]): TreeNavigator[N, MutableValueTree[N]] =
		NavigateUsingValues.from[N, MutableValueTree[N]](MutableValueTree.from[N](this)) { nav: N => MutableValueTree(nav) }
}

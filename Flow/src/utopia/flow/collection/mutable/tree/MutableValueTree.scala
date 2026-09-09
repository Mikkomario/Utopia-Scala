package utopia.flow.collection.mutable.tree

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template.tree.{ValueTree, ValueTreeLike}
import utopia.flow.collection.template

object MutableValueTree
{
	def apply[A](value: A, children: Seq[template.tree.ValueTree[A]] = Empty): MutableValueTree[A] =
		new MutableValueTree(value, children.map(from))
		
	def from[A](tree: template.tree.ValueTree[A]): ValueTree[A] = tree match {
		case v: ValueTree[A] => v
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
	extends ValueTree[A] with ValueTreeLike[A, MutableValueTree[A]]
		with MutableTreeLike2[template.tree.ValueTree[A], MutableValueTree[A]]
{
	// ATTRIBUTES  --------------------------
	
	private var _children = initialChildren
	
	
	// IMPLEMENTED  -------------------------
	
	override def self: MutableValueTree[A] = this
	override def children: Seq[MutableValueTree[A]] = _children
	
	override def +=(child: ValueTree[A]): Unit = _children :+= child
	override def ++=(children: IterableOnce[ValueTree[A]]): Unit = _children ++= children
	
	override def filterDirect(f: MutableValueTree[A] => Boolean): Unit = _children = _children.filter(f)
	override def filter(f: MutableValueTree[A] => Boolean): Unit = {
		filterDirect(f)
		_children.foreach { _.filter(f) }
	}
	
	override def clear(): Unit = _children = Empty
}

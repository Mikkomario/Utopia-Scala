package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template

object ValueTree
{
	// OTHER    -------------------------
	
	def factory[A](value: A) = new ValueTreeFactory[A](value)
	
	
	// NESTED   -------------------------
	
	class ValueTreeFactory[A](value: A) extends TreeFactory[A, ValueTree[A]]
	{
		override def withChildren(children: IterableOnce[A]): ValueTree[A] =
			apply(value, children.iterator.map { ValueTree(_) }.toOptimizedSeq)
	}
	
	class ValueTreeMutator[A](override protected val root: ValueTree[A], override protected val path: Seq[ValueTree[A]],
	                          override val node: ValueTree[A], override val generated: Boolean = false)
		extends TreeMutatorLike[A, A, ValueTree[A], ValueTreeMutator[A]]
	{
		override protected def wrapChild(child: ValueTree[A], generated: Boolean): ValueTreeMutator[A] =
			new ValueTreeMutator[A](root, path :+ node, child, generated)
		
		override protected def wrapUpdatedChild(child: ValueTree[A]): A = ???
		
		override protected def findNodeFor(nodes: Seq[ValueTree[A]], nav: A): Option[ValueTree[A]] = ???
		
		override protected def current: ValueTreeMutator[A] = ???
		
		override protected def nodeFor(nav: A): ValueTreeMutator[A] = ???
	}
}

/**
 * Common trait for immutable trees where each node wraps some kind of value
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
// TODO: We need two interfaces: One for nodes and another for values
case class ValueTree[A](override val value: A, override val children: Seq[ValueTree[A]] = Empty)
	extends template.tree.ValueTree[A] with CopyableValueTreeLike[A, A, ValueTree[A]]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val factory: TreeFactory[A, ValueTree[A]] = ValueTree.factory[A](value)
	override lazy val appendingFactory: TreeFactory[A, ValueTree[A]] =
		factory.appendingTo(children.view.map { _.value })
		
	
	// IMPLEMENTED  -------------------------
	
	override def self: ValueTree[A] = this
	
	override def slicingFactory(index: Int, replaceCount: Int): TreeFactory[A, ValueTree[A]] =
		factory.slicing(children.view.map { _.value }, index, replaceCount)
	
	override def filterDirect(f: ValueTree[A] => Boolean): ValueTree[A] = copy(children = children.filter(f))
	
	override def filter(f: ValueTree[A] => Boolean): ValueTree[A] =
		copy(children = children.view.filter(f).map { _.filter(f) }.toOptimizedSeq)
	
	/*
	/**
	 * Creates a new tree that contains a child node with specified value.
	 * If this node already contains such a child, doesn't add a new one.
	 * @param value Child node value
	 * @return A copy of this tree including a child with that value
	 */
	def including(value: A)(implicit eq: EqualsFunction[A] = EqualsFunction.default) = {
		// Case: This node already contains that child => ignores
		if (children.exists { _.value ~== value })
			self
		// Case: New child => inserts
		else
			this :+ value
	}
	
	def mutate[Nav >: A](step: N, otherSteps: N*)(implicit eq: EqualsFunction[Nav] = EqualsFunction.default) =
		mutatePath[Nav](step +: otherSteps)
	
	def mutatePath[Nav >: A](path: Seq[N])(implicit eq: EqualsFunction[Nav] = EqualsFunction.default) =
		mutatePathUsing(path, eq)
	
	def mutatePathUsing[Nav >: A](path: Seq[N], eq: EqualsFunction[Nav]) =
		TreeMutator(self, path) { (node, nav) => eq.apply(node.value, nav) } { ValueTree(_) }
	 */
}
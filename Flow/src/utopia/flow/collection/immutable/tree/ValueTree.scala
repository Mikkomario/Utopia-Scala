package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.LazySingle
import utopia.flow.collection.immutable.{Empty, SingleView}
import utopia.flow.collection.immutable.tree.ValueTree.ValueTreeMutator
import utopia.flow.collection.template
import utopia.flow.collection.template.tree
import utopia.flow.operator.equality.EqualsFunction

import scala.collection.IndexedSeqView
import scala.language.implicitConversions

object ValueTree
{
	// IMPLICIT -------------------------
	
	/**
	 * Implicitly wraps a value under a node
	 * @param value Value to wrap
	 * @tparam A Type of the value to wrap
	 * @return A root level node wrapping that value
	 */
	implicit def wrap[A](value: A): ValueTree[A] = apply(value).withoutChildren
	/**
	 * Implicitly wraps a value with n child nodes
	 * @param valueAndChildren A pair of two values:
	 *                              1. Value to wrap by the root node
	 *                              1. Child nodes to place under said node
	 * @tparam A Type of the wrapped values
	 * @return A new value tree, based on the specified input
	 */
	implicit def wrap[A](valueAndChildren: (A, IterableOnce[ValueTree[A]])): ValueTree[A] =
		apply(valueAndChildren._1).withChildren(valueAndChildren._2)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param value Value to wrap by the root node
	 * @param lazily Whether the child nodes should be initialized lazily. Default = false.
	 * @tparam A Type of the wrapped value
	 * @return A factory for constructing value trees
	 */
	def apply[A](value: A, lazily: Boolean = false) = new ValueTreeFactory[A](value)
	
	/**
	 * @param node A node to wrap
	 * @tparam A Type of the node's value
	 * @return A value tree from the specified node
	 */
	def from[A](node: template.tree.ValueTree[A]): ValueTree[A] = node match {
		case t: ValueTree[A] => t
		case t => apply(t.value).withChildren(t.children.map(ValueTree.from))
	}
	
	
	// NESTED   -------------------------
	
	/**
	 * A factory interface used for constructing new value trees
	 * @param value Value to wrap by the root node
	 * @param lazily Whether the child nodes should be initialized lazily. Default = false.
	 * @tparam A Type of the wrapped values
	 */
	case class ValueTreeFactory[A](value: A, lazily: Boolean = false)
		extends TreeFactory[template.tree.ValueTree[A], ValueTree[A]]
	{
		// IMPLEMENTED  ------------------
		
		override def withChildren(children: IterableOnce[template.tree.ValueTree[A]]): ValueTree[A] = {
			val childIter = children.iterator.map(ValueTree.from)
			ValueTree(value, if (lazily) childIter.caching else childIter.toOptimizedSeq, lazily)
		}
		
		
		// OTHER    -----------------------
		
		/**
		 * Creates a new tree with a recursive function.
		 * @param goDeeper A function that accepts a node value and returns the values of the nodes directly below.
		 *
		 *                 This function must return empty collections at some point, otherwise an infinite recursive
		 *                 loop will ensue.
		 * @return A tree generated using the specified function
		 */
		def iterate(goDeeper: A => IterableOnce[A]): ValueTree[A] = {
			val childInput = goDeeper(value).iterator.map { v => copy(value = v).iterate(goDeeper) }
			withChildren(if (lazily) childInput.caching else childInput.toOptimizedSeq)
		}
		
		/**
		 * Makes this node into a single branch
		 * @param values Values that continue this branch
		 * @return A node with children based on the specified branch values
		 */
		def branch(values: IterableOnce[A]): ValueTree[A] = _branch(values.iterator)
		
		/**
		 * Assigns a series of (potentially overlapping) branches under this node.
		 * @param branches The branches to place under this node (where each iterates from top to bottom)
		 * @param keyFrom A function that converts a value into a grouping key.
		 *                Branches which yield identical keys will be placed under the same node.
		 * @param group Maps the grouped values into a tree nav element.
		 *              Receives:
		 *              1. The grouping key
		 *              1. A view into the values to group. Contains 1-n elements.
		 * @param keyEquals An implicit equals function to use for comparing the generated keys.
		 *                  Default = use ==.
		 * @tparam K Type of the assigned keys
		 * @tparam V Type of the branch values
		 * @return This node with branches based on the specified input
		 */
		def withGroupedBranches[K, V](branches: IterableOnce[IterableOnce[V]])
		                             (keyFrom: V => K)(group: (K, IndexedSeqView[V]) => A)
		                             (implicit keyEquals: EqualsFunction[K]): ValueTree[A] =
			withChildren(groupBranches(branches.iterator.map { _.iterator })(keyFrom)(group))
			
		private def groupBranches[K, V](branchIterators: IterableOnce[Iterator[V]])
		                               (keyFrom: V => K)(group: (K, IndexedSeqView[V]) => A)
		                               (implicit keyEquals: EqualsFunction[K]): Seq[ValueTree[A]] =
		{
			// Generates the next layer of nodes
			val childrenIter = branchIterators.iterator.flatMap { branch => branch.nextOption().map { _ -> branch } }
				// Groups the generated values based on their keys
				.groupToSeqsBy { case (value, _) => keyFrom(value) }.iterator
				// Converts each key into a new node
				.map { case (key, values) =>
					val childFactory = copy(value = group(key, values.view.map { _._1 }))
					values.oneOrMany match {
						// Case: Only one branch belonged to this group => Continues downwards without grouping
						case Left((_, branch)) =>
							childFactory.branch(branch.map { v => group(keyFrom(v), SingleView(v)) })
							
						// Case: 2 or more branches were merged => Applies grouping on the lower levels, also
						case Right(branches) =>
							childFactory.withChildren(groupBranches(branches.iterator.map { _._2 })(keyFrom)(group))
					}
				}
			
			// Converts the generated iterator into the appropriate collection type
			if (lazily)
				childrenIter.caching
			else
				childrenIter.toOptimizedSeq
		}
		
		/**
		 * Generates a linear branch under this node
		 * @param valuesIter An iterator that yields the values to place under this node (sequentially)
		 * @return This node with the specified branch under it
		 */
		private def _branch(valuesIter: Iterator[A]): ValueTree[A] = valuesIter.nextOption() match {
			case Some(nextValue) =>
				if (lazily)
					withChildren(LazySingle(copy(value = nextValue)._branch(valuesIter)))
				else
					withChild(copy(value = nextValue)._branch(valuesIter))
					
			case None => withoutChildren
		}
	}
	
	/**
	 * A mutator interface for value trees
	 * @param root Modified root-level node
	 * @param path Path to the targeted node
	 * @param node Targeted node
	 * @param generated Whether the targeted node has been generated (i.e. didn't previously exist under 'root')
	 * @param eq Implicit equals function used in navigation
	 * @tparam A Type of the node values
	 */
	class ValueTreeMutator[A](override protected val root: ValueTree[A], override protected val path: Seq[ValueTree[A]],
	                          override val node: ValueTree[A], override val generated: Boolean = false)
	                         (implicit eq: EqualsFunction[A])
		extends TreeMutatorLike[A, template.tree.ValueTree[A], ValueTree[A], ValueTreeMutator[A]]
	{
		override protected def current: ValueTreeMutator[A] = this
		
		override protected def wrapChild(child: ValueTree[A], generated: Boolean): ValueTreeMutator[A] =
			new ValueTreeMutator[A](root, path :+ node, child, generated)
		
		override protected def wrapUpdatedChild(child: ValueTree[A]): tree.ValueTree[A] = child
		
		override protected def findNodeFor(nodes: Seq[ValueTree[A]], nav: A): Option[ValueTree[A]] =
			nodes.find { node => eq(node.value, nav) }
		
		override protected def nodeFor(nav: A): ValueTreeMutator[A] =
			wrapChild(ValueTree(nav).withoutChildren, generated = true)
	}
}

/**
 * Common trait for immutable trees where each node wraps some kind of value
 * @tparam A Type of the wrapped values
 * @param value The wrapped value in this node
 * @param children Child nodes directly under this node
 * @param lazily Whether copies of this tree should be initialized lazily. Default = false.
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
case class ValueTree[A](override val value: A, override val children: Seq[ValueTree[A]], lazily: Boolean)
	extends template.tree.ValueTree[A] with CopyableValueTreeLike[A, template.tree.ValueTree[A], ValueTree[A]]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val factory: TreeFactory[template.tree.ValueTree[A], ValueTree[A]] = ValueTree(value, lazily)
	override lazy val appendingFactory: TreeFactory[template.tree.ValueTree[A], ValueTree[A]] =
		factory.appendingTo(children)
		
	
	// COMPUTED -----------------------------
	
	/**
	 * @return A copy of this tree that initializes new nodes lazily
	 */
	def growingLazily = if (lazily) this else copy(lazily = true)
	
	/**
	 * @param eq Implicit equals function to apply. Used in navigation. Default = use ==.
	 * @return A mutator interface targeting this node
	 */
	def mutate(implicit eq: EqualsFunction[A] = EqualsFunction.default) = mutateUsing(eq)
		
	
	// IMPLEMENTED  -------------------------
	
	override def self: ValueTree[A] = this
	
	override def slicingFactory(index: Int, replaceCount: Int): TreeFactory[template.tree.ValueTree[A], ValueTree[A]] =
		factory.slicing(children, index, replaceCount)
	
	override def filterDirect(f: ValueTree[A] => Boolean): ValueTree[A] = copy(children = children.filter(f))
	
	override def filter(f: ValueTree[A] => Boolean): ValueTree[A] =
		copy(children = children.view.filter(f).map { _.filter(f) }.toOptimizedSeq)
		
	
	// OTHER    -----------------------------
	
	/**
	 * @param eq Equals function to apply. Used in navigation.
	 * @return A mutator interface targeting this node
	 */
	def mutateUsing(eq: EqualsFunction[A]) = new ValueTreeMutator[A](this, Empty, this)(eq)
}
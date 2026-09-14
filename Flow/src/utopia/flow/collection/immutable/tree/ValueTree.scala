package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.LazySingle
import utopia.flow.collection.immutable.{Empty, SingleView}
import utopia.flow.collection.template
import utopia.flow.collection.template.tree
import utopia.flow.collection.template.tree.{NavigateUsingValues, TreeNavigator}
import utopia.flow.operator.Identity
import utopia.flow.operator.equality.EqualsFunction

import scala.annotation.tailrec
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
	implicit def wrap[A](value: A)(implicit valueEquals: EqualsFunction[A] = EqualsFunction.default): ValueTree[A] =
		apply(value).withoutChildren
	/**
	 * Implicitly wraps a value with n child nodes
	 * @param valueAndChildren A pair of two values:
	 *                              1. Value to wrap by the root node
	 *                              1. Child nodes to place under said node
	 * @tparam A Type of the wrapped values
	 * @return A new value tree, based on the specified input
	 */
	implicit def wrapWithChildren[A](valueAndChildren: (A, IterableOnce[ValueTree[A]]))
	                                (implicit valueEquals: EqualsFunction[A] = EqualsFunction.default): ValueTree[A] =
		apply(valueAndChildren._1).withChildren(valueAndChildren._2)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param value Value to wrap by the root node
	 * @param lazily Whether the child nodes should be initialized lazily. Default = false.
	 * @tparam A Type of the wrapped value
	 * @return A factory for constructing value trees
	 */
	def apply[A](value: A, lazily: Boolean = false)(implicit valueEquals: EqualsFunction[A] = EqualsFunction.default) =
		new ValueTreeFactory[A](value)
	
	/**
	 * @param node A node to wrap
	 * @tparam A Type of the node's value
	 * @return A value tree from the specified node
	 */
	def from[A](node: template.tree.ValueTree[A])
	           (implicit valueEquals: EqualsFunction[A] = EqualsFunction.default): ValueTree[A] =
		node match {
			case t: ValueTree[A] => t
			case t => apply(t.value).withChildren(t.children.map[ValueTree[A]](from))
		}
	
	/**
	 * @param tree A tree to modify
	 * @param eq Implicit equals function used for value-based navigation
	 * @tparam A Type of the tree values
	 * @return A new mutator interface yielding modified copies of 'tree'
	 */
	def mutatorFor[A](tree: template.tree.ValueTree[A])
	                 (implicit eq: EqualsFunction[A] = EqualsFunction.default): ValueTreeMutator[A] =
	{
		val valueTree = from(tree)
		new ValueTreeMutator[A](valueTree, Empty, valueTree)
	}
	
	
	// NESTED   -------------------------
	
	/**
	 * A factory interface used for constructing new value trees
	 * @param value Value to wrap by the root node
	 * @param lazily Whether the child nodes should be initialized lazily. Default = false.
	 * @param valueEquals A function used for matching values in tree navigation
	 * @tparam A Type of the wrapped values
	 */
	case class ValueTreeFactory[A](value: A, lazily: Boolean = false)(implicit valueEquals: EqualsFunction[A])
		extends TreeFactory[template.tree.ValueTree[A], ValueTree[A]]
	{
		// IMPLEMENTED  ------------------
		
		override def withChildren(children: IterableOnce[template.tree.ValueTree[A]]): ValueTree[A] = {
			val childIter = children.iterator.map(ValueTree.from)
			_ValueTree(value, if (lazily) childIter.caching else childIter.toOptimizedSeq, lazily)
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
		 *
		 * Overlapping (leading) parts will be joined into the same nodes / branches.
		 * E.g. `[ [1, 2], [1, 3], [4] ]` would yield two trees: 1 -> [2, 3] and 4.
		 *
		 * @param branches Branches to place under this node.
		 * @return A node containing the specified branches
		 */
		def withBranches(branches: IterableOnce[IterableOnce[A]]) =
			withGroupedBranches(branches)(Identity) { (v, _) => v }
		
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
	
	/*
	 * A mutator interface for value trees
	 * @param root Modified root-level node
	 * @param path Path to the targeted node
	 * @param node Targeted node
	 * @param generated Whether the targeted node has been generated (i.e. didn't previously exist under 'root')
	 * @param eq Implicit equals function used in navigation
	 * @tparam A Type of the node values
	 */
	// NB: Contains a lot of duplicated code from TreeMutatorLike.
	// However, TreeMutatorLike doesn't use generic type parameters.
	class ValueTreeMutator[A](root: ValueTree[A], path: Seq[ValueTree[A]], private val node: ValueTree[A],
	                          generated: Boolean = false)
	                         (implicit eq: EqualsFunction[A])
		extends TreeNavigator[A, ValueTreeMutator[A]] with ValueTree[A]
	{
		// COMPUTED ---------------------------
		
		/**
		 * @return Copy of the root node including this node
		 */
		def included = {
			if (generated) {
				val pathIter = ascendingIter
				pathIter.nextOption() match {
					case Some(parent) => assign(parent, parent :+ node, pathIter)
						// Case: Including a generated root node (not expected) => Yields the new node
					case None => node
				}
			}
			else
				root
		}
		/**
		 * @return Copy of the root node with this node excluded / not present
		 */
		def excluded = {
			// Case: Attempting to exclude the root => Fails
			if (path.hasSize < 2)
				throw new UnsupportedOperationException("Can't exclude the root node")
			
			// Case: A generated node => Already excluded
			if (generated)
				root
			else {
				val pathIter = ascendingIter
				val parent = pathIter.next()
				assign(parent, parent.withoutDirect(node), pathIter)
			}
		}
		
		private def ascendingIter = path.reverseIterator.drop(1)
		
		
		// IMPLEMENTED  -----------------------
		
		override def self: ValueTree[A] = node
		override protected def current: ValueTreeMutator[A] = this
		
		override def children: Seq[ValueTree[A]] = node.children
		override def value: A = node.value
		
		override def lazily: Boolean = node.lazily
		override def growingLazily: ValueTree[A] = if (node.lazily) node else mapped { _.growingLazily }
		
		override def withoutChildren: ValueTree[A] =
			if (!generated && node.isEmpty) node else mapped { _.withoutChildren }
		
		override def withValue[B >: A](newValue: B): ValueTree[B] = mapped { _.withValue(newValue) }
		override def mapValues[B >: A](f: A => B): ValueTree[B] = mapped { _.mapValues(f) }
		
		override def ++[B >: A](newChildren: IterableOnce[tree.ValueTree[B]]): ValueTree[B] =
			mapped { _ ++ newChildren }
		override def withChildren[B >: A](newChildren: IterableOnce[tree.ValueTree[B]]): ValueTree[B] =
			mapped { _.withChildren(newChildren) }
		
		override def filterDirect(f: ValueTree[A] => Boolean): ValueTree[A] = mapped { _.filterDirect(f) }
		override def filter(f: ValueTree[A] => Boolean): ValueTree[A] = mapped { _.filter(f) }
		
		override protected def nodeFor(nav: A): ValueTreeMutator[A] =
			wrapChild(ValueTree(nav).withoutChildren, generated = true)
		
		override protected def findUnder(parent: ValueTreeMutator[A], nav: A): Option[ValueTreeMutator[A]] =
			parent.node.children.find { node => eq(node, nav) }.map { wrapChild(_) }
		
		
		// OTHER    -------------------------------
		
		/**
		 * @param f A mapping function to apply to the targeted node
		 * @tparam B Type of value-mapping results
		 * @return A copy of the root tree with this node mapped
		 */
		def mapped[B >: A](f: ValueTree[A] => ValueTree[B]): ValueTree[B] = replacedWith(f(node))
		/**
		 * Replaces the current node with a new version, yielding a modified copy of the root node.
		 * @param updated Updated version of [[node]].
		 * @return Updated version of [[root]]
		 */
		def replacedWith[B >: A](updated: ValueTree[B]) = {
			if (generated) {
				val pathIter = ascendingIter
				pathIter.nextOption() match {
					case Some(parent) => assign(parent, parent :+ updated, pathIter)
					case None => updated
				}
			}
			else
				assign(node, updated, ascendingIter)
		}
		
		/**
		 * Assigns a modified node under a parent, traversing the path up until root is modified
		 * @param original Original node version
		 * @param updated Updated node version
		 * @param pathIter An iterator that yields the parents of 'original' (ascending)
		 * @return Modified root node
		 */
		@tailrec
		private def assign[B >: A](original: ValueTree[A], updated: ValueTree[B],
		                           pathIter: Iterator[ValueTree[A]]): ValueTree[B] =
		{
			// Case: Reached root => Yields the modified version
			if (!pathIter.hasNext || original == root)
				updated
			// Case: Still going up => Replaces the updated node within the parent
			else {
				val nextOriginal = pathIter.next()
				assign(nextOriginal, nextOriginal.replaceChild(original, updated), pathIter)
			}
		}
		
		private def wrapChild(child: ValueTree[A], generated: Boolean = false): ValueTreeMutator[A] =
			new ValueTreeMutator[A](root, path :+ node, child, generated)
	}
	
	private case class _ValueTree[+A](override val value: A, override val children: Seq[ValueTree[A]], lazily: Boolean)
		extends ValueTree[A]
	{
		// IMPLEMENTED  -------------------------
		
		override def self: ValueTree[A] = this
		
		override def growingLazily = if (lazily) self else copy(lazily = true)
		
		override def withValue[B >: A](newValue: B): ValueTree[B] = copy(value = newValue)
		
		override def mapValues[B >: A](f: A => B): ValueTree[B] =
			copy(value = f(value), children = children.map { _.mapValues(f) })
		
		override def withoutChildren: ValueTree[A] = if (isEmpty) self else copy(children = Empty)
		
		override def withChildren[B >: A](newChildren: IterableOnce[template.tree.ValueTree[B]]) = {
			val childIter = newChildren.iterator.map(ValueTree.from)
			copy(children = if (lazily) childIter.caching else childIter.toOptimizedSeq)
		}
		
		override def ++[B >: A](newChildren: IterableOnce[template.tree.ValueTree[B]]) =
			copy(children = children ++ newChildren.iterator.map(ValueTree.from))
		
		override def filterDirect(f: ValueTree[A] => Boolean): ValueTree[A] = copy(children = children.filter(f))
		override def filter(f: ValueTree[A] => Boolean): ValueTree[A] = {
			val newChildrenView = children.view.filter(f).map { _.filter(f) }
			copy(children = if (lazily) newChildrenView.caching else newChildrenView.toOptimizedSeq)
		}
	}
}

/**
 * Common trait for immutable trees where each node wraps some kind of value
 * @tparam A Type of the wrapped values
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
trait ValueTree[+A]
	extends template.tree.ValueTree[A] with ValueTreeLike[A, template.tree.ValueTree, ValueTree, ValueTree[A]]
{
	// ABSTRACT -----------------------------
	
	def lazily: Boolean
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return A copy of this tree that initializes new nodes lazily
	 */
	def growingLazily: ValueTree[A]
	
	/**
	 * @return A mutator interface targeting this node
	 */
	def mutate[B >: A](implicit eq: EqualsFunction[B]) = mutateUsing[B](eq)
	
	
	// IMPLEMENTED  -------------------------
	
	override def navigateUsing[N >: A](equals: EqualsFunction[N]): TreeNavigator[N, ValueTree[N]] =
		NavigateUsingValues[N, N, ValueTree[N]](ValueTree.from(this)) {
			nav: N => ValueTree(nav).withoutChildren }(equals)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param eq Equals function to apply. Used in navigation.
	 * @return A mutator interface targeting this node
	 */
	def mutateUsing[B >: A](eq: EqualsFunction[B]) = ValueTree.mutatorFor[B](self)(eq)
}
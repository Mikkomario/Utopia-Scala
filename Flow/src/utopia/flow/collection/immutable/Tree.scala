package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.{CachingSeq, LazySingle}
import utopia.flow.operator.equality.EqualsFunction

import scala.collection.{IndexedSeqView, IterableFactory, SeqFactory}

object Tree
{
	/**
	  * Creates a new tree that consists of a single linear branch that doesn't divide at any point
	  * @param b Items to place on this branch, from the root to the leaf
	  * @param navEquals An implicit equals function to use when comparing navigational elements in this tree.
	  *                  Default is ==
	  * @tparam A Type of nav elements used by this tree
	  * @throws NoSuchElementException If the specified branch is empty
	  * @return A new tree
	  */
	@throws[NoSuchElementException]("If the specified branch is empty")
	def branch[A](b: IterableOnce[A], lazily: Boolean = false)
	             (implicit navEquals: EqualsFunction[A] = EqualsFunction.default): Tree[A] =
		_branch(b.iterator)(wrapperFunction(lazily))
	private def _branch[A](branchIterator: Iterator[A])(toSeq: (() => Tree[A]) => Seq[Tree[A]])
	                      (implicit navEquals: EqualsFunction[A]): Tree[A] =
	{
		val nav = branchIterator.next()
		if (branchIterator.hasNext)
			apply(nav, toSeq({ () => _branch(branchIterator)(toSeq) }))
		else
			apply(nav)
	}
	
	/**
	 * Converts a series of (potentially overlapping) branches to trees.
	 *
	 * Overlapping (leading) parts will be joined into the same nodes / branches.
	 * E.g. `[ [1, 2], [1, 3], [4] ]` would yield two trees: 1 -> [2, 3] and 4.
	 *
	 * @param b Branches that will form these trees.
	 * @param lazily Whether to form the branches lazily. Default = false.
	 * @param navEquals An implicit equals function to use when comparing navigational elements in this tree.
	 *                  Default is ==
	 * @tparam A Type of the branch nav elements
	 * @return Trees formed from the specified branches
	 */
	def branches[A](b: IterableOnce[IterableOnce[A]], lazily: Boolean = false)
	               (implicit navEquals: EqualsFunction[A] = EqualsFunction.default): Seq[Tree[A]] =
		_branches(b.iterator.map { _.iterator }, if (lazily) CachingSeq: SeqFactory[Seq] else OptimizedIndexedSeq)(
			wrapperFunction(lazily))
	private def _branches[A](branchIterators: IterableOnce[Iterator[A]], seqFactory: IterableFactory[Seq])
	                        (wrap: (() => Tree[A]) => Seq[Tree[A]])
	                        (implicit navEquals: EqualsFunction[A]): Seq[Tree[A]] =
		seqFactory.from(branchIterators.iterator.flatMap { iter => iter.nextOption().map { _ -> iter } }
			.groupMapToSeqs { _._1 } { _._2 }.iterator
			.map { case (nav, branchIterators) =>
				branchIterators.emptyOneOrMany match {
					case None => apply(nav)
					case Some(Left(branch)) =>
						if (branch.hasNext)
							apply(nav, wrap({ () => _branch(branch)(wrap) }))
						else
							apply(nav)
					case Some(Right(branches)) => apply(nav, _branches(branches, seqFactory)(wrap))
				}
			})
	
	/**
	 * Converts a series of (potentially overlapping) branches to trees.
	 * @param branches The branches to convert to trees (where each iterates from top to bottom)
	 * @param lazily Whether to generate the tree lazily (default = false)
	 * @param keyFrom A function which converts a value into a grouping key.
	 *                Branches which yield identical keys will be placed under the same node.
	 * @param group Maps the grouped values into a tree nav element.
	 *              Receives:
	 *                  1. The grouping key
	 *                  1. A view into the values to group. Contains 1-n elements.
	 * @param navEquals An implicit equals function to use when comparing navigational elements in this tree.
	 *                  Default is ==
	 * @tparam K Type of the grouping keys used
	 * @tparam V Type of the grouped values
	 * @tparam G Type of the map results of 'group' / resulting tree nav elements
	 * @return A collection of trees formed from the specified branches
	 */
	def groupingBranches[K, V, G](branches: IterableOnce[IterableOnce[V]], lazily: Boolean = false)
	                             (keyFrom: V => K)(group: (K, IndexedSeqView[V]) => G)
	                             (implicit navEquals: EqualsFunction[G] = EqualsFunction.default) =
	{
		val seqFactory: IterableFactory[Seq] = if (lazily) CachingSeq else OptimizedIndexedSeq
		_groupingBranches[K, V, G](branches.iterator.map { _.iterator }, seqFactory)(keyFrom)(group)(
			wrapperFunction(lazily))
	}
	private def _groupingBranches[K, V, G](branchIterators: IterableOnce[Iterator[V]], seqFactory: IterableFactory[Seq])
	                                      (keyFrom: V => K)(group: (K, IndexedSeqView[V]) => G)
	                                      (wrap: (() => Tree[G]) => Seq[Tree[G]])
	                                      (implicit navEquals: EqualsFunction[G]): Seq[Tree[G]] =
		seqFactory.from(branchIterators.iterator.flatMap { b => b.nextOption().map { _ -> b } }
			.groupToSeqsBy { case (value, _) => keyFrom(value) }.iterator
			.map { case (key, values) =>
				val nav = group(key, values.view.map { _._1 })
				val children = values.emptyOneOrMany match {
					case None => Empty
					case Some(Left((_, branch))) =>
						if (branch.hasNext)
							wrap { () =>
								_mapToBranch(branch) { v =>
									val key = keyFrom(v)
									group(key, SingleView(v))
								}(wrap)
							}
						else
							Empty
					case Some(Right(branches)) =>
						_groupingBranches(branches.iterator.map { _._2 }, seqFactory)(keyFrom)(group)(wrap)
				}
				apply(nav, children)
			})
	private def _mapToBranch[V, G](branchIterator: Iterator[V])(map: V => G)(wrap: (() => Tree[G]) => Seq[Tree[G]])
	                              (implicit navEquals: EqualsFunction[G]): Tree[G] =
	{
		val nav = map(branchIterator.next())
		if (branchIterator.hasNext)
			apply(nav, wrap({ () => _mapToBranch(branchIterator)(map)(wrap) }))
		else
			apply(nav)
	}
	
	/**
	  * Creates a new tree with a recursive function.
	  * The whole tree structure is initialized at once. For lazily initialized structures,
	  * see [[utopia.flow.collection.immutable.caching.LazyTree]]
	 * or yield a lazily initialized collection, such as CachingSeq, in 'goDeeper'
	  * @param root The root node nav-element
	  * @param goDeeper A function that accepts a nav-element and returns the nav-elements of the nodes directly
	  *                 below.
	  *
	  *                 This function must return empty collections at some point, otherwise an infinite recursive
	  *                 loop will ensue.
	  * @tparam A Type of nav-elements used by this tree structure
	  * @return A tree generated by the specified function
	  */
	def iterate[A](root: A)(goDeeper: A => Seq[A])
	              (implicit navEquals: EqualsFunction[A] = EqualsFunction.default): Tree[A] =
		apply(root, goDeeper(root).map { nav => iterate(nav)(goDeeper) })
		
	private def wrapperFunction[A](lazily: Boolean): (() => Tree[A]) => Seq[Tree[A]] =
		if (lazily) { f => LazySingle { f() } } else { f => Single(f()) }
}

/**
  * This TreeNode implementation is immutable and safe to reference from multiple places
  * @author Mikko Hilpinen
  * @since 4.11.2016
  */
case class Tree[A](override val nav: A, override val children: Seq[Tree[A]] = Empty)
                  (implicit override val navEquals: EqualsFunction[A] = EqualsFunction.default)
	extends TreeLike[A, Tree[A]]
{
	// IMPLEMENTED    ---------------
	
	override def self = this
	
	override protected def createCopy(content: A, children: Seq[Tree[A]]) = Tree(content, children)
	
	override protected def newNode(content: A) = Tree(content)
	
	
	// OTHER METHODS    -------------
	
	/**
	  * Maps the content of all nodes in this tree
	  * @param f A mapping function
	  * @tparam B Target content type
	  * @return A mapped version of this tree
	  */
	def map[B](f: A => B)(implicit equals: EqualsFunction[B] = EqualsFunction.default): Tree[B] =
		Tree(f(nav), children.map { _.map(f) })(equals)
	
	/**
	  * Maps the content of all nodes in this tree. May map to None where the node is removed.
	  * @param f A mapping function
	  * @tparam B Target content type
	  * @return A mapped version of this tree. None if the content of this node mapped to None.
	  */
	def flatMap[B](f: A => Option[B])(implicit equals: EqualsFunction[B] = EqualsFunction.default): Option[Tree[B]] =
		f(nav).map { c => Tree(c, children.flatMap { _.flatMap(f) })(equals) }
}

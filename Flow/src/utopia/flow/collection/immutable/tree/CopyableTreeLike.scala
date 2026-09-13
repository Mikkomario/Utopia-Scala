package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty

/**
  * A common trait for tree implementations which support copy operations
  * @author Mikko Hilpinen
  * @since 05.06.2026, v2.9
  */
trait CopyableTreeLike[-N, +Repr <: CopyableTreeLike[N, Repr]] extends FilterableTreeLike[Repr]
{
	// ABSTRACT --------------------
	
	/**
	 * @return A factory used for constructing copies of this tree node
	 */
	def factory: TreeFactory[N, Repr]
	/**
	 * @return A factory used for adding more nodes under this tree.
	 *         Always includes this node's existing children.
	 */
	def appendingFactory: TreeFactory[N, Repr]
	/**
	 * @param index Index where new children will be inserted
	 * @param replaceCount Number of existing child nodes (starting from 'index')
	 *                     that will be removed / replaced with the new entries
	 * @return A factory used for inserting and/or updating nodes under this tree
	 */
	def slicingFactory(index: Int, replaceCount: Int = 0): TreeFactory[N, Repr]
	
	
	// IMPLEMENTED -------------
	
	override def withoutChildren = if (hasChildren) withChildren(Empty) else self
	
	
	// OTHER    ----------------
	
	/**
	  * Adds a new direct child node to this tree
	  * @param node A child node to add
	  * @return a copy of this tree with the specified child added
	  */
	def :+(node: N) = appendingFactory.withChild(node)
	/**
	 * Adds n new child nodes directly under this node
	 * @param newChildren New children to add under this node
	 * @return A copy of this tree that includes the specified child nodes
	 */
	def ++(newChildren: IterableOnce[N]): Repr = appendingFactory.withChildren(newChildren)
	
	/**
	 * @param newChildren New children to assign to this tree.
	 *                    Overwrites the existing children.
	 * @return A copy of this tree with the specified children only
	 */
	def withChildren(newChildren: IterableOnce[N]): Repr = factory.withChildren(newChildren)
	
	/**
	 * Replaces one of the child nodes of this tree
	 * @param original Child node to replace
	 * @param replacement Replacing node
	 * @return Copy of this tree with 'original' replaced with 'replacement'.
	 *         Note: If this tree didn't directly contain 'original', 'replacement' is still added.
	 */
	def replaceChild(original: Any, replacement: N) = children.findIndexWhere { _ == original } match {
		case Some(i) => slicingFactory(i, replaceCount = 1).withChild(replacement)
		case None => this :+ replacement
	}
	
	/**
	 * Creates a copy of this node with its direct children mapped
	 * @param f A mapping function for direct child nodes
	 * @return A mapped copy of this node
	 */
	def mapDirect(f: Repr => N) = if (isEmpty) self else withChildren(children.map(f))
	
	/**
	 * Maps the topmost nodes in this tree, which match the specified filter function.
	 * @param filter A filter function that yields true for nodes that should be mapped (using 'f')
	 * @param f A function that performs the mapping for nodes for which 'filter' yielded true.
	 * @return A mapped copy of this tree
	 */
	def mapRootsWhere(filter: Repr => Boolean)(f: Repr => N)(implicit ev: Repr <:< N): Repr =
		mapDirect { c => if (filter(c)) f(c) else c.mapRootsWhere(filter)(f) }
	
	/**
	  * Maps the first node that satisfies the specified search condition.
	 * Targets all nodes under this one.
	  * @param find A search function that yields true for the node to map
	  * @param map A mapping function applied to the found node
	  * @return Either:
	  *         - Left: This tree, if no node satisfied the specified search condition, or
	  *         - Right: A copy of this tree with a single node mapped
	  */
	def mapFirstWhere(find: Repr => Boolean)(map: Repr => N)(implicit ev: Repr <:< N) =
		_mapFirstWhere(find)(map).toRight(self)
	private def _mapFirstWhere(find: Repr => Boolean)(map: Repr => N)(implicit ev: Repr <:< N): Option[Repr] = {
		children.iterator.zipWithIndex
			.findMap { case (c, i) =>
				if (find(c))
					Some(map(c) -> i)
				else
					c._mapFirstWhere(find)(map).map { c => (c: N) -> i }
			}
			.map { case (newChild, i) => slicingFactory(i, replaceCount = 1).withChild(newChild) }
	}
	
	/*
	/**
	 * Creates a copy of this tree that contains the specified trees as some of its child nodes.
	 *
	 * For each node, if this node already contains a matching direct child,
	 * merges it with this new node so that the children of both are kept.
	 *
	 * @param children Child nodes to insert
	 * @param matches Tests whether two nodes should be considered a match
	 * @param merge A function which merges the two node versions together.
	 *              Note: Child nodes from the 2nd node will always be added to the merge result afterwards.
	 * @return a copy of this tree with the specified child included / drafted in
	 */
	def draftIn(children: IterableOnce[Repr])(matches: EqualsFunction[Repr])(merge: (Repr, Repr) => Repr): Repr =
		children.iterator.foldLeft(self) { _.draftIn(_)(matches)(merge) }
	/**
	 * Creates a copy of this tree that contains the specified tree as one of its child nodes.
	 * If this node already contains a matching child, merges it with this new node so that the children of both
	 * are kept.
	 * @param child A child node to insert
	 * @param matches Tests whether two nodes should be considered a match
	 * @param merge A function which merges the two node versions together.
	 *              Note: Child nodes from the 2nd node will always be added to the merge result afterwards.
	 * @return a copy of this tree with the specified child included / drafted in
	 */
	def draftIn[N2 <: N with TreeLike2[N2]](child: N2)
	                                       (matches: (Repr, N2) => Boolean)(merge: (Repr, N2) => N2): Repr =
	{
		children.findIndexWhere { matches(_, child) } match {
			case Some(i) =>
				val merged = child.children.foldLeft(merge(children(i), child)) { (parent, child) =>
					if (parent.containsDirect(child))
						parent
					else
						parent.dra
				}
			
			case None => this :+ child
		}
		
		withChildren(children.mergeOrAppend(child) { matches(_, child) } { (existing, newEntry) =>
			// Merges the two nodes together, and drafts in all the new children as well
			newEntry.children.foldLeft(merge(existing, newEntry)) { _.draftIn(_)(matches)(merge) }
		})
	}
	 */
}

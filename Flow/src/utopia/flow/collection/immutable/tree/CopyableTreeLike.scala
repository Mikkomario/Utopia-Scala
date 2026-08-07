package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template.tree.{Tree, TreeLike2}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.util.Mutate

/**
  * A common trait for tree implementations which support copy operations
  * @author Mikko Hilpinen
  * @since 05.06.2026, v2.9
  */
trait CopyableTreeLike[Repr <: CopyableTreeLike[Repr]] extends TreeLike2[Repr]
{
	// ABSTRACT --------------------
	
	/**
	 * @param newChildren New children to apply (overrides existing values)
	 * @return A copy of this tree with the specified child nodes
	 */
	def withChildren(newChildren: IterableOnce[Repr]): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return A copy of this tree without any child nodes included
	  */
	def withoutChildren = withChildren(Empty)
	
	
	// OTHER    ----------------
	
	/**
	  * Adds a new direct child node to this tree
	  * @param node A child node to add
	  * @return a copy of this tree with the specified child added
	  */
	def :+(node: Repr) = withChildren(children :+ node)
	/**
	 * Adds n new child nodes directly under this node
	 * @param newChildren New children to add under this node
	 * @return A copy of this tree that includes the specified child nodes
	 */
	def ++(newChildren: IterableOnce[Repr]): Repr = withChildren(children ++ newChildren)
	/**
	  * Creates a new copy of this tree where the specified tree doesn't occur anywhere.
	  * @param node The tree that is not included in the copy
	  * @return A copy of this tree without the provided tree
	  */
	def -(node: Tree): Repr = {
		if (isEmpty)
			self
		else
			withChildren(children.view.filterNot { _ == node } map { _ - node })
	}
	
	/**
	  * @param child A new child node
	  * @return A copy of this tree with that child node added
	  */
	@deprecated("Please use this :+ child instead", "v2.9")
	def withChildAdded(child: Repr) = this :+ child
	/**
	  * @param newChildren New child nodes
	  * @return A copy of this tree with those child nodes added inder it
	  */
	@deprecated("Please use this ++ newChildren instead", "v2.9")
	def withChildrenAdded(newChildren: IterableOnce[Repr]) = this ++ newChildren
	/**
	  * Creates a new copy of this tree without the provided direct child node
	  * @param child The child node that is removed from the direct children under this tree
	  */
	def withoutDirect(child: TreeLike2[_]) = filterDirect { _ == child }
	@deprecated("Renamed to withoutDirect(Tree)", "v2.9")
	def withoutChild(child: Tree) = withoutDirect(child)
	
	/**
	 * Replaces one of the child nodes of this tree
	 * @param original Child node to replace
	 * @param replacement Replacing node
	 * @return Copy of this tree with 'original' replaced with 'replacement'.
	 *         Note: If this tree didn't directly contain 'original', 'replacement' is still added.
	 */
	def replaceChild(original: Repr, replacement: Repr) = {
		var updated = false
		val mapped = children.mapFirstWhere { _ == original } { _ =>
			updated = true
			replacement
		}
		if (updated) withChildren(mapped) else this :+ replacement
	}
	
	/**
	  * Filters this whole tree structure using the specified filter function
	  * @param f A filtering function that determines which nodes should be kept
	  * @return A filtered copy of this tree
	  */
	def filter(f: Repr => Boolean): Repr =
		if (isEmpty) self else withChildren(children.view.filter(f).map { _.filter(f) })
	/**
	 * Filters the children directly under this node
	 * @param f A function that determines whether a direct child should be kept attached to this node
	 * @return A filtered copy of this tree
	 */
	def filterDirect(f: Repr => Boolean) = if (isEmpty) self else withChildren(children.filter(f))
	
	/**
	 * Creates a copy of this node with its direct children mapped
	 * @param f A mapping function for direct child nodes
	 * @return A mapped copy of this node
	 */
	def mapDirect(f: Mutate[Repr]) = if (isEmpty) self else withChildren(children.map(f))
	/**
	 * Creates a copy of this node with its direct children mapped
	 * @param f A mapping function for direct child nodes
	 * @return A mapped copy of this node
	 */
	@deprecated("Renamed to mapDirect(...)", "v2.9")
	def mapChildren(f: Repr => Repr) = mapDirect(f)
	
	/**
	 * Maps the topmost nodes in this tree, which match the specified filter function.
	 * Note: Might map self.
	 * @param filter A filter function that yields true for nodes that should be mapped (using 'f')
	 * @param f A function that performs the mapping for nodes for which 'filter' yielded true.
	 * @return A mapped copy of this tree
	 */
	def mapRootsWhere(filter: Repr => Boolean)(f: Mutate[Repr]): Repr =
		if (filter(self)) f(self) else mapDirect { _.mapRootsWhere(filter)(f) }
	/**
	  * Maps all (top level) nodes within this tree that satisfy the specified search condition
	  * @param find A function that tells whether a node should be mapped (true) or not (false).
	  * @param map A mapping function
	  * @return A copy of this tree where all (highest) nodes that satisfy the specified search
	  *         condition have been mapped.
	  */
	@deprecated("Renamed to mapRootsWhere(...)", "v2.9")
	def mapAllWhere(find: Repr => Boolean)(map: Repr => Repr): Repr = mapRootsWhere(find)(map)
	/**
	 * Maps the topmost nodes in this tree, which match the specified filter function.
	 * @param filter A filter function that yields true for nodes that should be mapped (using 'f')
	 * @param f A function that performs the mapping for nodes for which 'filter' yielded true.
	 * @return A mapped copy of this tree
	 */
	def mapRootsBelowWhere(filter: Repr => Boolean)(f: Mutate[Repr]) = mapDirect { _.mapRootsWhere(filter)(f) }
	
	/**
	  * Maps the first node that satisfies the specified search condition
	  * @param find A search function that yields true for the node to map
	  * @param map A mapping function applied to the found node
	  * @return Either:
	  *         - Left: This tree, if no node satisfied the specified search condition, or
	  *         - Right: A copy of this tree with a single node mapped
	  */
	def mapFirstWhere(find: Repr => Boolean)(map: Repr => Repr) =
		_mapFirstWhere(find)(map).toRight(self)
	private def _mapFirstWhere(find: Repr => Boolean)(map: Mutate[Repr]): Option[Repr] = {
		if (find(self))
			Some(map(self))
		else
			children.iterator.zipWithIndex.findMap { case (c, i) =>
				c._mapFirstWhere(find)(map).map { c2 => withChildren(children.updated(i, c2)) }
			}
	}
	
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
	def draftIn(child: Repr)(matches: EqualsFunction[Repr])(merge: (Repr, Repr) => Repr): Repr =
		withChildren(children.mergeOrAppend(child) { matches(_, child) } { (existing, newEntry) =>
			// Merges the two nodes together, and drafts in all the new children as well
			newEntry.children.foldLeft(merge(existing, newEntry)) { _.draftIn(_)(matches)(merge) }
		})
}

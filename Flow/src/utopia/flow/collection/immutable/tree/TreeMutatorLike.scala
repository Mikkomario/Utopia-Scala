package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.template.tree.{TreeLike2, TreeNavigator}
import utopia.flow.util.Mutate

import scala.annotation.tailrec
import scala.annotation.unchecked.uncheckedVariance

/**
 * Used for mutating a deeper section of a tree
 * @tparam Nav Type of the accepted navigational elements
 * @tparam N Type of input accepted for node creation
 * @tparam Node type of the modified tree nodes
 * @tparam Repr Type of the implementing mutator
 * @author Mikko Hilpinen
 * @since 07.08.2026, v2.9
 */
trait TreeMutatorLike[-Nav, N, Node <: CopyableTreeLike[N, Node], +Repr <: TreeLike2[Node]]
	extends TreeNavigator[Nav, Repr] with CopyableTreeLike[N, Node]
{
	// ABSTRACT   ------------------------
	
	/**
	 * @return The root level node that's being modified / mutated, ultimately.
	 */
	protected def root: Node
	/**
	 * @return A path leading to the currently targeted node.
	 *         Should NOT contain [[root]]. Should contain [[node]] as the last element.
	 */
	protected def path: Seq[Node]
	/**
	 * @return The currently targeted node
	 */
	def node: Node
	/**
	 * @return Whether [[node]] has been generated and is not originally part of [[root]].
	 */
	def generated: Boolean
	
	/**
	 * Wraps a child of [[node]], yielding a new mutator targeting that child.
	 * @param child A child node to wrap
	 * @param generated Whether this node has been created for this function and didn't previously exist.
	 * @return A tree mutator instance wrapping the specified child node.
	 */
	protected def wrapChild(child: Node, generated: Boolean = false): Repr
	/**
	 * Wraps an updated version of a child node, in order to append or insert it to a parent node
	 * @param child Updated child node to wrap
	 * @return A node-creation element matching that updated child
	 */
	protected def wrapUpdatedChild(child: Node): N
	
	/**
	 * Finds a node matching a navigational element
	 * @param nodes Nodes to search from
	 * @param nav Navigational element being targeted
	 * @return A node from 'nodes' matching 'nav'. None if no node matched 'nav'.
	 */
	protected def findNodeFor(nodes: Seq[Node], nav: Nav): Option[Node]
		
	
	// COMPUTED     ----------------------
	
	/**
	 * @return Copy of the root node including this node
	 */
	def included = {
		if (generated) {
			val pathIter = ascendingIter
			pathIter.nextOption() match {
				case Some(parent) => assign(parent, parent :+ wrapUpdatedChild(node), pathIter)
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
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: Node = node
	
	override def children: Seq[Node] = node.children
	
	override def appendingFactory: TreeFactory[N, Node] = node.appendingFactory.mapResult(replacedWith)
	override def slicingFactory(index: Int, replaceCount: Int): TreeFactory[N, Node] =
		node.slicingFactory(index, replaceCount).mapResult(replacedWith)
	
	override protected def findUnder(parent: Repr @uncheckedVariance, nav: Nav): Option[Repr] =
		findNodeFor(parent.children, nav).map { wrapChild(_) }
	
	override def filterDirect(f: Node => Boolean): Node = {
		if (node.hasChildren)
			mapped { _.filterDirect(f) }
		else
			root
	}
	override def filter(f: Node => Boolean): Node = {
		if (node.hasChildren)
			mapped { _.filter(f) }
		else
			root
	}
	
	override def factory: TreeFactory[N, Node] = RootModifyingFactory
	
	
	// OTHER    -----------------------
	
	/**
	 * Modifies this node under the root node
	 * @param f A mapping function to apply to this node
	 * @return A copy of [[root]] with a modified copy of this node
	 */
	def mapped(f: Mutate[Node]) = replacedWith(f(node))
	
	/**
	 * Replaces the current node with a new version, yielding a modified copy of the root node.
	 * @param updated Updated version of [[node]].
	 * @return Updated version of [[root]]
	 */
	def replacedWith(updated: Node) = {
		if (generated) {
			val pathIter = ascendingIter
			pathIter.nextOption() match {
				case Some(parent) => assign(parent, parent :+ wrapUpdatedChild(updated), pathIter)
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
	private def assign(original: Node, updated: Node, pathIter: Iterator[Node]): Node = {
		// Case: Reached root => Yields the modified version
		if (!pathIter.hasNext || original == root)
			updated
		// Case: Still going up => Replaces the updated node within the parent
		else {
			val nextOriginal = pathIter.next()
			assign(nextOriginal, nextOriginal.replaceChild(original, wrapUpdatedChild(updated)), pathIter)
		}
	}
		
	
	// NESTED   -----------------------------
	
	private object RootModifyingFactory extends TreeFactory[N, Node]
	{
		override def withChildren(children: IterableOnce[N]): Node = {
			children.nonEmptyCollection match {
				case Some(children) => assign(node, node.withChildren(children), ascendingIter)
				case None =>
					// Case: Generated node with no children added => Won't modify root
					if (generated)
						root
					else
						assign(node, node.withoutChildren, ascendingIter)
			}
		}
	}
}

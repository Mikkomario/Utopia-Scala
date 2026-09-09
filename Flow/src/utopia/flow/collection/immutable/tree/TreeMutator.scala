package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.template.tree.TreeNavigator
import utopia.flow.util.Mutate

import scala.annotation.tailrec

@deprecated("Deprecated for removal. Replaced with TreeMutatorLike", "v2.9")
object TreeMutator
{
	/**
	 * Creates a new tree mutator
	 * @param root The modified root level node
	 * @param path Targeted path under the root node
	 * @param matches A function which determines whether a node (1) matches a nav element (2)
	 * @param newNode A function which generates a new node for nav elements, which don't appear in the tree
	 * @tparam N Type of nav elements used
	 * @tparam Node Type of nodes interacted with
	 * @return A new tree mutator interface, pointing to the node at the end of 'path'
	 */
	def apply[N, Node <: CopyableTreeLike[Node, Node]](root: Node, path: Seq[N])
	                                                  (matches: (Node, N) => Boolean)(newNode: N => Node) =
	{
		var generated = false
		val nodePath = path
			.foldLeftIterator(root) { (node, nav) =>
				if (generated)
					newNode(nav)
				else
					node.children.find { matches(_, nav) }.getOrElse {
						generated = true
						newNode(nav)
					}
			}
			.toOptimizedSeq
		
		new TreeMutator[N, Node](root, nodePath.tail, nodePath.last, generated)(matches)(newNode)
	}
}

/**
 * Used for mutating a deeper section of a tree
 * @tparam Nav Type of the accepted navigational elements
 * @tparam Node type of the modified tree nodes
 * @param root The modified root level node
 * @param path A path leading to the currently targeted node.
 *             Should not contain the root. Should contain 'node' as the last element.
 * @param node Currently targeted node
 * @param generated Whether 'node' has been generated and is not originally part of 'root'
 * @param matcher A function which determines whether a node matches a navigational element
 * @param navToNode A function which generates a new node, based on a nav element
 * @author Mikko Hilpinen
 * @since 07.08.2026, v2.9
 */
@deprecated("Deprecated for removal. Replaced with TreeMutatorLike", "v2.9")
class TreeMutator[Nav, Node <: CopyableTreeLike[Node, Node]](root: Node, path: Seq[Node],
                                                             val node: Node, generated: Boolean = false)
                                                            (matcher: (Node, Nav) => Boolean)(navToNode: Nav => Node)
	extends TreeNavigator[Nav, TreeMutator[Nav, Node]] with CopyableFromNodesTreeLike[Node]
{
	// ATTRIBUTES   ----------------------
	
	override val children: Seq[Node] = node.children
		
	
	// COMPUTED     ----------------------
	
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
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: Node = node
	
	override def factory: TreeFactory[Node, Node] = RootModifyingFactory
	override protected def current: TreeMutator[Nav, Node] = this
	
	override protected def findUnder(parent: TreeMutator[Nav, Node], nav: Nav): Option[TreeMutator[Nav, Node]] =
		parent.children.find { c => matcher(c, nav) }.map { wrapChild(_) }
		
	override protected def nodeFor(nav: Nav): TreeMutator[Nav, Node] = wrapChild(navToNode(nav), generated = true)
	
	
	// OTHER    -----------------------
	
	/**
	 * Modifies this node under the root node
	 * @param f A mapping function to apply to this node
	 * @return A copy of root with a modified copy of this node
	 */
	def mapped(f: Mutate[Node]) = {
		if (generated) {
			val pathIter = ascendingIter
			pathIter.nextOption() match {
				case Some(parent) => assign(parent, parent :+ f(node), pathIter)
				case None => f(node)
			}
		}
		else
			assign(node, f(node), ascendingIter)
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
			assign(nextOriginal, nextOriginal.replaceChild(original, updated), pathIter)
		}
	}
	
	private def wrapChild(child: Node, generated: Boolean = false) =
		new TreeMutator[Nav, Node](root, path :+ child, child, generated)(matcher)(navToNode)
		
	
	// NESTED   -----------------------------
	
	private object RootModifyingFactory extends TreeFactory[Node, Node]
	{
		override def withChildren(children: IterableOnce[Node]): Node = {
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

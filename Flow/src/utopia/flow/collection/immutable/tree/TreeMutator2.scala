package utopia.flow.collection.immutable.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.template.tree.TreeNavigator
import utopia.flow.util.Mutate

import scala.annotation.tailrec


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
trait TreeMutator2[Nav, N, Node <: CopyableTreeLike[N, Node]]
	extends TreeNavigator[Nav, TreeMutator2[Nav, N, Node]] with CopyableTreeLike[N, Node]
{
	// ABSTRACT   ------------------------
	
	protected def root: Node
	
	protected def path: Seq[Node]
	
	def node: Node
	
	def generated: Boolean
	
	protected def wrapChild(child: Node, generated: Boolean = false): TreeMutator2[Nav, N, Node]
	
	protected def wrapUpdatedChild(child: Node): N
	
	protected def findNodeFor(nodes: Seq[Node], nav: Nav): Option[Node]
		
	
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
	
	override def children: Seq[Node] = node.children
	
	override protected def findUnder(parent: TreeMutator2[Nav, N, Node], nav: Nav): Option[TreeMutator2[Nav, N, Node]] =
		findNodeFor(parent.children, nav).map { wrapChild(_) }
	
	override def appendingFactory: TreeFactory[N, Node] = ???
	
	override def slicingFactory(index: Int, replaceCount: Int): TreeFactory[N, Node] = ???
	
	override def filterDirect(f: Node => Boolean): Node = ???
	
	override def filter(f: Node => Boolean): Node = ???
	
	override def factory: TreeFactory[N, Node] = RootModifyingFactory
	override protected def current: TreeMutator2[Nav, N, Node] = this
	
	
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

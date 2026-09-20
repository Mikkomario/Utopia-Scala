package utopia.flow.collection.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.View

import scala.annotation.unchecked.uncheckedVariance

object LookupPath
{
	// OTHER    ---------------------------
	
	/**
	 * @param origin The root/starting node
	 * @tparam Node Type of searched nodes
	 * @return A factory for constructing lookup interfaces from that node
	 */
	def from[Node](origin: Node) = new LookUpPathFactory[Node](origin)
	
	
	// NESTED   ---------------------------
	
	class LookUpPathFactory[Node](origin: Node)
	{
		/**
		 * @param f A function that attempts to find a matching element accessible from a node
		 * @tparam N Type of the navigational values used
		 * @return A new path lookup interface using the specified search function
		 */
		def findUsing[N](f: (Node, N) => Option[Node]): LookupPath[N, Node] = new _LookupPath[N, Node](origin, f)
		
		/**
		 * @param childrenOf A function that yields the directly accessible (child) nodes of a specific node
		 * @param eq Implicit equals function to use for comparing node values
		 * @param ev Implicit evidence that the searched nodes are views
		 * @tparam N Type of navigational elements used
		 * @return A new path lookup interface that compares nav elements with node values
		 */
		def matchingValues[N](childrenOf: Node => IterableOnce[Node])
		                     (implicit eq: EqualsFunction[N] = EqualsFunction.default,
		                      ev: Node <:< View[N]): LookupPath[N, Node] =
			new LookupValuePath[N, Node](origin, childrenOf, _.value)
	}
	
	private class LookupValuePath[-N, Node](override val current: Node, childrenOf: Node => IterableOnce[Node],
	                                        valueOf: Node => N)
	                                       (implicit eq: EqualsFunction[N])
		extends LookupPath[N, Node]
	{
		override protected def findUnder(parent: Node, nav: N): Option[Node] =
			childrenOf(parent).iterator.find { child => eq(valueOf(child), nav) }
	}
	
	private class _LookupPath[-N, Node](override val current: Node, f: (Node, N) => Option[Node])
		extends LookupPath[N, Node]
	{
		override protected def findUnder(parent: Node, nav: N): Option[Node] = f(parent, nav)
	}
}

/**
 * Common trait for implementations that provide navigation based on some navigation elements.
 * @tparam N Type of navigational elements accepted
 * @tparam Node Type of nodes yielded
 * @author Mikko Hilpinen
 * @since 20.09.2026, v2.9
 */
trait LookupPath[-N, +Node]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The node that's targeted with an empty path
	 */
	protected def current: Node
	
	/**
	 * Checks whether a tree node matches a navigational element
	 * @param parent Node under which other nodes are sought.
	 *
	 *               NB: Must be part of the navigated tree, i.e. <= Node.
	 *                   If this condition is met, this may be kept @uncheckedVariance.
	 *
	 * @param nav Navigational element to find
	 * @return A node directly under 'parent', which matches the specified 'nav'
	 */
	protected def findUnder(parent: Node @uncheckedVariance, nav: N): Option[Node]
	
	
	// OTHER    --------------------------
	
	/**
	 * Finds a child directly under this node that matches the specified navigational step
	 * @param nav The navigational step to take next if a matching node is found
	 * @return The first child that matches the specified step. None if no such (direct) child was found.
	 */
	def get(nav: N) = findUnder(current, nav)
	/**
	 * @param first First step to take
	 * @param second Second step to take
	 * @param more More steps to take
	 * @return Node at the end of the specified path.
	 *         None if no existing node lies at the end of that path.
	 */
	def get(first: N, second: N, more: N*): Option[Node] = get(Pair(first, second) ++ more)
	/**
	 * @param path A path of navigational steps to take. Ordered.
	 * @return Node at the end of that path.
	 *         None if no existing node lies at the end of that path.
	 */
	def get(path: IterableOnce[N]) =
		path.foldLeftIterator[Option[Node]](Some(current)) { case (node, nav) => node.flatMap { findUnder(_, nav) } }
			.takeTo { _.isEmpty }.last
}

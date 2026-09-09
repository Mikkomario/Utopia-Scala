package utopia.flow.collection.template.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair

import scala.annotation.unchecked.uncheckedVariance

/**
 * Common trait for implementations that provide tree navigation based on some navigation elements
 * @tparam N Type of navigational elements accepted
 * @tparam Node Type of nodes yielded
 * @author Mikko Hilpinen
 * @since 05.06.2026, v2.9
 */
trait TreeNavigator[-N, +Node]
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
	/**
	 * @param nav A nav element that didn't match a node in this tree
	 * @return A new node that matches the specified nav element
	 */
	protected def nodeFor(nav: N): Node
	
	
	// OTHER    --------------------------
	
	/**
	 * Finds or generates a node directly under this one
	 * @param nav The next navigation "step"
	 * @return Either an existing node or a made-up one
	 */
	def /(nav: N) = get(nav).getOrElse { nodeFor(nav) }
	/**
	 * @param path A path of navigational steps to take. Ordered.
	 * @return Node at the end of that path. May be generated.
	 */
	def /(path: Iterable[N]) = get(path).getOrElse { nodeFor(path.last) }
	/**
	 * Finds or generates a node directly under this one
	 * @param nav The next navigation "step"
	 * @return Either an existing node or a made-up one
	 */
	def apply(nav: N) = this/nav
	/**
	 * Finds or generates a node under this one
	 * @param first The next navigation "step"
	 * @param second The step after that
	 * @param more Additional steps to take
	 * @return Node at the end of the specified navigation path. May be generated.
	 */
	def apply(first: N, second: N, more: N*) = this / (Pair(first, second) ++ more)
	
	/**
	 * Finds a child directly under this node that matches the specified navigational step
	 * @param nav The navigational step to take next if a matching node is found
	 * @return The first child that matches the specified step. None if no such (direct) child was found.
	 */
	def get(nav: N) = findUnder(current, nav)
	/**
	 * @param path A path of navigational steps to take. Ordered.
	 * @return Node at the end of that path. May be generated.
	 */
	def get(path: IterableOnce[N]) =
		path.foldLeftIterator[Option[Node]](Some(current)) { case (node, nav) => node.flatMap { findUnder(_, nav) } }
			.takeTo { _.isEmpty }.last
}

package utopia.flow.collection.template.tree

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair}

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
	 * Generates a new node
	 * @param nav A nav element that didn't match a node in this tree
	 * @return A new node that matches the specified nav element
	 */
	protected def nodeFor(nav: N): Node
	/**
	 * Generates a new node at the end of a path
	 * @param parents Existing nodes that lead to the targeted node. Always starts with [[current]].
	 *
	 *                NB: Must be part of the navigated tree, i.e. <= Node.
	 *                    If this condition is met, this may be kept @uncheckedVariance.
	 *
	 * @param path An iterator that yields the remaining path. Never empty.
	 * @return A new node that matches the nav element at the end of 'path'
	 */
	protected def nodeForPath(parents: Seq[Node] @uncheckedVariance, path: Iterator[N]): Node
	
	
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
	def /(path: IterableOnce[N]) = {
		// Traverses the path as long as existing nodes are found
		val pathIter = path.iterator
		var parent = current
		var missingStep: Option[N] = None
		val previousParentsBuilder = OptimizedIndexedSeq.newBuilder[Node]
		
		while (missingStep.isEmpty && pathIter.hasNext) {
			val step = pathIter.next()
			findUnder(parent, step) match {
				case Some(next) =>
					previousParentsBuilder += parent
					parent = next
					
				// Case: Node not found => Stops traversing the path and remembers the missing step
				case None => missingStep = Some(step)
			}
		}
		
		missingStep match {
			// Case: The specified path didn't lead to an existing node => Generates a new node
			case Some(nextStep) =>
				// Case: Not traversing a deep path => Uses the simpler 'nodeFor' function
				if (previousParentsBuilder.isEmpty && pathIter.isEmpty)
					nodeFor(nextStep)
				else {
					previousParentsBuilder += parent
					nodeForPath(previousParentsBuilder.result(), Iterator.single(nextStep) ++ pathIter)
				}
			// Case: An existing node found => Returns it
			case None => parent
		}
	}
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
	 * @param path A path of navigational steps to take. Ordered.
	 * @return Node at the end of that path. May be generated.
	 */
	def apply(path: Iterable[N]) = this/path
	
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

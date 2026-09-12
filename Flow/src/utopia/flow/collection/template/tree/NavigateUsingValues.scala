package utopia.flow.collection.template.tree

import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.View

object NavigateUsingValues
{
	// OTHER    ----------------------
	
	/**
	 * @param root Root node from which navigation is performed
	 * @tparam A Type of the values in this tree
	 * @tparam Node Type of the nodes in this tree
	 * @return A factory for constructing navigators from that node
	 */
	def from[A, Node <: TreeLike2[Node] with View[A]](root: Node) = new NavigatorFactory[A, Node](root)
	
	/**
	 * @param root Root node from which navigation is performed
	 * @param wrapNav A function that wraps a nav element and yields a new node
	 * @param eq Implicit equals function used for comparing nav elements and tree values
	 * @tparam A Type of the tree values returned
	 * @tparam Nav Type of the nav elements used
	 * @tparam Node Type of the nodes returned
	 * @return
	 */
	def apply[A, Nav >: A, Node <: TreeLike2[Node] with View[A]](root: Node)(wrapNav: Nav => Node)
	                                                            (implicit eq: EqualsFunction[Nav] = EqualsFunction.default) =
		new NavigateUsingValues[A, Nav, Node](root)(wrapNav)(eq)
	
	
	// NESTED   ----------------------
	
	// TODO: Remove this factory class. The type parameters are not being interpreted correctly
	class NavigatorFactory[A, Node <: TreeLike2[Node] with View[A]](root: Node)
	{
		/**
		 * @param wrapNav A function that accepts a nav element and wraps it in a new node
		 * @param eq An implicit equals function used for comparing nav elements and node values.
		 *           Default = use ==.
		 * @tparam Nav Type of the accepted nav elements
		 * @return A new tree navigator
		 */
		def apply[Nav >: A](wrapNav: Nav => Node)(implicit eq: EqualsFunction[Nav] = EqualsFunction.default) =
			new NavigateUsingValues[A, Nav, Node](root)(wrapNav)(eq)
	}
}

/**
 * A simple [[TreeNavigator]] implementation that supports value trees
 * @param current The wrapped (root) node
 * @param wrapNav A function that wraps a nav element into a tree node
 * @param eq An implicit equals function used for comparing nav elements and node values. Default = use ==.
 * @author Mikko Hilpinen
 * @since 11.09.2026, v2.9
 */
class NavigateUsingValues[A, -Nav >: A, Node <: TreeLike2[Node] with View[A]](override protected val current: Node)
                                                                             (wrapNav: Nav => Node)
                                                                             (implicit eq: EqualsFunction[Nav] = EqualsFunction.default)
	extends TreeNavigator[Nav, Node]
{
	override protected def findUnder(parent: Node, nav: Nav): Option[Node] =
		parent.children.find { node => eq(node.value, nav) }
	
	override protected def nodeFor(nav: Nav): Node = wrapNav(nav)
}

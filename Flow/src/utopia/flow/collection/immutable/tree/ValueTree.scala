package utopia.flow.collection.immutable.tree

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.template
import utopia.flow.collection.template.tree.ValueTreeLike
import utopia.flow.operator.equality.EqualsFunction

object ValueTree
{
	def apply[A](value: A, children: Seq[ValueTree[A]] = Empty): ValueTree[A] = ???
}

/**
 * Common trait for immutable trees where each node wraps some kind of value
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
trait ValueTree[A, -N]
	extends template.tree.ValueTree[A] with CopyableValueTreeLike[A, N, ValueTree[A, N]]
{
	/**
	 * @param value Child node value to add
	 * @return A copy of this tree including a new child with that value
	 */
	def :+(value: A): ValueTree[A, N] = withChildren(children :+ ValueTree(value))
	/**
	 * Creates a new tree that contains a child node with specified value.
	 * If this node already contains such a child, doesn't add a new one.
	 * @param value Child node value
	 * @return A copy of this tree including a child with that value
	 */
	def including(value: A)(implicit eq: EqualsFunction[A] = EqualsFunction.default) = {
		// Case: This node already contains that child => ignores
		if (children.exists { _.value ~== value })
			self
		// Case: New child => inserts
		else
			this :+ value
	}
	
	def mutate[Nav >: A](step: N, otherSteps: N*)(implicit eq: EqualsFunction[Nav] = EqualsFunction.default) =
		mutatePath[Nav](step +: otherSteps)
	
	def mutatePath[Nav >: A](path: Seq[N])(implicit eq: EqualsFunction[Nav] = EqualsFunction.default) =
		mutatePathUsing(path, eq)
	
	def mutatePathUsing[Nav >: A](path: Seq[N], eq: EqualsFunction[Nav]) =
		TreeMutator(self, path) { (node, nav) => eq.apply(node.value, nav) } { ValueTree(_) }
}
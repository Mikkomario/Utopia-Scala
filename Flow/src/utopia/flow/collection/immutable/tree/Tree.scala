package utopia.flow.collection.immutable.tree

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.template

object Tree extends TreeFactory[Tree, Tree]
{
	// IMPLEMENTED  -----------------------
	
	override def withChildren(children: IterableOnce[Tree]): Tree = apply(OptimizedIndexedSeq.from(children))
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param children Children to assign to this tree
	 * @return A tree with the specified child nodes
	 */
	def apply(children: Seq[Tree]): Tree = new _Tree(children)
	
	
	// NESTED   ---------------------------
	
	private class _Tree(override val children: Seq[Tree]) extends Tree with CopyableFromNodesTreeLike[Tree]
	{
		// ATTRIBUTES   -------------------
		
		override val factory: TreeFactory[Tree, Tree] = Tree
		
		
		// IMPLEMENTED  ------------------
		
		override def self: Tree = this
	}
}

/**
 * Common trait for immutable trees.
 * Based on [[CopyableTreeLike]], but removes the generic Repr type.
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
// TODO: We most likely need to remove this trait
trait Tree extends template.tree.Tree with CopyableTreeLike[Tree, Tree]

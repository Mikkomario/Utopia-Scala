package utopia.flow.collection.immutable.tree

/**
 * A simplification of [[CopyableTreeLike]], where accepted additions are completed tree nodes.
 * @author Mikko Hilpinen
 * @since 19.08.2026, v2.9
 */
trait CopyableFromNodesTreeLike[Repr <: CopyableTreeLike[Repr, Repr]] extends CopyableTreeLike[Repr, Repr]
{
	override def appendingFactory: TreeFactory[Repr, Repr] = factory.appendingTo(children)
	override def slicingFactory(index: Int, replaceCount: Int): TreeFactory[Repr, Repr] =
		factory.slicing(children, index, replaceCount)
	
	override def filterDirect(f: Repr => Boolean): Repr =
		if (isEmpty) self else factory.withChildren(children.filter(f))
	override def filter(f: Repr => Boolean): Repr =
		if (isEmpty) self else factory.withChildren(children.view.filter(f).map { _.filter(f) })
}

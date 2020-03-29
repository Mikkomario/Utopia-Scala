package utopia.flow.datastructure.template

object Node
{
    def contentsOf[T](nodes: TraversableOnce[Node[T]]) = nodes.map { node => node.content }
}

/**
 * Nodes are elements that contain data
 * @author Mikko Hilpinen
 * @since 28.10.2016
 */
trait Node[T]
{
    /**
     * The contents of the node
     */
    def content: T
}
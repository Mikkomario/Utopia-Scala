package utopia.flow.collection.immutable.tree

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.immutable.tree.TreeFactory.{AppendingFactory, MappingFactory, ReplacingFactory}

import scala.collection.{SeqView, View}
import scala.language.implicitConversions

object TreeFactory
{
	// IMPLICIT -----------------------
	
	/**
	 * @param f A function that receives a collection of child nodes and produces the parent node
	 * @tparam N Type of the child node inputs
	 * @tparam T Type of the produced parent node
	 * @return A new tree factory
	 */
	implicit def apply[N , T](f: IterableOnce[N] => T): TreeFactory[N, T] = new _TreeFactory[N, T](f)
	
	
	// NESTED   -----------------------
	
	private class MappingFactory[-N, T1, +T2](delegate: TreeFactory[N, T1], f: T1 => T2) extends TreeFactory[N, T2]
	{
		override def withChildren(children: IterableOnce[N]): T2 = f(delegate.withChildren(children))
	}
	
	private class AppendingFactory[-N, +T](delegate: TreeFactory[N, T], existing: Iterable[N])
		extends TreeFactory[N, T]
	{
		override def withChildren(children: IterableOnce[N]): T = children match {
			case i: Iterable[N] => delegate.withChildren(View.concat(existing, i))
			case i => delegate.withChildren(existing.iterator ++ i)
		}
	}
	
	private class ReplacingFactory[-N, +T](delegate: TreeFactory[N, T], existingView: SeqView[N], index: Int,
	                                       replaceCount: Int)
		extends TreeFactory[N, T]
	{
		override def withChildren(children: IterableOnce[N]): T = {
			children match {
				case i: Iterable[N] =>
					delegate.withChildren(View.concat(existingView.take(index), i,
						existingView.drop(index + replaceCount)))
					
				case i =>
					delegate.withChildren(existingView.take(index).iterator ++ i ++
						existingView.drop(index + replaceCount))
			}
		}
	}
	
	private class _TreeFactory[-N, +T](f: IterableOnce[N] => T) extends TreeFactory[N, T]
	{
		override def withChildren(children: IterableOnce[N]): T = f(children)
	}
}

/**
 * Common trait for factories which produce new tree instances
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
trait TreeFactory[-N, +T]
{
	// ABSTRACT -----------------------
	
	/**
	 * @param children Children to assign
	 * @return A new tree node containing the specified child nodes
	 */
	def withChildren(children: IterableOnce[N]): T
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return A tree node with no children
	 */
	def withoutChildren = withChildren(Empty)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param child Child node to include
	 * @return A tree node with a single child node
	 */
	def withChild(child: N) = withChildren(Single(child))
	
	/**
	 * @param children Existing child nodes
	 * @return A copy of this factory that always includes the specified nodes
	 */
	def appendingTo(children: Iterable[N]): TreeFactory[N, T] = new AppendingFactory[N, T](this, children)
	/**
	 * @param children Existing child nodes
	 * @param atIndex Index to which new nodes are inserted
	 * @param replacing Number of 'children' to replace at 'atIndex'
	 * @return A copy of this factory that inserts new children to a specific index
	 */
	def slicing(children: Seq[N], atIndex: Int, replacing: Int = 0): TreeFactory[N, T] =
		slicing(children.view, atIndex, replacing)
	/**
	 * @param children View of the existing child nodes
	 * @param atIndex Index to which new nodes are inserted
	 * @param replacing Number of 'children' to replace at 'atIndex'
	 * @return A copy of this factory that inserts new children to a specific index
	 */
	def slicing(children: SeqView[N], atIndex: Int, replacing: Int): TreeFactory[N, T] =
		new ReplacingFactory[N, T](this, children, atIndex, replacing)
	
	/**
	 * @param f A mapping function applied to the generated node(s)
	 * @tparam T2 Type of the mapped nodes
	 * @return A copy of this factory applying the specified mapping function
	 */
	def mapResult[T2](f: T => T2): TreeFactory[N, T2] = new MappingFactory[N, T, T2](this, f)
}

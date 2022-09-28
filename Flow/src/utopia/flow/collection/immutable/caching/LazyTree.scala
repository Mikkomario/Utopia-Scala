package utopia.flow.collection.immutable.caching

import utopia.flow.collection.immutable.TreeLike
import utopia.flow.collection.immutable.caching.iterable.LazySeq
import utopia.flow.operator.EqualsFunction
import utopia.flow.view.immutable.caching.Lazy

// TODO: Add constructors

/**
  * A lazily initialized tree
  * @author Mikko Hilpinen
  * @since 28.9.2022, v2.0
  */
class LazyTree[A](lazyNav: Lazy[A], override val children: LazySeq[LazyTree[A]] = LazySeq.empty)
                 (override implicit val navEquals: EqualsFunction[A] = EqualsFunction.default)
	extends TreeLike[A, LazyTree[A]]
{
	// IMPLEMENTED  ---------------------------
	
	override def nav = lazyNav.value
	override def repr = this
	
	override protected def createCopy(nav: A, children: Seq[LazyTree[A]]) =
		new LazyTree[A](Lazy.initialized(nav), LazySeq.from(children))
	
	override protected def newNode(content: A) = new LazyTree[A](Lazy.initialized(content))
	
	
	// OTHER    ------------------------------
	
	/**
	  * Lazily maps the contents / navs of this tree
	  * @param f A mapping function for the nav elements of this tree
	  * @tparam B Type of mapping results
	  * @return Lazily mapped tree
	  */
	def map[B](f: A => B): LazyTree[B] = new LazyTree[B](lazyNav.map(f), children.map { _.map(f) })
}

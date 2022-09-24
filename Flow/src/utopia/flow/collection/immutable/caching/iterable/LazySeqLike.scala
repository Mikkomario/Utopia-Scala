package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.template.factory.LazyFactory
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.LazyLike

/**
  * A common trait for lazily initialized sequences of items
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
trait LazySeqLike[+A, +Coll[X]]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return A factory class used for creating more of these items
	  */
	protected def factory: LazyFactory[Coll]
	
	/**
	  * @return Lazily initialized contents within this sequence
	  */
	protected def lazyContents: IterableOnce[LazyLike[A]]
	
	
	// OTHER    -----------------------
	
	def :+[B >: A](item: LazyLike[B]): Coll[B] = factory(lazyContents.iterator :+ item)
	
	def +:[B >: A](item: LazyLike[B]): Coll[B] = factory(item +: lazyContents.iterator)
	
	def :+[B >: A](item: => B): Coll[B] = this :+ Lazy(item)
	
	def +:[B >: A](item: => B): Coll[B] = Lazy(item) +: this
	
	def lazyAppendAll[B >: A](items: IterableOnce[LazyLike[B]]) = factory(lazyContents.iterator ++ items)
	
	def lazyPrependAll[B >: A](items: IterableOnce[LazyLike[B]]) = factory(items.iterator ++ lazyContents)
}

package utopia.flow.util

import collection.{GenIterable, IterableLike, SeqLike, TraversableLike, mutable}
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.HashSet
import scala.util.{Failure, Success, Try}

/**
* This object contains some extensions for the more traditional collections / data structures
* @author Mikko Hilpinen
* @since 10.10.2018
**/
object CollectionExtensions
{
    implicit class RichSeq[A](val s: Seq[A]) extends AnyVal
    {
        /**
          * Same as apply except returns None on non-existing indices
          * @param index Target index
          * @return Value from index or None if no such index exists
          */
        def getOption(index: Int) = if (s.isDefinedAt(index)) Some(s(index)) else None
        
        /**
          * Same as apply except returns a default value on non-existing indices
          * @param index Target index
          * @param default Default value
          * @return Value from index or default value if no such index exists
          */
        def getOrElse(index: Int, default: => A) = if (s.isDefinedAt(index)) s(index) else default
    }
    
    implicit class RichSeqLike[A, Repr](val seq: SeqLike[A, Repr]) extends AnyVal
    {
        /**
         * Finds the index of the first item that matches the predicate
         * @param find a function for finding the correct item
         * @return The index of the item in this seq or None if no such item was found
         */
        def indexWhereOption(find: A => Boolean) =
        {
            val result = seq.indexWhere(find)
            if (result < 0)
                None
            else
                Some(result)
        }
    
        /**
         * Finds the index of the last item that matches the predicate
         * @param find a function for finding the correct item
         * @return The index of the item in this seq or None if no such item was found
         */
        def lastIndexWhereOption(find: A => Boolean) =
        {
            val result = seq.lastIndexWhere(find)
            if (result < 0) None else Some(result)
        }
    
        /**
          * Finds the index of the specified item
          * @param item Searched item
          * @tparam B Item type
          * @return The index of specified item or none if no such index was found
          */
        def optionIndexOf[B >: A](item: B) =
        {
            val result = seq.indexOf(item)
            if (result >= 0)
                Some(result)
            else
                None
        }
    
        /**
          * Filters a seq so that only distinct values remain. Uses a special function to determine equality
          * @param equals A function that determines whether two values are equal
          * @param cbf A canbuildfrom (implicit) to build the final collection
          * @tparam To The type of final collection
          * @return A collection with only distinct values (when considering the provided 'equals' function)
          */
        def distinctWith[To](equals: (A, A) => Boolean)(implicit cbf: CanBuildFrom[_, A, To]) =
        {
            val builder = cbf.apply()
            val collected = mutable.HashSet[A]()
            
            seq.foreach
            {
                item =>
                    if (!collected.exists { equals(_, item) })
                    {
                        collected += item
                        builder += item
                    }
            }
            
            builder.result()
        }
    
        /**
          * Filters a seq so that only distinct values remain. Compares the values by mapping them.
          * @param f A mapping function to produce comparable values
          * @param cbf A canbuildfrom (implicit) to build the final collection
          * @tparam B Map targe type
          * @tparam To Type of resulting collection
          * @return A collection with only distinct values (based on mapping)
          */
        def distinctBy[B, To](f: A => B)(implicit cbf: CanBuildFrom[_, A, To]) = distinctWith[To] { (a, b) => f(a) == f(b) }
    
        /**
          * @return A version of this seq with consecutive items paired. Each item will be present twice in the returned
          *         collection, except the first and the last item
          */
        def paired = (1 until seq.size).map { i => (seq(i - 1), seq(i)) }
    
        /**
          * This function works like foldLeft, except that it stores each step (including the start) into a seq
          * @param start The starting step
          * @param map A function for calculating the next step, takes the previous result + the next item in this seq
          * @param cbf A canbuildfrom for final collection (implicit)
          * @tparam B The type of steps
          * @tparam To The type of final collection
          * @return All of the steps mapped into a collection
          */
        def foldMapLeft[B, To](start: B)(map: (B, A) => B)(implicit cbf: CanBuildFrom[_, B, To]) =
        {
            val builder = cbf()
            var last = start
            builder += last
            
            seq.foreach
            {
                item =>
                    last = map(last, item)
                    builder += last
            }
            
            builder.result()
        }
    
        /**
         * Drops items from the right as long as the specified condition returns true
         * @param f A function that tests whether items should be dropped
         * @return A copy of this collection with rightmost items (that satisfy provided predicate) removed
         */
        def dropRightWhile(f: A => Boolean) = lastIndexWhereOption { !f(_) } match
        {
            case Some(index) => seq.take(index + 1)
            case None => seq.take(0)
        }
    
        /**
          * @param another Another sequence
          * @param equals Equality function
          * @tparam B Type of another sequence's content
          * @return Whether these two sequences are equal when using specified equality function
          */
        def compareWith[B](another: SeqLike[B, _])(equals: (A, B) => Boolean) = seq.size == another.size &&
            seq.indices.forall { i => equals(seq(i), another(i)) }
    
        /**
         * Sorts this collection based on multiple orderings (second ordering is only used if first one fails to
         * differentiate the items, then third and so on)
         * @param firstOrdering The first ordering to use
         * @param secondOrdering The second ordering to use
         * @param moreOrderings More orderings to use
         * @return A sorted copy of this collection
         */
        def sortedWith(firstOrdering: Ordering[A], secondOrdering: Ordering[A], moreOrderings: Ordering[A]*) =
            seq.sorted(new CombinedOrdering[A](Vector(firstOrdering, secondOrdering) ++ moreOrderings))
    
        /**
         * Performs a map operation until a non-empty value is returned. Returns both the mapped value and the mapped index.
         * @param f A mapping function
         * @tparam B Type of map result
         * @return The first non-empty map result, along with the index of the mapped item. None if all items were
         *         mapped to None.
         */
        def findMapAndIndex[B](f: A => Option[B]) = seq.indices.view.flatMap { i => f(seq(i))
            .map { _ -> i }  }.headOption
    
        /**
         * Maps each item + index in this sequence
         * @param f A mapping function that takes both the item and the index of that item
         * @tparam B Type of map result
         * @return Mapped data in a sequence (same order as in this sequence)
         */
        def mapWithIndex[B](f: (A, Int) => B) = seq.indices.map { i => f(seq(i), i) }
    }
    
    implicit class RichSeqLike2[A, Repr <: SeqLike[A, _]](val seq: SeqLike[A, Repr]) extends AnyVal
    {
        /**
         * Maps the first item that matches provided condition, leaves the other items as they were
         * @param find A function for finding the mapped item
         * @param map A mapping function for that item
         * @param cbf A can build from for resulting collection (implicit)
         * @return A copy of this sequence with specified item mapped. Returns this if no such item was found.
         */
        def mapFirstWhere(find: A => Boolean)(map: A => A)(implicit cbf: CanBuildFrom[_, A, Repr]) =
        {
            seq.indexWhereOption(find) match
            {
                case Some(mapIndex) =>
                    val builder = cbf()
                    builder ++= seq.take(mapIndex)
                    builder += map(seq(mapIndex))
                    builder ++= seq.drop(mapIndex + 1)
                    builder.result()
                case None => seq.repr
            }
        }
    
        /**
         * @param index Targeted index
         * @param cbf A can build from (implicit)
         * @return A copy of this sequence without specified index
         */
        def withoutIndex(index: Int)(implicit cbf: CanBuildFrom[_, A, Repr]) =
        {
            if (index < 0 || index >= seq.size)
                seq.repr
            else
            {
                val builder = cbf()
                builder ++= seq.take(index)
                builder ++= seq.drop(index + 1)
                builder.result()
            }
        }
    
        /**
         * Maps a single item in this sequence
         * @param index The index that should be mapped
         * @param f A mapping function
         * @param cbf A can build from (implicit)
         * @return A copy of this sequence with the specified index mapped
         */
        def mapIndex(index: Int)(f: A => A)(implicit cbf: CanBuildFrom[_, A, Repr]) =
        {
            if (index < 0 || index > seq.size)
                seq.repr
            else
            {
                val builder = cbf()
                builder ++= seq.take(index)
                builder += f(seq(index))
                builder ++= seq.drop(index + 1)
                builder.result()
            }
        }
    }
    
    implicit class RichOption[A](val o: Option[A]) extends AnyVal
    {
        /**
         * Converts this option to a try
         * @param generateFailure A function for generating a throwable for a failure if one is needed
         * @return Success with this option's value or failure if this option was empty
         */
        def toTry(generateFailure: => Throwable) = o match
        {
            case Some(v) => Success(v)
            case None => Failure(generateFailure)
        }
    }
    
    implicit class RichTry[T](val t: Try[T]) extends AnyVal
    {
        /**
         * The success value of this try. None if this try was a failure
         */
        def success = t.toOption
        /**
         * The failure (throwable) value of this try. None if this try was a success.
         */
        def failure = t.failed.toOption
    }
    
    implicit class RichEither[L, R](val e: Either[L, R]) extends AnyVal
    {
        /**
          * @return This either's left value or None if this either is right
          */
        def leftOption = e match
        {
            case Left(l) => Some(l)
            case Right(_) => None
        }
    
        /**
          * @return This either's right value or None if this either is left (same as toOption)
          */
        def rightOption = e.toOption
    
        /**
          * If this either is left, maps it
          * @param f A mapping function for left side
          * @tparam B New type for left side
          * @return A mapped version of this either
          */
        def mapLeft[B](f: L => B) = e match
        {
            case Right(r) => Right(r)
            case Left(l) => Left(f(l))
        }
    
        /**
          * If this either is right, maps it
          * @param f A mapping function for left side
          * @tparam B New type for right side
          * @return A mapped version of this either
          */
        def mapRight[B](f: R => B) = e match
        {
            case Right(r) => Right(f(r))
            case Left(l) => Left(l)
        }
    
        /**
         * Maps the value of this either to a single value, whichever side this is
         * @param leftMap Mapping function used when left value is present
         * @param rightMap Mapping function used when right value is present
         * @tparam B Resulting item type
         * @return Mapped left or mapped right
         */
        def mapToSingle[B](leftMap: L => B)(rightMap: R => B) = e match
        {
            case Right(r) => rightMap(r)
            case Left(l) => leftMap(l)
        }
    
        /**
         * Maps this either, no matter which side it is
         * @param leftMap Mapping function used when this either is left
         * @param rightMap Mapping function used when this either is right
         * @tparam L2 New left type
         * @tparam R2 New right type
         * @return A mapped version of this either (will have same side)
         */
        def mapBoth[L2, R2](leftMap: L => L2)(rightMap: R => R2) = e match
        {
            case Right(r) => Right(rightMap(r))
            case Left(l) => Left(leftMap(l))
        }
    }
    
    implicit class RichTraversable[A](val t: Traversable[A]) extends AnyVal
    {
        /**
         * @return Duplicate items within this traversable
         */
        def duplicates: Set[A] =
        {
            var foundResults = HashSet[A]()
            var checked = HashSet[A]()
            t.foreach { item => if (checked.contains(item)) foundResults += item else checked += item }
            foundResults
        }
        
        /**
          * Maps items until a concrete result is found, then returns that result
          * @param map A mapping function that maps to either Some or None
          * @tparam B The map target type
          * @return The first item that was mapped to Some. None if all items were mapped to None.
          */
        def findMap[B](map: A => Option[B]) = t.view.map(map).find { _.isDefined }.flatten
    
        /**
          * Finds the maximum value in this traversable
          * @param cmp Ordering (implicit)
          * @tparam B Ordering type
          * @return Maximum item or None if this traversable was empty
          */
        def maxOption[B >: A](implicit cmp: Ordering[B]): Option[A] =
        {
            if (t.isEmpty)
                None
            else
                Some(t.max(cmp))
        }
        
        /**
          * Finds the minimum value in this traversable
          * @param cmp Ordering (implicit)
          * @tparam B Ordering type
          * @return Minimum item or None if this traversable was empty
          */
        def minOption[B >: A](implicit cmp: Ordering[B]): Option[A] =
        {
            if (t.isEmpty)
                None
            else
                Some(t.min(cmp))
        }
    
        /**
         * Finds the maximum value based on map result
         * @param map A mapping function
         * @param cmp Implicit ordering
         * @tparam B Type of map result
         * @return Maximum item based on map result. None if this traversable was empty
         */
        def maxByOption[B](map: A => B)(implicit cmp: Ordering[B]): Option[A] =
        {
            if (t.isEmpty)
                None
            else
                Some(t.maxBy(map))
        }
    
        /**
         * Finds the minimum value based on map result
         * @param map A mapping function
         * @param cmp Implicit ordering
         * @tparam B Type of map result
         * @return Minimum item based on map result. None if this traversable was empty
         */
        def minByOption[B](map: A => B)(implicit cmp: Ordering[B]): Option[A] =
        {
            if (t.isEmpty)
                None
            else
                Some(t.minBy(map))
        }
    
        /**
          * Finds the item(s) that best match the specified conditions
          * @param matchers Search conditions used. The conditions that are introduced first are considered more
          *                 important than those which are introduced the last.
          * @param cbf A builder for the final result (implicit)
          * @tparam To Target collection type
          * @return The items in this collection that best match the specified conditions
          */
        def bestMatch[To](matchers: Seq[A => Boolean])(implicit cbf: CanBuildFrom[_, A, To]): To =
        {
            // If there is only a single option, that is the best match. If there are 0 options, there's no best match
            // If there are no matchers left, cannot make a distinction between items
            if (t.size < 2 || matchers.isEmpty)
            {
                val buffer = cbf()
                buffer ++= t
                buffer.result()
            }
            else
            {
                val nextMatcher = matchers.head
                val matched = t.filter(nextMatcher.apply)
                
                // If matcher found some results, limits to those. if not, cannot use that group
                if (matched.nonEmpty)
                    matched.bestMatch(matchers.drop(1))
                else
                    bestMatch(matchers.drop(1))
            }
        }
    
        /**
         * Maps the contents of this traversable. Mapping may fail, interrupting all remaining mappings
         * @param f A mapping function. May fail.
         * @param cbf Implicit can build from for final collection
         * @tparam B Type of map result
         * @tparam To Type of final collection
         * @return Mapped collection if all mappings succeeded. Failure otherwise.
         */
        def tryMap[B, To](f: A => Try[B])(implicit cbf: CanBuildFrom[_, B, To]): Try[To] =
        {
            val buffer = cbf()
            // Maps items until the mapping function fails
            t.view.map { a =>
                val result = f(a)
                result.toOption.foreach { buffer += _ }
                result
            }.find { _.isFailure } match
            {
                case Some(failure) => Failure(failure.failure.get)
                // On success (no failure found), returns all mapped items
                case None => Success(buffer.result())
            }
        }
    
        /**
          * Compares this set of items with another set. Lists items that have been added and removed, plus the changes
          * between items that have stayed
          * @param another Another traversable
          * @param connectBy A function for providing the unique key based on which items are connected
          *                  (should be unique within each collection). Items sharing this key are connected.
          * @param merge A function for merging two connected items. Takes connection key, item in this collection and
          *              item in the other collection
          * @tparam B Type of items in the other collection
          * @tparam K Type of match key used
          * @tparam Merge Merge function for merging connected items
          * @return 1) Items only present in this collection, 2) Merged items shared between these two collections,
          *         3) Items only present in the other collection
          */
        def listChanges[B >: A, K, Merge](another: Traversable[B])(connectBy: B => K)(merge: (K, A, B) => Merge) =
        {
            val meByKey = t.map { a => connectBy(a) -> a }.toMap
            val theyByKey = another.map { a => connectBy(a) -> a }.toMap
            
            val myKeys = meByKey.keySet
            val theirKeys = theyByKey.keySet
            
            val onlyInMe = (myKeys -- theirKeys).toVector.map { meByKey(_) }
            val onlyInThem = (theirKeys -- myKeys).toVector.map { theyByKey(_) }
            val merged = (myKeys & theirKeys).toVector.map { key => merge(key, meByKey(key), theyByKey(key)) }
            
            (onlyInMe, merged, onlyInThem)
        }
    }
    
    implicit class RichTraversableLike[A, Repr](val t: TraversableLike[A, Repr]) extends AnyVal
    {
        /**
          * Divides the items in this traversable into two groups, based on boolean result
          * @param f A function that separates the items
          * @param cbf An implicit can build from
          * @return The 'false' group, followed by the 'true' group
          */
        def divideBy(f: A => Boolean)(implicit cbf: CanBuildFrom[_, A, Repr]) =
        {
            val groups = t.groupBy(f)
            groups.getOrElse(false, cbf.apply().result()) -> groups.getOrElse(true, cbf.apply().result())
        }
    }
    
    implicit class RichIterableLike[A, Repr](val t: IterableLike[A, Repr]) extends AnyVal
    {
        /**
          * Iterates through the items in this iterable along with another iterable's items. Will stop when either
          * iterable runs out of items
          * @param another Another iterable
          * @param f A function that handles the items
          * @param bf A can build from (implicit)
          * @tparam B The type of another iterable's items
          * @tparam U Arbitrary result type
          */
        def foreachWith[B, U](another: GenIterable[B])(f: (A, B) => U)
                                      (implicit bf: CanBuildFrom[Repr, (A, B), Traversable[(A, B)]]) =
            t.zip(another).foreach { p => f(p._1, p._2) }
    
        /**
         * Performs an operation for each item in this collection. Stops if an operation fails.
         * @param f A function that takes an item and performs an operation that may fail
         * @return Failure if any of the operations failed, success otherwise.
         */
        def tryForEach(f: A => Try[Any]): Try[Any] = t.view.map(f).find { _.isFailure }.getOrElse(Success(Unit))
        
        /**
          * @return An iterator that keeps repeating over and over (iterator continues infinitely or until this
          *         collection is empty)
          */
        def repeatingIterator(): Iterator[A] = new RepeatingIterator[A](t)
    }
    
    implicit class RichMap[K, V](val m: Map[K, V]) extends AnyVal
    {
        /**
          * Merges this map with another map. If value is present only in one map, it is preserved as is.
          * @param another Another map
          * @param merge A merge function used when both maps contain a value
          * @tparam V2 The resulting value type
          * @return A map with merged values
          */
        def mergedWith[V2 >: V](another: Map[K, V2], merge: (V, V2) => V2) =
        {
            val myKeys = m.keySet
            val theirKeys = another.keySet
            val onlyInMe = myKeys.diff(theirKeys)
            val onlyInThem = theirKeys.diff(myKeys)
            val inBoth = myKeys.intersect(theirKeys)
            
            val myPart = onlyInMe.map { k => k -> m(k) }.toMap
            val theirPart = onlyInThem.map { k => k -> another(k) }.toMap
            val ourPart = inBoth.map { k => k -> merge(m(k), another(k)) }.toMap
            
            myPart ++ theirPart ++ ourPart
        }
    }
    
    /**
     * This implicit class is used for extending collection map conversion
     */
    implicit class MultiMapConvertible[T](val list: TraversableOnce[T]) extends AnyVal
    {
        // Referenced from: https://stackoverflow.com/questions/22090371/scala-grouping-list-of-tuples [10.10.2018]
        def toMultiMap[Key, Value, Values](key: T => Key, value: T => Value)
                (implicit cbfv: CanBuildFrom[Nothing, Value, Values]): Map[Key, Values] = 
        {
            val b = mutable.Map.empty[Key, mutable.Builder[Value, Values]]
	        list.foreach { elem => b.getOrElseUpdate(key(elem), cbfv()) += value(elem) }
	        b.map { case (k, vb) => (k, vb.result()) } (collection.breakOut)
        }
    }
    
    /**
     * This extension allows tuple lists to be transformed into multi maps directly
     */
    implicit class RichTupleList[K, V](val list: TraversableOnce[(K, V)]) extends AnyVal
    {
        def toMultiMap[Values]()(implicit cbfv: CanBuildFrom[Nothing, V, Values]): Map[K, Values] = 
        {
            new MultiMapConvertible(list).toMultiMap(_._1, _._2)
        }
    }
    
    private class RepeatingIterator[A](val c: IterableLike[A, _]) extends Iterator[A]
    {
        // ATTRIBUTES   -----------------
        
        private var currentIterator: Option[Iterator[A]] = None
        
        
        // IMPLEMENTED  -----------------
        
        override def hasNext = iterator().hasNext
    
        override def next() = iterator().next()
        
        
        // OTHER    -------------------
        
        private def iterator() =
        {
            if (currentIterator.forall { !_.hasNext } )
                currentIterator = Some(c.iterator)
    
            currentIterator.get
        }
    }
}
package utopia.flow.util

import utopia.flow.collection.{GroupIterator, LazyVector, LimitedLengthIterator, PairingIterator, PollingIterator, TerminatingIterator}
import utopia.flow.datastructure.immutable.{Lazy, Pair}
import utopia.flow.datastructure.mutable.PollableOnce

import scala.language.implicitConversions
import collection.{AbstractIterator, AbstractView, BuildFrom, Factory, IterableOps, SeqOps, mutable}
import scala.collection.generic.{IsIterable, IsIterableOnce, IsSeq}
import scala.collection.immutable.{HashSet, VectorBuilder}
import scala.util.{Failure, Success, Try}

/**
  * This object contains some extensions for the more traditional collections / data structures
  * @author Mikko Hilpinen
  * @since 10.10.2018
  **/
object CollectionExtensions
{
	// ITERABLE ONCE    ---------------------------------------
	
	class IterableOnceOperations[Repr, I <: IsIterableOnce[Repr]](coll: Repr, iter: I)
	{
		/**
		  * Filters this collection so that only distinct values remain. Uses a special function to determine equality
		  * @param equals A function that determines whether two values are equal
		  * @param buildFrom Builder for the new collection
		  * @return A collection with only distinct values (when considering the provided 'equals' function)
		  */
		def distinctWith(equals: (iter.A, iter.A) => Boolean)(implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr =
		{
			val iterOps = iter(coll)
			val builder = buildFrom.newBuilder(coll)
			val collected = mutable.HashSet[iter.A]()
			
			iterOps.iterator.foreach { item =>
				if (!collected.exists { e => equals(e, item) })
				{
					builder += item
					collected += item
				}
			}
			
			builder.result()
		}
		
		/**
		  * Filters this collection so that only distinct values remain. Compares the values by mapping them.
		  * @param f A mapping function to produce comparable values
		  * @param buildFrom A builder (implicit) to build the final collection
		  * @tparam B Map target type
		  * @return A collection with only distinct values (based on mapping)
		  */
		def distinctBy[B](f: iter.A => B)(implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr =
			distinctWith { (a, b) => f(a) == f(b) }
		
		/**
		  * This function works like foldLeft, except that it stores each step (including the start) into a seq
		  * @param start The starting step
		  * @param map A function for calculating the next step, takes the previous result + the next item in this seq
		  * @param buildFrom A buildfrom for final collection (implicit)
		  * @tparam B The type of steps
		  * @tparam That The type of final collection
		  * @return All of the steps mapped into a collection
		  */
		def foldMapLeft[B, That](start: B)(map: (B, iter.A) => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That =
		{
			val builder = buildFrom.newBuilder(coll)
			var last = start
			builder += last
			
			val iterOps = iter(coll)
			iterOps.iterator.foreach { item =>
				last = map(last, item)
				builder += last
			}
			
			builder.result()
		}
		
		// Referenced from: https://stackoverflow.com/questions/22090371/scala-grouping-list-of-tuples [10.10.2018]
		/**
		  * Converts this iterable item to a map with possibly multiple values per key
		  * @param toKey A function for mapping items to keys
		  * @param toValue A function for mapping items to values
		  * @param bf Implicit build from for the final values collections
		  * @tparam Key Type of key in the final map
		  * @tparam Value Type of individual values in the final map
		  * @tparam Values Type of values collections in the final map
		  * @return A multi map based on this iteration mapping
		  */
		def toMultiMap[Key, Value, Values](toKey: iter.A => Key)(toValue: iter.A => Value)(
			implicit bf: BuildFrom[Repr, Value, Values]): Map[Key, Values] = toMultiMap { a => toKey(a) -> toValue(a) }
		
		/**
		  * Converts this iterable item to a map with possibly multiple values per key
		  * @param f A function for mapping items to key value pairs
		  * @param bf Implicit build from for the final values collections
		  * @tparam Key Type of key in the final map
		  * @tparam Value Type of individual values in the final map
		  * @tparam Values Type of values collections in the final map
		  * @return A multi map based on this iteration mapping
		  */
		def toMultiMap[Key, Value, Values](f: iter.A => (Key, Value))(implicit bf: BuildFrom[Repr, Value, Values]): Map[Key, Values] =
		{
			val buffer = mutable.Map.empty[Key, mutable.Builder[Value, Values]]
			val iterOps = iter(coll)
			iterOps.iterator.foreach { item =>
				val (key, value) = f(item)
				buffer.getOrElseUpdate(key, bf.newBuilder(coll)) += value
			}
			buffer.view.mapValues { _.result() }.toMap
		}
		
		/**
		  * Performs an operation for each item in this collection. Stops if an operation fails.
		  * @param f A function that takes an item and performs an operation that may fail
		  * @return Failure if any of the operations failed, success otherwise.
		  */
		def tryForeach[U](f: iter.A => Try[U]): Try[Unit] =
		{
			val iterOps = iter(coll)
			iterOps.iterator.map(f).find { _.isFailure } match {
				case Some(failure) => failure.map { _ => () }
				case None => Success(())
			}
		}
		/**
		  * Maps the contents of this collection. Mapping may fail, interrupting all remaining mappings
		  * @param f A mapping function. May fail.
		  * @param bf A build from for the final collection (implicit)
		  * @tparam B Type of map result
		  * @tparam To Type of final collection
		  * @return Mapped collection if all mappings succeeded. Failure otherwise.
		  */
		def tryMap[B, To](f: iter.A => Try[B])(implicit bf: BuildFrom[Repr, B, To]): Try[To] =
		{
			val buffer = bf.newBuilder(coll)
			// Maps items until the mapping function fails
			tryForeach { f(_).map { buffer += _ } }.map { _ => buffer.result() }
		}
		/**
		  * FlatMaps the contents of this collection. Mapping may fail, however, cancelling all remaining mappings
		  * @param f A mapping function. May fail.
		  * @param bf A build from for the final collection (implicit)
		  * @tparam B Type of individual map result item
		  * @tparam To Type of final collection
		  * @return Flat mapped collection if all mappings succeeded. Failure otherwise.
		  */
		def tryFlatMap[B, To](f: iter.A => Try[IterableOnce[B]])(implicit bf: BuildFrom[Repr, B, To]): Try[To] =
		{
			val buffer = bf.newBuilder(coll)
			tryForeach { f(_).map { buffer ++= _ } }.map { _ => buffer.result() }
		}
		
		/**
		  * Takes elements from this collection until the specified condition is met. If found, includes the item
		  * on which that condition was met.
		  * @param endCondition A condition for ending this take operation (returns true on the last item to collect)
		  * @param buildFrom Implicit build from
		  * @tparam That Target collection type
		  * @return All elements of this collection until the first item that matches the specified condition +
		  *         the matching item itself. Contains all items of this collection if the condition was never met.
		  */
		def takeTo[That](endCondition: iter.A => Boolean)(implicit buildFrom: BuildFrom[Repr, iter.A, That]): That =
		{
			val iterOps = iter(coll)
			buildFrom.fromSpecific(coll)(TerminatingIterator(iterOps.iterator)(endCondition))
		}
		
		/**
		  * Divides the items in this collection into two groups, based on boolean result
		  * @param f A function that separates the items
		  * @param bf an implicit buildFrom for the resulting collection type
		  * @tparam To type of the resulting collection
		  * @return The 'false' group, followed by the 'true' group
		  */
		def divideBy[To](f: iter.A => Boolean)(implicit bf: BuildFrom[Repr, iter.A, To]) =
		{
			val falseBuilder = bf.newBuilder(coll)
			val trueBuilder = bf.newBuilder(coll)
			
			iter(coll).iterator.foreach { a => if (f(a)) trueBuilder += a else falseBuilder += a }
			
			falseBuilder.result() -> trueBuilder.result()
		}
	}
	
	implicit def iterableOnceOperations[Repr](coll: Repr)(implicit iter: IsIterableOnce[Repr]): IterableOnceOperations[Repr, iter.type] =
		new IterableOnceOperations(coll, iter)
	
	implicit class RichIterableOnce[A](val i: IterableOnce[A]) extends AnyVal
	{
		/**
		  * @return Whether all items within this collection are considered equal (comparing with ==)
		  */
		def areAllEqual = {
			val iter = i.iterator
			if (iter.hasNext) {
				val first = iter.next()
				iter.forall { _ == first }
			}
			else
				true
		}
		
		/**
		  * Checks whether there exists at least 'requiredCount' items in this collection where the specified
		  * condition 'f' returns true. Compared to .count -function, this function is more optimized since it stops
		  * counting once the required amount has been reached.
		  * @param requiredCount The required amount of matches before returning true
		  * @param f A test function
		  * @return Whether 'requiredCount' number of items were found where the specified function 'f' returned true
		  */
		def existsCount(requiredCount: Int)(f: A => Boolean) =
		{
			val iter = i.iterator
			var currentCount = 0
			while (iter.hasNext && currentCount < requiredCount)
			{
				if (f(iter.next()))
					currentCount += 1
			}
			currentCount == requiredCount
		}
		
		/**
		  * Maps items until a concrete result is found, then returns that result
		  * @param map A mapping function that maps to either Some or None
		  * @tparam B The map target type
		  * @return The first item that was mapped to Some. None if all items were mapped to None.
		  */
		def findMap[B](map: A => Option[B]) = i.iterator.map(map).find { _.isDefined }.flatten
		
		/**
		  * Lazily maps the contents of this iterable entity.
		  * @param f A mapping function
		  * @tparam B Mapping result type
		  * @return A lazily initialized collection containing the mapping results
		  */
		def lazyMap[B](f: A => B) = new LazyVector[B](i.iterator.map { a => Lazy { f(a) } }.toSeq)
		
		/**
		  * Divides / maps the items in this collection to two groups
		  * @param f A function for separating / mapping the items
		  * @tparam L Type of left group items
		  * @tparam R Type of right group items
		  * @return Left group and right group
		  */
		def divideWith[L, R](f: A => Either[L, R]) =
		{
			val lBuilder = new VectorBuilder[L]()
			val rBuilder = new VectorBuilder[R]()
			i.iterator.map(f).foreach {
				case Left(l) => lBuilder += l
				case Right(r) => rBuilder += r
			}
			lBuilder.result() -> rBuilder.result()
		}
		/**
		  * Divides / maps the items in this collection to two groups
		  * @param f A function for separating / mapping the items
		  * @tparam L Type of left group items
		  * @tparam R Type of right group items
		  * @return Left group and right group
		  */
		@deprecated("Please use .divideWith(...) instead", "v1.4.1")
		def dividedWith[L, R](f: A => Either[L, R]) = divideWith[L, R](f)
		/**
		  * Divides the contents of this collection into two groups. Each item may represent 0-n items in the
		  * resulting group(s)
		  * @param f A function that accepts an item in this collection and returns 0-n grouped items
		  *          (Left(x) for a left group item x and Right(y) for a right group item y)
		  * @tparam L Type of left group items
		  * @tparam R Type of right group items
		  * @return Collected left group items and collected right group items as two separate vectors
		  */
		def flatDivideWith[L, R](f: A => IterableOnce[Either[L, R]]) =
		{
			val lBuilder = new VectorBuilder[L]()
			val rBuilder = new VectorBuilder[R]()
			i.iterator.flatMap(f).foreach {
				case Left(l) => lBuilder += l
				case Right(r) => rBuilder += r
			}
			lBuilder.result() -> rBuilder.result()
		}
		
		/**
		  * Maps the items in this collection into two different collections
		  * @param f A mapping function that produces two results (left -> right) for each item
		  * @tparam L Type of left result item
		  * @tparam R Type of right result item
		  * @return Left results -> right results
		  */
		def splitMap[L, R](f: A => (L, R)) =
		{
			val lBuilder = new VectorBuilder[L]
			val rBuilder = new VectorBuilder[R]
			i.iterator.map(f).foreach { case (l, r) =>
				lBuilder += l
				rBuilder += r
			}
			lBuilder.result() -> rBuilder.result()
		}
		/**
		  * Maps items in this collection into two groups, where an item maps to
		  * x left group items and y right group items
		  * @param f A function that accepts an item in this collection and returns a tuple of collections where the
		  *          first item is collection of left side map results and the second item is a collection of
		  *          right side map results
		  * @tparam L Type of left side map results
		  * @tparam R Type of right side map results
		  * @return Left side map results, collected together + right side map results, collected together (tuple)
		  */
		def splitFlatMap[L, R](f: A => (IterableOnce[L], IterableOnce[R])) =
		{
			val lBuilder = new VectorBuilder[L]()
			val rBuilder = new VectorBuilder[R]()
			i.iterator.map(f).foreach { case (lefts, rights) =>
				lBuilder ++= lefts
				rBuilder ++= rights
			}
			lBuilder.result() -> rBuilder.result()
		}
	}
	
	implicit class TriesIterableOnce[A](val tries: IterableOnce[Try[A]]) extends AnyVal
	{
		/**
		  * Converts this series of attempts to a single try. The resulting try succeeds only if all attempts succeeded.
		  * If a failure is encountered, iteration is immediately ended and that failure is returned.
		  * @return Success containing all success results or Failure containing the encountered error
		  */
		def toTry = {
			val successesBuilder = new VectorBuilder[A]()
			val iter = tries.iterator
			var failure: Option[Throwable] = None
			while (failure.isEmpty && iter.hasNext) {
				iter.next() match {
					case Success(item) => successesBuilder += item
					case Failure(error) => failure = Some(error)
				}
			}
			failure match {
				case Some(error) => Failure(error)
				case None => Success(successesBuilder.result())
			}
		}
		/**
		  * @return Failure if all attempts in this collection failed, containing the first encountered error.
		  *         If one or more attempts succeeded, or if no attempts were made, returns a success containing
		  *         caught errors, as well as successes
		  */
		def toTryCatch = {
			val (failures, successes) = tries.divided
			if (successes.isEmpty) {
				failures.headOption match {
					// Case: All attempts failed => fails with the first encountered error
					case Some(firstError) => Failure(firstError)
					// Case: No attempts were made => empty success
					case None => Success(failures -> successes)
				}
			}
			// Case: One or more attempts succeeded => success
			else
				Success(failures -> successes)
		}
		
		/**
		  * @return An iterator that only includes failed attempts
		  */
		def failuresIterator = tries.iterator.flatMap { _.failure }
		/**
		  * @return The first failure that was encountered. None if no failures were encountered.
		  */
		def anyFailure = tries.iterator.findMap { _.failure }
	}
	
	implicit class PairsIterableOnce[A](val pairs: IterableOnce[Pair[A]]) extends AnyVal
	{
		/**
		  * @return A map based on this collection (first items as keys, second items as values)
		  */
		def toMap: Map[A, A] = pairs.iterator.map { _.toTuple }.toMap
	}
	
	
	// ITERABLE ----------------------------------------
	
	class IterableOperations[Repr, I <: IsIterable[Repr]](coll: Repr, iter: I)
	{
		/**
		  * Finds the item or items with the maximum value, based on a mapping function. Works like maxBy, except that
		  * multiple values are returned in cases where items map to the same (maximum) value.
		  * @param f A function that maps items to comparable values
		  * @param bf An implicit buildfrom for the resulting collection
		  * @param ord Implicit ordering for the mapped values
		  * @tparam B Compared type (map result type)
		  * @tparam To Type of the resulting collection
		  * @return A version of this collection that only contains the items that mapped to the largest available value
		  */
		def maxGroupBy[B, To](f: iter.A => B)(implicit bf: BuildFrom[Repr, iter.A, To], ord: Ordering[B]): To = {
			val iterOps = iter(coll)
			if (iterOps.isEmpty)
				bf.fromSpecific(coll)(Iterator.empty)
			else {
				val pairs = iterOps.iterator.map { a => a -> f(a) }.toVector
				val maxValue = pairs.map { _._2 }.max
				bf.fromSpecific(coll)(pairs.iterator.filter { _._2 == maxValue }.map { _._1 })
			}
		}
		/**
		  * Finds the item or items with the minimum value, based on a mapping function. Works like minBy, except that
		  * multiple values are returned in cases where items map to the same (minimum) value.
		  * @param f A function that maps items to comparable values
		  * @param bf An implicit buildfrom for the resulting collection
		  * @param ord Implicit ordering for the mapped values
		  * @tparam B Compared type (map result type)
		  * @tparam To Type of the resulting collection
		  * @return A version of this collection that only contains the items that mapped to the smallest available value
		  */
		def minGroupBy[B, To](f: iter.A => B)(implicit bf: BuildFrom[Repr, iter.A, To], ord: Ordering[B]) =
			maxGroupBy(f)(bf, ord.reverse)
	}
	
	implicit def iterableOperations[Repr](coll: Repr)
	                                     (implicit iter: IsIterable[Repr]): IterableOperations[Repr, iter.type] =
		new IterableOperations(coll, iter)
	
	implicit class RichIterableLike[A, CC[X], Repr](val t: IterableOps[A, CC, Repr]) extends AnyVal
	{
		/**
		  * Performs an operation for each item in this collection. Stops if an operation fails.
		  * @param f A function that takes an item and performs an operation that may fail
		  * @return Failure if any of the operations failed, success otherwise.
		  */
		@deprecated("Please use .tryForeach(...) instead", "v1.12")
		def tryForEach(f: A => Try[Any]): Try[Any] = t.view.map(f).find { _.isFailure }.getOrElse(Success[Unit](()))
		
		/**
		  * @return An iterator that keeps repeating over and over (iterator continues infinitely or until this
		  *         collection is empty)
		  */
		def repeatingIterator(): Iterator[A] = new RepeatingIterator[A, CC](t)
	}
	
	implicit class RichIterableLike2[A, CC[X] <: IterableOps[X, CC, CC[X]], Repr](val t: IterableOps[A, CC, Repr]) extends AnyVal
	{
		/**
		  * Iterates through the items in this iterable along with another iterable's items. Will stop when either
		  * iterable runs out of items
		  * @param another Another iterable
		  * @param f A function that handles the items
		  * @tparam B The type of another iterable's items
		  * @tparam U Arbitrary result type
		  */
		def foreachWith[B, U](another: Iterable[B])(f: (A, B) => U) = t.zip(another).foreach { p => f(p._1, p._2) }
	}
	
	implicit class RichIterable[A](val t: Iterable[A]) extends AnyVal
	{
		/**
		  * @return Duplicate items within this Iterable
		  */
		def duplicates: Set[A] =
		{
			var foundResults = HashSet[A]()
			var checked = HashSet[A]()
			t.foreach { item => if (checked.contains(item)) foundResults += item else checked += item }
			foundResults
		}
		
		/**
		  * Finds the maximum value in this Iterable
		  * @param cmp Ordering (implicit)
		  * @tparam B Ordering type
		  * @return Maximum item or None if this Iterable was empty
		  */
		def maxOption[B >: A](implicit cmp: Ordering[B]): Option[A] =
		{
			if (t.isEmpty)
				None
			else
				Some(t.max(cmp))
		}
		
		/**
		  * Finds the minimum value in this Iterable
		  * @param cmp Ordering (implicit)
		  * @tparam B Ordering type
		  * @return Minimum item or None if this Iterable was empty
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
		  * @return Maximum item based on map result. None if this Iterable was empty
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
		  * @return Minimum item based on map result. None if this Iterable was empty
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
		  * @tparam That Target collection type
		  * @return The items in this collection that best match the specified conditions
		  */
		def bestMatch[That](matchers: Seq[A => Boolean]): Vector[A] =
		{
			// If there is only a single option, that is the best match. If there are 0 options, there's no best match
			// If there are no matchers left, cannot make a distinction between items
			if (t.size < 2 || matchers.isEmpty)
				t.toVector
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
		  * Compares this set of items with another set. Lists items that have been added and removed, plus the changes
		  * between items that have stayed
		  * @param another Another Iterable
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
		def listChanges[B >: A, K, Merge](another: Iterable[B])(connectBy: B => K)(merge: (K, A, B) => Merge) =
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
		
		/**
		  * Performs the specified mapping function until it succeeds or until all items in this collection have been
		  * tested
		  * @param f A mapping function which may fail
		  * @tparam B Type of map function result
		  * @return The first successful map result or failure if none of the items in this collection could be mapped
		  */
		def tryFindMap[B](f: A => Try[B]) =
		{
			val iter = t.iterator.map(f)
			if (iter.hasNext)
			{
				// Returns the first result if its a success or if no successes were found
				val firstResult = iter.next()
				if (firstResult.isSuccess)
					firstResult
				else
					iter.find { _.isSuccess }.getOrElse(firstResult)
			}
			else
				Failure(new NoSuchElementException("Called tryFindMap on an empty collection"))
		}
		
		/**
		  * @param items Items to test
		  * @tparam O Type of items collection
		  * @return Whether this collection of items contains all of the specified items
		  */
		def containsAll[O >: A](items: Iterable[O]) = items.forall { item => t.exists { _ == item } }
	}
	
	
	// SEQ  --------------------------------------------
	
	class SeqOperations[Repr, S <: IsSeq[Repr]](coll: Repr, seq: S)
	{
		/**
		  * Maps a single item in this sequence
		  * @param index The index that should be mapped
		  * @param f A mapping function
		  * @param buildFrom A can build from (implicit)
		  * @return A copy of this sequence with the specified index mapped
		  */
		def mapIndex[B >: seq.A, That](index: Int)(f: seq.A => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That =
		{
			val seqOps = seq(coll)
			buildFrom.fromSpecific(coll)(new AbstractView[B] {
				override def iterator: AbstractIterator[B] = new AbstractIterator[B]
				{
					val it = seqOps.iterator
					var nextIndex = 0
					
					override def hasNext = it.hasNext
					
					// Passes items from the original iterator, except at specified index
					override def next() =
					{
						val result =
						{
							if (nextIndex == index)
								f(it.next())
							else
								it.next()
						}
						nextIndex += 1
						result
					}
				}
			})
		}
		
		/**
		  * Maps the first item that matches provided condition, leaves the other items as they were
		  * @param find A function for finding the mapped item
		  * @param map A mapping function for that item
		  * @param buildFrom A can build from for resulting collection (implicit)
		  * @return A copy of this sequence with specified item mapped. Returns this if no such item was found.
		  */
		def mapFirstWhere(find: seq.A => Boolean)(map: seq.A => seq.A)(implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): Repr =
		{
			seq(coll).indexWhereOption(find) match
			{
				case Some(index) => mapIndex(index)(map)
				case None => coll
			}
		}
		
		/**
		  * @param index Targeted index
		  * @param buildFrom A build from (implicit)
		  * @return A copy of this sequence without specified index
		  */
		def withoutIndex(index: Int)(implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): Repr =
		{
			if (index < 0)
				coll
			else
			{
				val seqOps = seq(coll)
				if (index >= seqOps.size)
					coll
				else
					buildFrom.fromSpecific(coll)(seqOps.iterator.take(index) ++ seqOps.iterator.drop(index + 1))
			}
		}
		
		/**
		  * Splits this collection into a number of smaller pieces. Preserves order.
		  * @param maxLength Maximum length of each segment
		  * @param buildFrom A build from (implicit)
		  * @return This sequence split into possibly larger number of smaller sequences
		  */
		def splitToSegments(maxLength: Int)(implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): Vector[Repr] = {
			val seqOps = seq(coll)
			if (seqOps.size <= maxLength)
				Vector(buildFrom.fromSpecific(coll)(seqOps))
			else
			{
				val factory = buildFrom.toFactory(coll)
				val builder = new VectorBuilder[Repr]
				val iter = seqOps.iterator
				while (iter.hasNext)
				{
					val segmentBuilder = factory.newBuilder
					iter.forNext(maxLength) { segmentBuilder += _ }
					builder += segmentBuilder.result()
				}
				
				builder.result()
			}
		}
		
		/**
		  * Performs specified operation for each item in this sequence. Called function will also receive item index
		  * in this sequence
		  * @param f A function called for each item
		  * @tparam U Arbitrary result type
		  */
		def foreachWithIndex[U](f: (seq.A, Int) => U) =
		{
			val seqOps = seq(coll)
			seqOps.zipWithIndex.foreach { case(item, index) => f(item, index) }
		}
		
		/**
		  * Takes items from right to left as long as the specified condition holds
		  * @param f A function that determines whether an item is accepted to the final collection
		  * @param buildFrom A build from (implicit)
		  * @tparam That Resulting collection type
		  * @return A collection that contains the collected items in the same order as they appear in this
		  *         collection (left to right)
		  */
		def takeRightWhile[That](f: seq.A => Boolean)(implicit buildFrom: BuildFrom[Repr, seq.A, That]): That =
		{
			val seqOps = seq(coll)
			// Collects the items to a buffer first in order to reverse the order afterwards
			val bufferBuilder = new VectorBuilder[seq.A]()
			seqOps.reverseIterator.takeWhile(f).foreach { bufferBuilder += _ }
			buildFrom.fromSpecific(coll)(bufferBuilder.result().reverse)
		}
	}
	
	implicit def seqOperations[Repr](coll: Repr)(implicit seq: IsSeq[Repr]): SeqOperations[Repr, seq.type] =
		new SeqOperations(coll, seq)
	
	implicit class RichSeqLike[A, CC[X], Repr](val seq: SeqOps[A, CC, Repr]) extends AnyVal
	{
		/**
		  * Same as apply except returns None on non-existing indices
		  * @param index Target index
		  * @return Value from index or None if no such index exists
		  */
		def getOption(index: Int) = if (seq.isDefinedAt(index)) Some(seq(index)) else None
		
		/**
		  * Same as apply except returns a default value on non-existing indices
		  * @param index Target index
		  * @param default Default value
		  * @return Value from index or default value if no such index exists
		  */
		def getOrElse(index: Int, default: => A) = if (seq.isDefinedAt(index)) seq(index) else default
		
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
		  * @param f A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the largest value when mapped
		  * @throws NoSuchElementException If this sequence is empty
		  */
		@throws[NoSuchElementException]("Throws when called for an empty sequence")
		def maxIndexBy[B](f: A => B)(implicit order: Ordering[B]) =
		{
			var maxIndex = 0
			var maxResult = f(seq.head)
			seq.indices.drop(1).foreach { index =>
				val result = f(seq(index))
				if (order.compare(result, maxResult) > 0)
				{
					maxIndex = index
					maxResult = result
				}
			}
			maxIndex
		}
		
		/**
		  * @param f A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the smallest value when mapped
		  * @throws NoSuchElementException If this sequence is empty
		  */
		@throws[NoSuchElementException]("Throws when called for an empty sequence")
		def minIndexBy[B](f: A => B)(implicit order: Ordering[B]) = maxIndexBy(f)(order.reverse)
		
		/**
		  * @param f A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the largest value when mapped.
		  *         None if this sequence is empty.
		  */
		def maxOptionIndexBy[B](f: A => B)(implicit order: Ordering[B]) = if (seq.isEmpty) None else Some(maxIndexBy(f))
		
		/**
		  * @param f A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the smallest value when mapped.
		  *         None if this sequence is empty.
		  */
		def minOptionIndexBy[B](f: A => B)(implicit order: Ordering[B]) = maxOptionIndexBy(f)(order.reverse)
		
		/**
		  * @return A version of this seq with consecutive items paired. Each item will be present twice in the returned
		  *         collection, except the first and the last item. The first item will be presented once as the first
		  *         argument. The last item will be presented once as the second argument. If this sequence
		  *         contains less than two items, an empty seq is returned.
		  */
		def paired = (1 until seq.size).map { i => Pair(seq(i - 1), seq(i)) }
		
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
		  * @param range Range to slice from this sequence
		  * @return Slice of this sequence
		  */
		def slice(range: Range): Repr =
		{
			if (range.nonEmpty)
				seq.slice(range.head, range.last + 1)
			else
				seq.empty
		}
		
		/**
		  * @param another Another sequence
		  * @param equals Equality function
		  * @tparam B Type of another sequence's content
		  * @return Whether these two sequences are equal when using specified equality function
		  */
		def compareWith[B, CC2[X]](another: SeqOps[B, CC2, _])(equals: (A, B) => Boolean) = seq.size == another.size &&
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
	
	implicit class RichSeqLike2[A, CC[X] <: SeqOps[X, CC, CC[X]], Repr <: SeqOps[A, CC, CC[A]]](val seq: SeqOps[A, CC, Repr]) extends AnyVal
	{
		/**
		  * Creates a copy of this sequence with the specified item inserted to a certain index
		  * @param item An item to insert
		  * @param index Index where the item is inserted, where 0 is the first position
		  * @tparam B Type of resulting collection's items
		  * @return A copy of this collection with the item inserted
		  */
		def inserted[B >: A](item: B, index: Int): CC[B] =
		{
			if (index <= 0)
				seq.prepended(item)
			else if (index >= seq.size)
				seq.appended(item)
			else
			{
				val (beginning, end) = seq.splitAt(index)
				(beginning :+ item) ++ end
			}
		}
	}
	
	
	// ITERATOR ------------------------------------------
	
	implicit class RichIterator[A](val i: Iterator[A]) extends AnyVal
	{
		/**
		  * Enables polling on this iterator. This method yields a new iterator.
		  * This iterator shouldn't be used after the copy has been acquired. Only the pollable copy of this
		  * iterator should be used afterwards.
		  * @return A copy of this iterator that allows polling (checking of the next item without advancing)
		  */
		def pollable = new PollingIterator[A](i)
		
		/**
		  * Finds the last item accessible from this iterator. Consumes all items in this iterator.
		  * @throws NoSuchElementException If this iterator is empty
		  * @return The last item in this iterator
		  */
		@throws[NoSuchElementException]("If this iterator is empty")
		def last = {
			var current = i.next()
			while (i.hasNext) {
				current = i.next()
			}
			current
		}
		/**
		  * @return The last item accessible in this iterator. None if this iterator didn't have any items remaining.
		  */
		def lastOption = if (i.hasNext) Some(last) else None
		
		/**
		  * @return A paired copy of this iterator. An empty iterator if this iterator doesn't contain at least 2 items.
		  *         Consumes the first item within this iterator.
		  *         This iterator shouldn't be used after calling this function.
		  */
		def paired = {
			if (i.hasNext) {
				val start = i.next()
				new PairingIterator[A](start, i)
			}
			else
				Iterator.empty
		}
		
		/**
		  * @param item An item to prepend (call-by-name)
		  * @tparam B Type of that item
		  * @return A copy of this iterator with that item prepended. This iterator is invalidated.
		  */
		def +:[B >: A](item: => B): Iterator[B] = PollableOnce(item) ++ i
		/**
		  * @param item An item to append (call-by-name)
		  * @tparam B Type of that item
		  * @return A copy of this iterator with that item appended. This iterator is invalidated.
		  */
		def :+[B >: A](item: => B): Iterator[B] = i ++ PollableOnce(item)
		
		/**
		  * Checks whether there exists 'count' instances in this iterator that satisfy the specified predicate.
		  * Consumes items within this iterator until the required amount of matches has been found. If not enough
		  * matches were found, consumes this whole iterator.
		  * @param count Number of required matches (the minimum amount of times 'f' must return true)
		  * @param f A function for testing each item
		  * @return Whether 'f' returned true for 'count' items. Doesn't test whether 'f' would return true for more
		  *         than 'count' items.
		  */
		def existsCount(count: Int)(f: A => Boolean) =
		{
			var found = 0
			while (found < count && i.hasNext)
			{
				if (f(i.next()))
					found += 1
			}
			found >= count
		}
		
		/**
		  * Skips the next 'n' items in this iterator
		  * @param n Number of items to skip (default = 1)
		  */
		def skip(n: Int = 1) =
		{
			var skipped = 0
			while (skipped < n && i.hasNext)
			{
				i.next()
				skipped += 1
			}
		}
		
		/**
		  * Creates a copy of this iterator that terminates after the specified condition is met. This differs from
		  * takeWhile in that this function still returns the item which "terminated" this iterator
		  * (i.e. the first item for which the specified condition returned true).
		  * If present, this will be the last item returned by this new iterator.
		  * @param condition A condition that will terminate this new iterator
		  * @return A copy of this iterator that will not return items after the terminating item
		  */
		def takeTo(condition: A => Boolean) = TerminatingIterator(i)(condition)
		
		/**
		  * Performs the specified operation for the next 'n' items. This will advance the iterator n-steps
		  * (although limited by number of available items)
		  * @param n The maximum number of iterations / items handled
		  * @param operation Operation called for each handled item
		  * @tparam U Arbitrary operation result type (not used)
		  * @return Whether the full 'n' items were handled. If false, the end of this iterator was reached.
		  */
		def forNext[U](n: Int)(operation: A => U) =
		{
			var consumed = 0
			while (i.hasNext && consumed < n)
			{
				operation(i.next())
				consumed += 1
			}
			
			consumed == n
		}
		
		/**
		  * Collects the next 'n' items from this iterator, advancing it up to 'n' elements. The number of available
		  * items may be smaller, in case all remaining items are returned.
		  * @param n Number of items to collect
		  * @return Collected items as a vector
		  */
		def collectNext(n: Int) =
		{
			var consumed = 0
			val builder = new VectorBuilder[A]()
			while (i.hasNext && consumed < n)
			{
				builder += i.next()
				consumed += 1
			}
			
			builder.result()
		}
		/**
		  * Creates a new iterator that provides access only up to the next 'n' elements of this iterator
		  * @param n Number of items to make available
		  * @return An iterator that provides access to the next 'n' items in this iterator. Wraps this iterator.
		  */
		def takeNext(n: Int) = new LimitedLengthIterator[A](i, n)
		
		/**
		  * Collects the next n items from this iterator until a specified condition is met or until the end of this
		  * iterator is reached. The item which fulfills the specified condition is included in the result as the
		  * last item. Advances this iterator but doesn't invalidate it.
		  * @param stopCondition A condition that marks the last included item
		  * @return Items to and including the one accepted by the specified condition. All remaining items of this
		  *         iterator if the specified condition was never met.
		  */
		def collectTo(stopCondition: A => Boolean) =
		{
			val builder = new VectorBuilder[A]()
			var found = false
			while (i.hasNext && !found)
			{
				val nextItem = i.next()
				builder += nextItem
				if (stopCondition(nextItem))
					found = true
			}
			builder.result()
		}
		/**
		  * Takes the next n items from this iterator until a specified condition is met or until the end of this
		  * iterator is reached. The item which fulfills the specified condition is included in the result as the
		  * last item. Advances this iterator but doesn't invalidate it.
		  * @param stopCondition A condition that marks the last included item
		  * @return Items to and including the one accepted by the specified condition. All remaining items of this
		  *         iterator if the specified condition was never met.
		  */
		@deprecated("Please use collectTo(...) instead. Take implies returning another iterator", "v1.12")
		def takeNextTo(stopCondition: A => Boolean) =
		{
			val builder = new VectorBuilder[A]()
			var found = false
			while (i.hasNext && !found)
			{
				val nextItem = i.next()
				builder += nextItem
				if (stopCondition(nextItem))
					found = true
			}
			builder.result()
		}
		
		/**
		  * Consumes items until a specific condition is met
		  * @param condition A search condition
		  * @return The first item in this iterator that fulfills the condition.
		  *         None if none of the items in this iterator fulfilled the condition.
		  */
		def nextWhere(condition: A => Boolean) =
		{
			if (i.hasNext)
			{
				var current = i.next()
				var foundResult = condition(current)
				while (!foundResult && i.hasNext)
				{
					current = i.next()
					foundResult = condition(current)
				}
				if (foundResult)
					Some(current)
				else
					None
			}
			else
				None
		}
		
		/**
		  * Returns the next result that can be mapped to a specific value.
		  * After method call, this iterator will be placed at the item following the successfully mapped item.
		  * @param map A mapping function
		  * @tparam B Type of map result when one is found
		  * @return The first map result found. None if no map result could be acquired.
		  */
		def findMapNext[B](map: A => Option[B]) =
		{
			var current: Option[B] = None
			while (current.isEmpty && i.hasNext)
			{
				current = map(i.next())
			}
			current
		}
		
		/**
		  * Groups this iterator and performs the specified operation for each of the collected groups.
		  * Differs from .group(...).foreach(...) in that this method acts on all of the items in this iterator
		  * without discarding the possible smaller group at the end
		  * @param maxGroupSize Maximum number of items for a function call
		  * @param f A function that is called for each group of items
		  */
		def foreachGroup(maxGroupSize: Int)(f: Vector[A] => Unit) =
		{
			while (i.hasNext)
			{
				f(collectNext(maxGroupSize))
			}
		}
		
		/**
		  * Maps the items in this iterator, one group at a time
		  * @param groupSize The maximum size of an individual group of items to map
		  * @param map a mapping function applied to groups of items
		  * @tparam B Type of map result
		  * @return All map results in order
		  */
		def groupMap[B](groupSize: Int)(map: Vector[A] => B) =
		{
			val resultBuilder = new VectorBuilder[B]()
			foreachGroup(groupSize) { resultBuilder += map(_) }
			resultBuilder.result()
		}
		
		/**
		  * Groups the <b>consecutive</b> items in this iterator using the specified grouping function.
		  * The resulting iterator returns items as groups based on the group function result.
		  * @param f A grouping function
		  * @tparam G Type of group identifier
		  * @return An iterator that returns groups of consecutive items, including the group identifiers
		  */
		def groupBy[G](f: A => G) = GroupIterator(i)(f)
		
		/**
		  * Performs the specified function for each item in this iterator, consuming this iterator.
		  * Collects failures without interrupting iteration.
		  * @param f A function that returns success or failure
		  * @tparam U Arbitrary function return type
		  * @return Collected failures
		  */
		def foreachCatching[U](f: A => Try[U]) = {
			val failuresBuilder = new VectorBuilder[Throwable]()
			i.foreach { failuresBuilder ++= f(_).failure }
			failuresBuilder.result()
		}
		/**
		  * Maps this iterator with a function that can fail. Handles failures by catching them.
		  * @param f A mapping function
		  * @param handleError Function called for each encountered error
		  * @tparam B Type of successful map result
		  * @return Iterator of the mapped items
		  */
		def mapCatching[B](f: A => Try[B])(handleError: Throwable => Unit) = {
			i.flatMap { original =>
				f(original) match {
					case Success(item) => Some(item)
					case Failure(error) =>
						handleError(error)
						None
				}
			}
		}
		
		/**
		  * @param start The prepended pair start point (call-by-name).
		  *              This will be never called if this iterator is empty.
		  * @tparam B Type of pair parts
		  * @return A copy of this iterator that returns items as pairs, with the 'start' prepended.
		  *         E.g. If this iterator contained items [A, B, C] and start was X, the resulting iterator would
		  *         return [XA, AB, BC]
		  */
		def pairedFrom[B >: A](start: => B) = new PairingIterator[B](start, i)
	}
	
	
	// OTHER    ------------------------------------------
	
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
	
	implicit class RichTry[A](val t: Try[A]) extends AnyVal
	{
		/**
		  * The success value of this try. None if this try was a failure
		  */
		def success = t.toOption
		/**
		  * The failure (throwable) value of this try. None if this try was a success.
		  */
		def failure = t.failed.toOption
		
		/**
		  * @param f A mapping function for possible failure
		  * @tparam B Result type
		  * @return Contents of this try on success, mapped error on failure
		  */
		def getOrMap[B >: A](f: Throwable => B): B = t match
		{
			case Success(item) => item
			case Failure(error) => f(error)
		}
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
		  * @param f A mapping function for left values
		  * @tparam B Type of map result
		  * @return Right value or the mapped left value
		  */
		def rightOrMap[B >: R](f: L => B) = e match
		{
			case Right(r) => r
			case Left(l) => f(l)
		}
		/**
		  * @param f A mapping function for right values
		  * @tparam B Type of map result
		  * @return Left value or the mapped right value
		  */
		def leftOrMap[B >: L](f: R => B) = e match
		{
			case Right(r) => f(r)
			case Left(l) => l
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
	
	implicit class RichMap[K, V](val m: Map[K, V]) extends AnyVal
	{
		/**
		  * Maps the keys of this map
		  * @param f A mapping function
		  * @tparam K2 Type of the new keys to use
		  * @return A copy of this map with updated keys
		  */
		def mapKeys[K2](f: K => K2) = m.map { case (k, v) => f(k) -> v }
		
		/**
		  * Merges this map with another map. If value is present only in one map, it is preserved as is.
		  * @param another Another map
		  * @param merge A merge function used when both maps contain a value
		  * @tparam V2 The resulting value type
		  * @return A map with merged values
		  */
		@deprecated("Please use .mergeWith(Map)(...) instead", "v1.15")
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
		
		/**
		  * Merges this map with another map. If value is present only in one map, it is preserved as is.
		  * @param another Another map
		  * @param merge A merge function used when both maps contain a value
		  * @tparam V2 The resulting value type
		  * @return A map with merged values
		  */
		def mergeWith[V2 >: V](another: Map[K, V2])(merge: (V, V2) => V2) =
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
	
	implicit class RichIterableOnceEithers[L, R](val i: IterableOnce[Either[L, R]]) extends AnyVal
	{
		/**
		  * Divides this collection to two separate collections, one for left items and one for right items
		  * @return Left items + right items
		  */
		def divided = {
			val lBuilder = new VectorBuilder[L]
			val rBuilder = new VectorBuilder[R]
			i.iterator.foreach {
				case Left(l) => lBuilder += l
				case Right(r) => rBuilder += r
			}
			lBuilder.result() -> rBuilder.result()
		}
	}
	
	implicit class RichIterableOnceTries[A](val i: IterableOnce[Try[A]]) extends AnyVal
	{
		/**
		  * Divides this collection to two separate collections, one for failures and one for successes
		  * @return Failures + successes
		  */
		def divided = {
			val successesBuilder = new VectorBuilder[A]
			val failuresBuilder = new VectorBuilder[Throwable]
			i.iterator.foreach {
				case Success(a) => successesBuilder += a
				case Failure(error) => failuresBuilder += error
			}
			failuresBuilder.result() -> successesBuilder.result()
		}
	}
	
	/**
	  * This extension allows tuple lists to be transformed into multi maps directly
	  */
	implicit class RichTupleVector[K, V](val list: Vector[(K, V)]) extends AnyVal
	{
		/**
		  * @return This collection as a multi map
		  */
		def asMultiMap: Map[K, Vector[V]] = list.toMultiMap[K, V, Vector[V]] { t => t }
	}
	
	implicit class RichRange(val range: Range) extends AnyVal
	{
		/**
		  * @return The first index that is outside of this range
		  */
		def exclusiveEnd = range match
		{
			case r: Range.Exclusive => r.end
			case r: Range.Inclusive => if (r.step > 0) r.end + 1 else r.end - 1
		}
		
		/**
		  * This function works like foldLeft, except that it stores each step (including the start) into a vector
		  * @param start The starting step
		  * @param map A function for calculating the next step, takes the previous result + the next item in this range
		  * @param factory A factory for final collection (implicit)
		  * @tparam B The type of steps
		  * @return All of the steps mapped into a collection
		  */
		def foldMapToVector[B](start: B)(map: (B, Int) => B)(implicit factory: Factory[B, Vector[B]]): Vector[B] =
		{
			val builder = factory.newBuilder
			var last = start
			builder += last
			
			range.foreach { item =>
				last = map(last, item)
				builder += last
			}
			
			builder.result()
		}
	}
	
	implicit class RichInclusiveRange(val range: Range.Inclusive) extends AnyVal
	{
		/**
		  * @param stepSize How much this range is advanced on each step (sign doesn't matter)
		  * @return An iterator that contains all smaller ranges within this range. The length of these ranges is
		  *         determined by the 'step' of this range, although the last returned range may be shorter.
		  */
		def subRangeIterator(stepSize: Int): Iterator[Range.Inclusive] =
		{
			val step = if (range.start < range.end) stepSize.abs else -stepSize.abs
			new RangeIterator(range.start, range.end, step)
		}
	}
	
	private class RepeatingIterator[A, CC[X]](val c: IterableOps[A, CC, _]) extends Iterator[A]
	{
		// ATTRIBUTES   -----------------
		
		private var currentIterator: Option[Iterator[A]] = None
		
		
		// IMPLEMENTED  -----------------
		
		override def hasNext = iter().hasNext
		
		override def next() = iter().next()
		
		
		// OTHER    -------------------
		
		private def iter() =
		{
			if (currentIterator.forall { !_.hasNext } )
				currentIterator = Some(c.iterator)
			
			currentIterator.get
		}
	}
	
	private class RangeIterator(start: Int, end: Int, by: Int) extends Iterator[Range.Inclusive]
	{
		// ATTRIBUTES   ----------------------
		
		private val minStep = if (by < 0) -1 else if (by > 0) 1 else 0
		
		private var lastEnd = start - minStep
		
		
		// IMPLEMENTED  ----------------------
		
		override def hasNext = lastEnd != end
		
		override def next() =
		{
			val start = lastEnd + minStep
			val defaultEnd = start + by
			val actualEnd =
			{
				if ((by < 0 && defaultEnd < end) || (by > 0 && defaultEnd > end))
					end
				else
					defaultEnd
			}
			lastEnd = actualEnd
			start to actualEnd
		}
	}
}
package utopia.flow.collection

import utopia.flow.collection.immutable.Pair.PairIsIterable
import utopia.flow.collection.immutable.caching.iterable.{CachingSeq, LazySeq, LazyVector}
import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.collection.immutable.{Empty, IntSet, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.iterator._
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End.{EndingSequence, First, Last}
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.enumeration.{End, Extreme}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.util.{HasSize, TryCatch}
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.eventful.SettableFlag

import scala.collection.generic.{IsIterable, IsIterableOnce, IsSeq}
import scala.collection.immutable.{HashSet, VectorBuilder}
import scala.collection.{AbstractIterator, AbstractView, BuildFrom, Factory, IterableOps, SeqOps, View, mutable}
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.math.Ordered.orderingToOrdered
import scala.util.{Failure, Random, Success, Try}

/**
  * This object contains some extensions for the more traditional collections / data structures
  * @author Mikko Hilpinen
  * @since 10.10.2018
  * */
object CollectionExtensions
{
	// TYPES    -----------------------------
	
	/**
	  * Type where the item exists either on the Left or the Right side
	  */
	type Sided[+A] = Either[A, A]
	
	implicit def pairIsIterable[A]: PairIsIterable[A] = Pair.pairIsIterable
	
	
	// ITERABLE ONCE    ---------------------------------------
	
	// TODO: Move (some of) these to Iterator and/or Iterable instead
	class IterableOnceOperations[Repr, I <: IsIterableOnce[Repr]](coll: Repr, iter: I)
	{
		// ATTRIBUTES   -----------------------
		
		private lazy val ops = iter(coll)
		
		
		// OTHER    ---------------------------
		
		/**
		  * Splits this collection into a number of smaller pieces. Preserves order.
		  * @param maxLength Maximum length of each segment
		  * @param buildFrom A build from (implicit)
		  * @return An iterator that returns segments of this collection where each segment is at most 'maxLength' long
		  */
		def segmentsIterator(maxLength: Int)(implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Iterator[Repr] = {
			val knownSize = ops.knownSize
			if (knownSize >= 0 && knownSize <= maxLength)
				Iterator.single(coll)
			else {
				val iterator = ops.iterator
				OptionsIterator.continually {
					if (iterator.hasNext)
						Some(buildFrom.fromSpecific(coll)(iterator.takeNext(maxLength)))
					else
						None
				}
			}
		}
		/**
		  * Splits this collection into a number of smaller pieces. Preserves order.
		  * @param maxLength Maximum length of each segment
		  * @param buildFrom A build from (implicit)
		  * @return This sequence split into possibly larger number of smaller sequences
		  */
		def splitToSegments(maxLength: Int)(implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): IndexedSeq[Repr] =
			OptimizedIndexedSeq.from(segmentsIterator(maxLength))
		
		/**
		  * Filters this collection so that only distinct values remain. Uses a special function to determine equality
		  * @param equals    A function that determines whether two values are equal
		  * @param buildFrom Builder for the new collection
		  * @return A collection with only distinct values (when considering the provided 'equals' function)
		  */
		def distinctWith(equals: EqualsFunction[iter.A])(implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr = {
			val builder = buildFrom.newBuilder(coll)
			val collected = mutable.HashSet[iter.A]()
			
			ops.iterator.foreach { item =>
				if (!collected.exists { e => equals(e, item) }) {
					builder += item
					collected += item
				}
			}
			
			builder.result()
		}
		/**
		  * Filters this collection so that only distinct values remain. Compares the values by mapping them.
		  * @param f         A mapping function to produce comparable values
		  * @param buildFrom A builder (implicit) to build the final collection
		  * @tparam B Map target type
		  * @return A collection with only distinct values (based on mapping)
		  */
		def distinctBy[B](f: iter.A => B)(implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr =
			distinctWith { (a, b) => f(a) == f(b) }
		
		/**
		  * Zips the contents of this collection with another collection and merges the two values using
		  * the specified function.
		  * @param other Another collection
		  * @param merge A function that merges the values between these collections
		  * @param buildFrom Build-from for building the resulting collection
		  * @tparam B Type of values in the other collection
		  * @tparam R Type of merge results
		  * @tparam That Type of resulting collection
		  * @return Collection that contains merge results
		  */
		def zipAndMerge[B, R, That](other: IterableOnce[B])(merge: (iter.A, B) => R)
		                           (implicit buildFrom: BuildFrom[Repr, R, That]): That =
			buildFrom.fromSpecific(coll) { ops.iterator.zip(other).map { case (a, b) => merge(a, b) } }
		/**
		  * Zips the values of this collection with their map results
		  * @param f A mapping function
		  * @param buildFrom Implicit build-from for the resulting collection
		  * @tparam B Type of mapping results
		  * @tparam That Resulting collection type
		  * @return Collection that contains all items from this collection, coupled with their map results
		  */
		def zipMap[B, That](f: iter.A => B)(implicit buildFrom: BuildFrom[Repr, (iter.A, B), That]): That =
			buildFrom.fromSpecific(coll)(ops.iterator.map { a => (a, f(a)) })
		/**
		  * Joins the values of this collection with their 0-n map results, returning the values as individual pairs
		  * @param f Mapping function
		  * @param buildFrom Implicit build-from for the resulting collection
		  * @tparam B Type of the individual mapping results
		  * @tparam That Type of the resulting collection
		  * @return Collection where each map result is coupled with the original mapping input value (on the left)
		  */
		def zipFlatMap[B, That](f: iter.A => IterableOnce[B])
		                       (implicit buildFrom: BuildFrom[Repr, (iter.A, B), That]): That =
			buildFrom.fromSpecific(coll)(ops.iterator.flatMap { a => f(a).iterator.map { a -> _ } })
		
		/**
		  * This function works like foldLeft, except that it stores each step (including the start) into a seq
		  * @param start     The starting step
		  * @param map       A function for calculating the next step, takes the previous result + the next item in this seq
		  * @param buildFrom A buildfrom for final collection (implicit)
		  * @tparam B    The type of steps
		  * @tparam That The type of final collection
		  * @return All of the steps mapped into a collection
		  */
		def foldMapLeft[B, That](start: B)(map: (B, iter.A) => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That = {
			val builder = buildFrom.newBuilder(coll)
			var last = start
			builder += last
			
			ops.iterator.foreach { item =>
				last = map(last, item)
				builder += last
			}
			
			builder.result()
		}
		
		// Referenced from: https://stackoverflow.com/questions/22090371/scala-grouping-list-of-tuples [10.10.2018]
		
		/**
		  * Converts this iterable item to a map with possibly multiple values per key
		  * @param toKey   A function for mapping items to keys
		  * @param toValue A function for mapping items to values
		  * @param bf      Implicit build from for the final values collections
		  * @tparam Key    Type of key in the final map
		  * @tparam Value  Type of individual values in the final map
		  * @tparam Values Type of values collections in the final map
		  * @return A multi map based on this iteration mapping
		  */
		@deprecated("Please use .groupMap(...) instead", "v2.4")
		def toMultiMap[Key, Value, Values](toKey: iter.A => Key)(toValue: iter.A => Value)(
			implicit bf: BuildFrom[Repr, Value, Values]): Map[Key, Values] = toMultiMap { a => toKey(a) -> toValue(a) }
		/**
		  * Converts this iterable item to a map with possibly multiple values per key
		  * @param f  A function for mapping items to key value pairs
		  * @param bf Implicit build from for the final values collections
		  * @tparam Key    Type of key in the final map
		  * @tparam Value  Type of individual values in the final map
		  * @tparam Values Type of values collections in the final map
		  * @return A multi map based on this iteration mapping
		  */
		@deprecated("Please use .groupMap instead", "v2.4")
		def toMultiMap[Key, Value, Values](f: iter.A => (Key, Value))(implicit bf: BuildFrom[Repr, Value, Values]): Map[Key, Values] =
		{
			val buffer = mutable.Map.empty[Key, mutable.Builder[Value, Values]]
			ops.iterator.foreach { item =>
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
		def tryForeach[U](f: iter.A => Try[U]): Try[Unit] = {
			ops.iterator.map(f).find { _.isFailure } match {
				case Some(failure) => failure.map { _ => () }
				case None => Success(())
			}
		}
		/**
		  * Maps the contents of this collection. Mapping may fail, interrupting all remaining mappings
		  * @param f  A mapping function. May fail.
		  * @param bf A build from for the final collection (implicit)
		  * @tparam B  Type of map result
		  * @tparam To Type of final collection
		  * @return Mapped collection if all mappings succeeded. Failure otherwise.
		  */
		def tryMap[B, To](f: iter.A => Try[B])(implicit bf: BuildFrom[Repr, B, To]): Try[To] = {
			val buffer = bf.newBuilder(coll)
			// Maps items until the mapping function fails
			tryForeach { f(_).map { buffer += _ } }.map { _ => buffer.result() }
		}
		/**
		 * Maps the contents of this collection using a mapping function that may produce a failure.
		 * If the mapping fails for any item, the whole mapping process is cancelled and fails.
		 * @param f A mapping function. May yield full or partial failures.
		 *          Partial failures are recorded, full failures terminate and fail the mapping process.
		 * @param bf Implicit buildfrom for the resulting collection
		 * @tparam B Type of items in the resulting collection
		 * @tparam To Type of the resulting collection
		 * @return Success or failure.
		 *         Success contains the mapping results (all successful) and encountered partial failures.
		 */
		def tryMapCatching[B, To](f: iter.A => TryCatch[B])(implicit bf: BuildFrom[Repr, B, To]): TryCatch[To] = {
			val caughtFailuresBuilder = new VectorBuilder[Throwable]()
			// Maps items until the mapping function fails
			tryMap[B, To] { a =>
				val (result, failures) = f(a).separateToTry
				caughtFailuresBuilder ++= failures
				result
			} match {
				case Success(result) => TryCatch.Success(result, caughtFailuresBuilder.result())
				case Failure(error) => TryCatch.Failure(error)
			}
		}
		/**
		  * FlatMaps the contents of this collection. Mapping may fail, however, cancelling all remaining mappings
		  * @param f  A mapping function. May fail.
		  * @param bf A build from for the final collection (implicit)
		  * @tparam B  Type of individual map result item
		  * @tparam To Type of final collection
		  * @return Flat mapped collection if all mappings succeeded. Failure otherwise.
		  */
		def tryFlatMap[B, To](f: iter.A => Try[IterableOnce[B]])(implicit bf: BuildFrom[Repr, B, To]): Try[To] = {
			val buffer = bf.newBuilder(coll)
			tryForeach { f(_).map { buffer ++= _ } }.map { _ => buffer.result() }
		}
		
		/**
		  * Takes elements from this collection until the specified condition is met. If found, includes the item
		  * on which that condition was met.
		  * @param endCondition A condition for ending this take operation (returns true on the last item to collect)
		  * @param buildFrom    Implicit build from
		  * @tparam That Target collection type
		  * @return All elements of this collection until the first item that matches the specified condition +
		  *         the matching item itself. Contains all items of this collection if the condition was never met.
		  */
		def takeTo[That](endCondition: iter.A => Boolean)(implicit buildFrom: BuildFrom[Repr, iter.A, That]): That =
			buildFrom.fromSpecific(coll)(TerminatingIterator(ops.iterator)(endCondition))
		
		/**
		  * Divides the items in this collection into two groups, based on boolean result
		  * @param f  A function that separates the items
		  * @param bf an implicit buildFrom for the resulting collection type
		  * @tparam To type of the resulting collection
		  * @return A Pair that contains first the 'false' group, and then the 'true' group
		  */
		def divideBy[To](f: iter.A => Boolean)(implicit bf: BuildFrom[Repr, iter.A, To]) = {
			val falseBuilder = bf.newBuilder(coll)
			val trueBuilder = bf.newBuilder(coll)
			ops.iterator.foreach { a => if (f(a)) trueBuilder += a else falseBuilder += a }
			Pair(falseBuilder.result(), trueBuilder.result())
		}
		
		/**
		  * Collects the most extreme items from this collection, based on the specified mapping function
		  * @param extreme The extreme being collected (min | max)
		  * @param f A mapping function
		  * @param bf Implicit buildfrom
		  * @param ord Implicit ordering applied for the mapped values
		  * @tparam B Type of map results
		  * @tparam To Type of resulting collection
		  * @return All items in this collection that share the most extreme (i.e. min | max) mapping result
		  */
		def filterBy[B, To](extreme: Extreme)(f: iter.A => B)
		                   (implicit bf: BuildFrom[Repr, iter.A, To], ord: Ordering[B]) =
		{
			// Iterates over all items in this collection
			val iterator = ops.iterator
			if (iterator.hasNext) {
				// Collects the current best results into 'lastBuilder'
				val actualOrdering = extreme.ascendingToExtreme(ord)
				var lastBuilder = bf.newBuilder(coll)
				
				// Starts by collecting the first item.
				// Keeps track of the current best map result
				val firstItem = iterator.next()
				var bestValue = f(firstItem)
				lastBuilder += firstItem
				
				iterator.foreach { item =>
					// Compares the map result of the new item to the current best map result
					val value = f(item)
					val cmp = actualOrdering.compare(value, bestValue)
					// Case: As good as the best result => Adds this item to the same collection
					if (cmp == 0)
						lastBuilder += item
					// Case: Better than the best result => Replaces the collected item with a new collection
					else if (cmp > 0) {
						lastBuilder = bf.newBuilder(coll)
						lastBuilder += item
						bestValue = value
					}
				}
				lastBuilder.result()
			}
			else
				bf.fromSpecific(coll)(Iterator.empty)
		}
		/**
		  * Collects the largest items from this collection, based on the specified mapping function
		  * @param f       A mapping function
		  * @param bf      Implicit buildfrom
		  * @param ord     Implicit ordering applied for the mapped values
		  * @tparam B  Type of map results
		  * @tparam To Type of resulting collection
		  * @return All items in this collection that share the maximum mapping result
		  */
		def filterMaxBy[B, To](f: iter.A => B)(implicit bf: BuildFrom[Repr, iter.A, To], ord: Ordering[B]): To =
			filterBy(Max)(f)
		/**
		  * Collects the smallest items from this collection, based on the specified mapping function
		  * @param f       A mapping function
		  * @param bf      Implicit buildfrom
		  * @param ord     Implicit ordering applied for the mapped values
		  * @tparam B  Type of map results
		  * @tparam To Type of resulting collection
		  * @return All items in this collection that share the smallest mapping result
		  */
		def filterMinBy[B, To](f: iter.A => B)(implicit bf: BuildFrom[Repr, iter.A, To], ord: Ordering[B]): To =
			filterBy(Min)(f)
		/**
		  * Collects the most extreme items from this collection
		  * @param extreme The extreme being collected (min | max)
		  * @param bf      Implicit buildfrom
		  * @param ord     Implicit ordering to use
		  * @tparam To Type of resulting collection
		  * @return All items in this collection that share the status of the most extreme value (i.e. min | max)
		  */
		def filter[To](extreme: Extreme)(implicit bf: BuildFrom[Repr, iter.A, To], ord: Ordering[iter.A]) =
			filterBy[iter.A, To](extreme)(Identity)
	}
	
	implicit def iterableOnceOperations[Repr](coll: Repr)(implicit iter: IsIterableOnce[Repr]): IterableOnceOperations[Repr, iter.type] =
		new IterableOnceOperations(coll, iter)
	
	implicit class RichIterableOnce[A](val i: IterableOnce[A]) extends AnyVal
	{
		// COMPUTED -----------------------------
		
		/**
		  * @return Empty, Single, Pair or Vector, containing this collection's contents
		  */
		def toOptimizedSeq = OptimizedIndexedSeq.from(i)
		
		/**
		  * @return Iterator of this collection. None if this collection is empty.
		  */
		def nonEmptyIterator = {
			val iter = i.iterator
			if (iter.hasNext) Some(iter) else None
		}
		
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
		  * @return A version of this collection that caches iteration results
		  */
		def caching = CachingSeq.from(i)
		
		/**
		 * Counts how many times each item appears within this collection.
		 * @return A map where the keys are the unique items in this collection and the values
		 *         are the numbers of times they appear within this collection.
		 *
		 *         The resulting map has a default value of 0.
		 */
		def countAll: Map[A, Int] = {
			val iter = i.iterator
			if (iter.hasNext) {
				val buffer = mutable.Map[A, Int]()
				iter.foreach { a =>
					buffer.updateWith(a) {
						case Some(c) => Some(c + 1)
						case None => Some(1)
					}
				}
				buffer.toMap.withDefaultValue(0)
			}
			else
				Map.empty.withDefaultValue(0)
		}
		
		
		// OTHER    --------------------------
		
		/**
		 * Performs the specified operation for all elements in this collection,
		 * but only while the specified condition returns true.
		 * @param condition A condition that is required for the iteration to continue.
		 *                  Called between each iteration.
		 * @param f Function called for each item
		 * @tparam U Arbitrary function result type
		 */
		def foreachWhile[U](condition: => Boolean)(f: A => U) = {
			val iter = i.iterator
			while (condition && iter.hasNext) {
				f(iter.next())
			}
		}
		
		/**
		  * Checks whether the specified condition matches none of the items in this collection
		  * @param f A testing function / condition
		  * @return Whether the specified function returns false for all items in this collection
		  */
		def forNone(f: A => Boolean) = i.iterator.forall { !f(_) }
		
		/**
		  * Checks whether there exists at least 'requiredCount' items in this collection where the specified
		  * condition 'f' returns true. Compared to .count -function, this function is more optimized since it stops
		  * counting once the required amount has been reached.
		  * @param requiredCount The required amount of matches before returning true
		  * @param f             A test function
		  * @return Whether 'requiredCount' number of items were found where the specified function 'f' returned true
		  */
		def existsCount(requiredCount: Int)(f: A => Boolean) = {
			val iter = i.iterator
			var currentCount = 0
			while (iter.hasNext && currentCount < requiredCount) {
				if (f(iter.next()))
					currentCount += 1
			}
			currentCount == requiredCount
		}
		/**
		 * Checks whether there exists exactly 'count' items in this collection that satisfy the specified predicate.
		 * Consumes items until the result may be determined (i.e. count has been exceeded).
		 * @param count Required number of matches
		 * @param f     A function for testing items
		 * @return Whether there exist exactly 'count' items in this collection for which 'f' returned true.
		 */
		def existsExactCount(count: Int)(f: A => Boolean) = {
			val iter = i.iterator
			val tooMany = count + 1
			var found = 0
			while (found < tooMany && iter.hasNext) {
				if (f(iter.next()))
					found += 1
			}
			found == count
		}
		
		/**
		  * Maps items until a concrete result is found, then returns that result
		  * @param map A mapping function that maps to either Some or None
		  * @tparam B The map target type
		  * @return The first item that was mapped to Some. None if all items were mapped to None.
		  */
		def findMap[B](map: A => Option[B]) = i.iterator.map(map).find { _.isDefined }.flatten
		
		/**
		  * @param f A mapping function
		  * @tparam B Type of mapping results
		  * @return A collection where the items in this collection are lazily mapped and then cached
		  */
		def mapCaching[B](f: A => B) =
			new CachingSeq[B](i.iterator.map(f), externallyKnownSize = Some(i.knownSize).filter { _ >= 0 })
		/**
		  * Lazily maps the contents of this collection.
		  * @param f A mapping function
		  * @tparam B Mapping result type
		  * @return A lazily initialized collection containing the mapping results
		  */
		def lazyMap[B](f: A => B) = LazySeq[B](View.from(i).map { a => Lazy { f(a) } })
		/**
		  * Lazily maps the contents of this collection
		  * @param f A mapping function that returns 0-n lazily initialized items for each element
		  * @tparam B Type of the lazily initialized items
		  * @return A lazily initialized collection containing the mapping results
		  */
		def lazyFlatMap[B](f: A => IterableOnce[Lazy[B]]) = LazySeq[B](i.iterator.flatMap(f))
		
		/**
		  * Divides / maps the items in this collection to two groups
		  * @param f A function for separating / mapping the items
		  * @tparam L Type of left group items
		  * @tparam R Type of right group items
		  * @return The Left group and then the Right group (as sequences)
		  */
		def divideWith[L, R](f: A => Either[L, R]) = {
			val lBuilder = new VectorBuilder[L]()
			val rBuilder = new VectorBuilder[R]()
			i.iterator.map(f).foreach {
				case Left(l) => lBuilder += l
				case Right(r) => rBuilder += r
			}
			lBuilder.result() -> rBuilder.result()
		}
		/**
		  * Divides the contents of this collection into two groups. Each item may represent 0-n items in the
		  * resulting group(s)
		  * @param f A function that accepts an item in this collection and returns 0-n grouped items
		  *          (Left(x) for a left group item x and Right(y) for a right group item y)
		  * @tparam L Type of left group items
		  * @tparam R Type of right group items
		  * @return A Pair containing first the left group items and second the collected right group items.
		 *         Both groups appear in Vector format.
		  */
		def flatDivideWith[L, R](f: A => IterableOnce[Either[L, R]]) = {
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
		def splitMap[L, R](f: A => (L, R)) = {
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
		def splitFlatMap[L, R](f: A => (IterableOnce[L], IterableOnce[R])) = {
			val lBuilder = new VectorBuilder[L]()
			val rBuilder = new VectorBuilder[R]()
			i.iterator.map(f).foreach { case (lefts, rights) =>
				lBuilder ++= lefts
				rBuilder ++= rights
			}
			lBuilder.result() -> rBuilder.result()
		}
		
		/**
		  * @param start Fold starting value
		  * @param f     A fold function
		  * @tparam V Type of fold function return values
		  * @return An iterator that folds the items in this collection and returns every iteration result
		  */
		def foldLeftIterator[V](start: V)(f: (V, A) => V) = new FoldingIterator[A, V](start, i.iterator)(f)
		/**
		  * @param f A reduce function
		  * @return An iterator that reduces the items in this collection and returns every iteration result
		  */
		def reduceLeftIterator(f: (A, A) => A) = FoldingIterator.reduce(i.iterator)(f)
	}
	
	implicit class IntsIterableOnce(val i: IterableOnce[Int]) extends AnyVal
	{
		/**
		  * @return An IntSet instance containing the distinct integers in this collection
		  */
		def toIntSet = IntSet.from(i)
	}
	
	implicit class RichIterableOnceTuples[A, B](val i: IterableOnce[(A, B)]) extends AnyVal
	{
		/**
		 * @return Contents of this collection split into two groups (as vectors)
		 */
		def split = {
			val lBuilder = new VectorBuilder[A]()
			val rBuilder = new VectorBuilder[B]()
			i.iterator.foreach { case (a, b) =>
				lBuilder += a
				rBuilder += b
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
		def toTryCatch: TryCatch[Vector[A]] = {
			val (failures, successes) = tries.divided
			if (successes.isEmpty) {
				failures.headOption match {
					// Case: All attempts failed => fails with the first encountered error
					case Some(firstError) => TryCatch.Failure(firstError)
					// Case: No attempts were made => empty success
					case None => TryCatch.Success(successes, failures)
				}
			}
			// Case: One or more attempts succeeded => success
			else
				TryCatch.Success(successes, failures)
		}
		
		/**
		 * Divides this collection to two separate collections, one for failures and one for successes
		 * @return Failures + successes
		 */
		def divided = {
			val successesBuilder = new VectorBuilder[A]
			val failuresBuilder = new VectorBuilder[Throwable]
			tries.iterator.foreach {
				case Success(a) => successesBuilder += a
				case Failure(error) => failuresBuilder += error
			}
			failuresBuilder.result() -> successesBuilder.result()
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
	
	implicit class TryCatchesIterableOnce[A](val tries: IterableOnce[TryCatch[A]]) extends AnyVal
	{
		/**
		 * @return Success if at least one of the items in this collection was a success,
		 *         or if this collection is empty.
		 *         Failure otherwise.
		 */
		def toTryCatch: TryCatch[IndexedSeq[A]] = {
			// Collects all success and failure values, including partial failures
			val successesBuilder = new VectorBuilder[A]()
			val failuresBuilder = new VectorBuilder[Throwable]()
			tries.iterator.foreach {
				case TryCatch.Success(v, failures) =>
					successesBuilder += v
					failuresBuilder ++= failures
				case TryCatch.Failure(error) => failuresBuilder += error
			}
			val failures = failuresBuilder.result()
			successesBuilder.result().notEmpty match {
				// Case: There was at least one success => Succeeds
				case Some(successes) => TryCatch.Success(successes, failures)
				case None =>
					failures.headOption match {
						// Case: No successes => Fails
						case Some(error) => TryCatch.Failure(error)
						// Case: Empty collection => Succeeds
						case None => TryCatch.Success(Empty)
					}
			}
		}
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
		// ATTRIBUTES   -----------------------
		
		private lazy val ops = iter(coll)
		
		
		// COMPUTED ---------------------------
		
		/**
		  * @return This collection if not empty. Otherwise None.
		  */
		def notEmpty = if (ops.isEmpty) None else Some(coll)
		
		/**
		  * Takes 'n' largest items from this collection
		  * @param n Number of items to include in the result
		  * @param bf Implicit build-from for the resulting collection
		  * @param ord Implicit ordering to apply
		  * @return 'n' largest items from this collection
		  */
		def takeMax(n: Int)(implicit bf: BuildFrom[Repr, iter.A, Repr], ord: Ordering[iter.A]) = {
			// Case: Taking no items => Returns an empty collection
			if (n <= 0)
				bf.fromSpecific(coll)(Empty)
			// Case: Taking only one item => Same as maxOption with potentially different result type
			else if (n == 1)
				bf.fromSpecific(coll)(ops.maxOption)
			else {
				// Case: Taking all items => Returns this collection
				if (ops.sizeCompare(n) <= 0)
					coll
				else {
					val iterator = ops.iterator
					val buffer = mutable.Buffer[iter.A]()
					buffer ++= iterator.collectNext(n).sorted
					
					iterator.foreach { item =>
						val index = buffer.view.takeWhile { _ < item }.size
						if (index > 0) {
							buffer.remove(0)
							buffer.insert(index - 1, item)
						}
					}
					
					bf.fromSpecific(coll)(buffer)
				}
			}
		}
		/**
		  * Takes 'n' smallest items from this collection
		  * @param n Number of items to include in the result
		  * @param bf Implicit build-from for the resulting collection
		  * @param ord Implicit ordering to apply
		  * @return 'n' smallest items from this collection
		  */
		def takeMin(n: Int)(implicit bf: BuildFrom[Repr, iter.A, Repr], ord: Ordering[iter.A]) =
			takeMax(n)(bf, ord.reverse)
		/**
		  * Takes 'n' largest items from this collection based on mapping function results
		  * @param n Number of items to include in the result
		  * @param f Mapping function for acquiring the compared values
		  * @param bf Implicit build-from for the resulting collection
		  * @param ord Implicit ordering to apply
		  * @return 'n' largest items from this collection
		  */
		// WET WET from takeMax - This version is optimized for this use-case (could still use a common function)
		def takeMaxBy[B](n: Int)(f: iter.A => B)(implicit bf: BuildFrom[Repr, iter.A, Repr], ord: Ordering[B]) = {
			// Case: Taking no items => Returns an empty collection
			if (n <= 0)
				bf.fromSpecific(coll)(Empty)
			// Case: Taking only one item => Same as maxOption with potentially different result type
			else if (n == 1)
				bf.fromSpecific(coll)(ops.maxByOption(f))
			else {
				// Case: Taking all items => Returns this collection
				if (ops.sizeCompare(n) <= 0)
					coll
				else {
					val iterator = ops.iterator
					val buffer = mutable.Buffer[(iter.A, B)]()
					buffer ++= iterator.collectNext(n).map { a => a -> f(a) }.sortBy { _._2 }
					
					iterator.foreach { item =>
						val mapResult = f(item)
						val index = buffer.view.takeWhile { _._2 < mapResult }.size
						if (index > 0) {
							buffer.remove(0)
							buffer.insert(index - 1, item -> mapResult)
						}
					}
					
					bf.fromSpecific(coll)(buffer.view.map { _._1 })
				}
			}
		}
		/**
		  * Takes 'n' smallest items from this collection based on mapping function results
		  * @param n Number of items to include in the result
		  * @param f Mapping function for acquiring the compared values
		  * @param bf Implicit build-from for the resulting collection
		  * @param ord Implicit ordering to apply
		  * @return 'n' smallest items from this collection
		  */
		def takeMinBy[B](n: Int)(f: iter.A => B)(implicit bf: BuildFrom[Repr, iter.A, Repr], ord: Ordering[B]) =
			takeMaxBy(n)(f)(bf, ord.reverse)
		/**
		  * Takes 'n' smallest or largest items from this collection
		  * @param n Number of items to include in the result
		  * @param extreme Targeted extreme, i.e. whether to take the largest or the smallest items
		  * @param bf Implicit build-from for the resulting collection
		  * @param ord Implicit ordering to apply
		  * @return 'n' smallest or largest items from this collection
		  */
		def takeExtreme(n: Int, extreme: Extreme)(implicit bf: BuildFrom[Repr, iter.A, Repr], ord: Ordering[iter.A]) =
			takeMax(n)(bf, extreme.ascendingToExtreme(ord))
		/**
		  * Takes 'n' smallest or largest items from this collection based on mapping function results
		  * @param n Number of items to include in the result
		  * @param extreme Targeted extreme, i.e. whether to take the largest or the smallest items
		  * @param f Mapping function for acquiring the compared values
		  * @param bf Implicit build-from for the resulting collection
		  * @param ord Implicit ordering to apply
		  * @return 'n' smallest or largest items from this collection
		  */
		def takeExtremeBy[B](n: Int, extreme: Extreme)(f: iter.A => B)
		                    (implicit bf: BuildFrom[Repr, iter.A, Repr], ord: Ordering[B]) =
			takeMaxBy(n)(f)(bf, extreme.ascendingToExtreme(ord))
		
		/**
		 * Attempts to successfully "find" a value for each item in this collection.
		 * If the "find" process fails for one item, this process is considered to have failed for this whole
		 * collection and the process fails.
		 * @param f Function which returns either Some containing a successful mapping / find result or None.
		 * @param bf Implicit build-from for the resulting collection on success
		 * @tparam B Type of map / find results, when successful
		 * @tparam To Type of the resulting collection, when successful
		 * @return If 'f' yielded Some for all items in this collection,
		 *         returns a new collection containing the find / map results.
		 *         If 'f' yielded None for any item, returns None.
		 */
		def findForAll[B, To](f: iter.A => Option[B])(implicit bf: BuildFrom[Repr, B, To]) = {
			val iter = ops.iterator
			if (iter.hasNext) {
				// Maps and stores items as long as 'f' yields Some
				val builder = bf.newBuilder(coll)
				var successful = true
				while (successful && iter.hasNext) {
					f(iter.next()) match {
						case Some(a) => builder += a
						// Case: 'f' yielded None => Cancels the mapping process
						case None => successful = false
					}
				}
				// Case: All items successfully mapped => Returns the mapped collection
				if (successful)
					Some(builder.result())
				// Case: Mapping failed => Returns None
				else
					None
			}
			// Case: Empty collection => Returns immediately without mapping
			else
				Some(bf.fromSpecific(coll)(Empty))
		}
		
		/**
		  * Finds the item(s) that best match the specified conditions
		  * @param matchers Search conditions used. The conditions that are introduced first are considered more
		  *                 important than those which are introduced the last.
		  * @return The items in this collection that best match the specified conditions
		  */
		def bestMatch(matchers: IterableOnce[iter.A => Boolean])(implicit bf: BuildFrom[Repr, iter.A, Repr]): Repr =
			matchers.iterator.foldLeft(coll) { case (coll, matcher) => _bestMatch(coll, matcher) }
		def bestMatch(firstMatcher: iter.A => Boolean, secondMatcher: iter.A => Boolean, more: (iter.A => Boolean)*)
		             (implicit bf: BuildFrom[Repr, iter.A, Repr]): Repr =
			bestMatch(Pair(firstMatcher, secondMatcher) ++ more)
		/**
		  * Filters this collection with the specified filter function, but if the results would be empty, returns
		  * this collection instead
		  * @param matcher A filter / matcher function
		  * @return Filtered results, or this collection if resulting collection was empty
		  */
		def bestMatch(matcher: iter.A => Boolean)(implicit bf: BuildFrom[Repr, iter.A, Repr]) =
			_bestMatch(coll, matcher)
		
		private def _bestMatch(coll: Repr, matcher: iter.A => Boolean)(implicit bf: BuildFrom[Repr, iter.A, Repr]) = {
			val ops = iter(coll)
			// Case: 1 or 0 items => Result will always be the best match
			if (ops.sizeCompare(2) < 0)
				coll
			else {
				val iter = ops.iterator
				// Scans the collection for the first match => Result determines whether a new collection will be formed
				iter.find(matcher) match {
					// Case: Found a match => Generates a new collection by finding the remaining matches
					case Some(firstMatch) =>
						if (iter.hasNext)
							bf.fromSpecific(coll)(Iterator.single(firstMatch) ++ iter.filter(matcher))
						// Case: No more items available => No need to search for further matches
						else
							bf.fromSpecific(coll)(Single(firstMatch))
					// Case: No match found => Returns the original collection
					case None => coll
				}
			}
		}
		
		/**
		  * Collects the results of a 'takeWhile' operation, also returning the remaining items as a separate
		  * collection.
		  * In other words, collects into one collection all the initial consecutive items which satisfy the specified
		  * condition. Once a non-satisfying item has been found, collects it and all the remaining items into another
		  * collection.
		  * @param f A function for determining whether an item should be collected to the first collection
		  * @param bf An implicit buildFrom for the resulting collections
		  * @return The initial consecutive items which satisfied the specified predicate,
		  *         followed by the remaining items.
		  */
		def popWhile(f: iter.A => Boolean)(implicit bf: BuildFrom[Repr, iter.A, Repr]) = {
			val popBuilder = bf.newBuilder(coll)
			val remainBuilder = bf.newBuilder(coll)
			val foundFlag = SettableFlag()(SysErrLogger)
			val currentBuilderPointer = foundFlag.strongMap { if (_) remainBuilder else popBuilder }
			ops.foreach { item =>
				if (foundFlag.isNotSet && !f(item))
					foundFlag.set()
				currentBuilderPointer.value += item
			}
			popBuilder.result() -> remainBuilder.result()
		}
		
		/**
		  * 'Zips' this collection with another, padding the one that is shorter so that all items from both
		  * collections are included in the resulting collection.
		  * @param other        Another collection
		  * @param myPadding    Padding to use for this collection, if shorter (call-by-name)
		  * @param theirPadding Padding to use for the other collection, if shorter (call-by-name)
		  * @param bf           Implicit BuildFrom
		  * @tparam B  Type of items in the other collection
		  * @tparam To Type of resulting collection
		  * @return A collection that contains tuples where the first values are from this collection and
		  *         the second values are from the other collection.
		  */
		def zipPad[B, To](other: Iterable[B], myPadding: => iter.A, theirPadding: => B)
		                 (implicit bf: BuildFrom[Repr, (iter.A, B), To]) =
			bf.fromSpecific(coll)(ZipPadIterator(ops.iterator, other.iterator, myPadding, theirPadding))
		/**
		  * 'Zips' this collection with another, padding the one that is shorter so that all items from both
		  * collections are included in the resulting collection.
		  * @param other   Another collection
		  * @param padding Padding to use for the shorter collection, if one exists (call-by-name)
		  * @param bf      Implicit BuildFrom
		  * @tparam To Type of resulting collection
		  * @return A collection that contains tuples where the first values are from this collection and
		  *         the second values are from the other collection.
		  */
		def zipPad[To](other: Iterable[iter.A], padding: => iter.A)
		              (implicit bf: BuildFrom[Repr, (iter.A, iter.A), To]): To =
			zipPad[iter.A, To](other, padding, padding)
		
		/**
		  * Maps a single existing item in this collection, or appends a new item instead
		  * @param f A mapping function that yields a
		  *          Some if successful (i.e. if this should be 'the' mapping to apply) and
		  *          None if failed (i.e. if this wasn't the item to map)
		  * @param append A function that yields a new item to append to this collection.
		  *               Called only if the specified mapping function 'f' yielded None for all items
		  *               in this collection.
		  * @param buildFrom An implicit buildfrom for the resulting collection
		  * @return A copy of this collection with either one item mapped,
		  *         or the specified item added to the end of this collection
		  */
		def mapOrAppend(f: iter.A => Option[iter.A])(append: => iter.A)
		               (implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr =
		{
			val builder = buildFrom.newBuilder(coll)
			var found = false
			ops.iterator.foreach { a =>
				// Case: Already successfully mapped an item => Simply collects the remaining items
				if (found)
					builder += a
				// Case: No successful mapping done yet => attempts to map the next item
				else
					f(a) match {
						// Case: Mapping succeeded => Remembers it and adds the mapping result
						case Some(mapped) =>
							found = true
							builder += mapped
						// Case: Mapping failed => Adds the original item and moves to the next item instead
						case None => builder += a
					}
			}
			// If mapping failed for all items, appends a new item to this collection
			if (!found)
				builder += append
			builder.result()
		}
		/**
		  * Merges an item with an existing item, or appends it at the end of this collection
		  * @param item The item to either merge or append to this collection
		  * @param findMatch A function that yields true for the item that should be merged with the new item.
		  *                  A kind of a find function for the merge target.
		  * @param merge A function that accepts the already existing item and the new item and merges them yielding
		  *              a third item, which will then replace the first item.
		  * @param buildFrom An implicit buildfrom for the resulting collection
		  * @return A copy of this collection with either one item merged with the specified new item,
		  *         or the specified item added to the end of this collection
		  */
		def mergeOrAppend(item: iter.A)(findMatch: iter.A => Boolean)(merge: (iter.A, iter.A) => iter.A)
		                 (implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr =
			mapOrAppend { a => if (findMatch(a)) Some(item) else None }(item)
		/**
		 * Replaces an existing item with a new version, or appends that version to the end of this collection
		 * @param item An item to place in this collection
		 * @param findMatch A function for finding the item to replace
		 * @param buildFrom An implicit buildfrom for the resulting collection
		 * @return A copy of this collection with either one item replaced or the specified item appended
		 */
		def replaceOrAppend(item: iter.A)(findMatch: iter.A => Boolean)
		                   (implicit buildFrom: BuildFrom[Repr, iter.A, Repr]): Repr =
			mergeOrAppend(item)(findMatch) { (_, i) => i }
	}
	
	implicit def iterableOperations[Repr](coll: Repr)
	                                     (implicit iter: IsIterable[Repr]): IterableOperations[Repr, iter.type] =
		new IterableOperations(coll, iter)
	
	// Used for operations that expose the underlying element type 'A'
	class IterableOperations2[A, C](coll: C, ops: IterableOps[A, Iterable, _])
	{
		/**
		  * @return Either
		  *             Left: The only item within this collection, if the size of this collection is exactly 1, or
		  *             Right: This collection
		  */
		def oneOrMany = if (ops.sizeCompare(1) == 0) Left(ops.head) else Right(coll)
		/**
		  * @return Returns one of three cases:
		  *             1) None - if this collection is empty,
		  *             2) Some(Left(item)) - If this collection only contains a single item, and
		  *             3) Some(Right(this)) - If this collection contains 2 or more items
		  */
		def emptyOneOrMany = if (ops.isEmpty) None else Some(oneOrMany)
	}
	
	implicit def iterableOperations2[Repr](coll: Repr)(implicit it: IsIterable[Repr]): IterableOperations2[it.A, Repr] =
		new IterableOperations2[it.A, Repr](coll, it(coll))
	
	implicit class RichIterableLike[A, CC[X], Repr <: Iterable[_]](val t: IterableOps[A, CC, Repr]) extends AnyVal
	{
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
		  * @param f       A function that handles the items
		  * @tparam B The type of another iterable's items
		  * @tparam U Arbitrary result type
		  */
		def foreachWith[B, U](another: Iterable[B])(f: (A, B) => U) = t.zip(another).foreach { p => f(p._1, p._2) }
	}
	
	implicit class RichIterable[A](val t: Iterable[A]) extends AnyVal
	{
		/**
		  * @return The first and the last item from this collection
		  */
		@throws[IllegalStateException]("If this collection is empty")
		def ends = Pair(t.head, t.last)
		/**
		  * @return The first and the last item from this collection. None if this collection is empty.
		  */
		def endsOption = if (t.isEmpty) Some(ends) else None
		
		/**
		  * @return An instance used for testing the size of this collection against fixed values and
		  *         sizes of other collections.
		  *         This function allows more effective size comparisons, compared to using .size == ...
		  */
		def hasSize = new HasSize(t)
		/**
		  * @return The only item in this collection.
		  *         None if this collection is empty or has more than one item.
		  */
		def only = if (hasSize(1)) Some(t.head) else None
		
		/**
		  * Converts this collection to a map by pairing each value with a map result key
		  * @param f A function that extracts a key from each item (expected to return unique results)
		  * @tparam K Type of keys used
		  * @return A new map where each item from this collection is mapped to a key
		  */
		def toMapBy[K](f: A => K) = Map.from(t.iterator.map { a => f(a) -> a })
		/**
		  * Converts this collection to a map by pairing each item (as key) with a map result value
		  * @param f A function that forms a value for each item
		  * @tparam V Type of map values
		  * @return A map where each item from this collection acts as a key to a mapping result value
		  */
		def mapTo[V](f: A => V): Map[A, V] = Map.from(t.iterator.map { a => a -> f(a) })
		
		/**
		  * @param end Targeted end of this collection
		  * @throws NoSuchElementException If this collection is empty
		  * @return The item at the specified end of this collection
		  */
		@throws[NoSuchElementException]("If this collection is empty")
		def apply(end: End) = end match {
			case First => t.head
			case Last => t.last
		}
		/**
		  * @param end Targeted end of this collection
		  * @return The item at the specified end of this collection. None if this collection is empty.
		  */
		def find(end: End) = end match {
			case First => t.headOption
			case Last => t.lastOption
		}
		
		/**
		  * @param extreme The targeted extreme
		  * @param ord     Implicit ordering to use
		  * @return The most extreme item in this collection
		  * @throws NoSuchElementException If this collection is empty
		  */
		@throws[NoSuchElementException]("This collection is empty")
		def apply(extreme: Extreme)(implicit ord: Ordering[A]) = extreme.from(t)
		/**
		  * @param extreme The targeted extreme
		  * @param ord     Implicit ordering to use
		  * @return The most extreme item in this collection. None if this collection is empty.
		  */
		def find(extreme: Extreme)(implicit ord: Ordering[A]) = extreme.optionFrom(t)
		
		/**
		  * @return Duplicate items within this Iterable
		  */
		def duplicates: Set[A] = {
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
		def maxOption[B >: A](implicit cmp: Ordering[B]): Option[A] = if (t.isEmpty) None else Some(t.max(cmp))
		/**
		  * Finds the minimum value in this Iterable
		  * @param cmp Ordering (implicit)
		  * @tparam B Ordering type
		  * @return Minimum item or None if this Iterable was empty
		  */
		def minOption[B >: A](implicit cmp: Ordering[B]): Option[A] = if (t.isEmpty) None else Some(t.min(cmp))
		/**
		  * @param ord Implicit ordering
		  * @return The minimum and the maximum value within this collection as a pair
		  */
		def minMax(implicit ord: Ordering[A]) = t.iterator.minMax
		/**
		  * @param ord Implicit ordering
		  * @return The minimum and the maximum value within this collection as a pair.
		  *         None if this collection is empty.
		  */
		def minMaxOption(implicit ord: Ordering[A]) = if (t.isEmpty) None else Some(minMax)
		
		/**
		  * Finds the maximum value based on map result
		  * @param map A mapping function
		  * @param cmp Implicit ordering
		  * @tparam B Type of map result
		  * @return Maximum item based on map result. None if this Iterable was empty
		  */
		def maxByOption[B](map: A => B)(implicit cmp: Ordering[B]): Option[A] =
			if (t.isEmpty) None else Some(t.maxBy(map))
		/**
		  * Finds the minimum value based on map result
		  * @param map A mapping function
		  * @param cmp Implicit ordering
		  * @tparam B Type of map result
		  * @return Minimum item based on map result. None if this Iterable was empty
		  */
		def minByOption[B](map: A => B)(implicit cmp: Ordering[B]): Option[A] =
			if (t.isEmpty) None else Some(t.minBy(map))
		/**
		  * @param extreme Targeted extreme (minimum or maximum)
		  * @param map Mapping function for acquiring compared values
		  * @param ord Implicit ordering to use
		  * @tparam B Type of compared values
		  * @return The minimum or maximum item from this collection, based on the specified mapping.
		  */
		def extremeByOption[B](extreme: Extreme)(map: A => B)(implicit ord: Ordering[B]): Option[A] = {
			if (t.isEmpty)
				None
			else
				Some(extreme match {
					case Min => t.minBy(map)
					case Max => t.maxBy(map)
				})
		}
		
		/**
		  * @param f A mapping function for ordered values
		  * @param ord Implicit ordering for mapping results
		  * @tparam B Type of mapping results
		  * @return The minimum and the maximum values of this collection, based on the specified mapping function.
		  */
		def minMaxBy[B](f: A => B)(implicit ord: Ordering[B]) = t.iterator.minMaxBy(f)
		/**
		  * @param f   A mapping function for ordered values
		  * @param ord Implicit ordering for mapping results
		  * @tparam B Type of mapping results
		  * @return The minimum and the maximum values of this collection, based on the specified mapping function.
		  *         None if this collection is empty.
		  */
		def minMaxByOption[B](f: A => B)(implicit ord: Ordering[B]) = if (t.isEmpty) None else Some(minMaxBy(f))
		
		/**
		  * @param end Targeted end of this collection
		  * @return Index of the specified end of this collection. -1 if this collection is empty.
		  */
		def indexOfEnd(end: End) = end match {
			case First => if (t.isEmpty) -1 else 0
			case Last => t.size - 1
		}
		
		/**
		  * @param other Another collection
		  * @return Whether these two collections have the same size
		  */
		def hasEqualSizeWith(other: Iterable[_]) = t.sizeCompare(other) == 0
		
		/**
		  * Tests whether these two collections are equal when using the specified equals function.
		  * The size, order and contents must match.
		  * @param other Another collection
		  * @param eq An equals function to use
		  * @tparam B Type of items in the other collection
		  * @return Whether these collections are equal
		  */
		def ~==[B >: A](other: Iterable[B])(implicit eq: EqualsFunction[B]) =
			hasSize.of(other) && t.iterator.zip(other.iterator).forall { case (a, b) => eq(a, b) }
		
		/**
		  * Compares this set of items with another set. Lists items that have been added and removed, plus the changes
		  * between items that have stayed
		  * @param another   Another Iterable
		  * @param connectBy A function for providing the unique key based on which items are connected
		  *                  (should be unique within each collection). Items sharing this key are connected.
		  * @param merge     A function for merging two connected items. Takes connection key, item in this collection and
		  *                  item in the other collection
		  * @tparam B     Type of items in the other collection
		  * @tparam K     Type of match key used
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
			
			val onlyInMe = (myKeys -- theirKeys).view.map { meByKey(_) }.toVector
			val onlyInThem = (theirKeys -- myKeys).view.map { theyByKey(_) }.toVector
			val merged = (myKeys & theirKeys).view.map { key => merge(key, meByKey(key), theyByKey(key)) }.toVector
			
			(onlyInMe, merged, onlyInThem)
		}
		
		/**
		  * Performs the specified mapping function until it succeeds or until all items in this collection have been
		  * tested
		  * @param f A mapping function which may fail
		  * @tparam B Type of map function result
		  * @return The first successful map result or failure if none of the items in this collection could be mapped
		  */
		def tryFindMap[B](f: A => Try[B]) = {
			val iter = t.iterator.map(f)
			if (iter.hasNext) {
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
		  * Checks whether this collection contains an item equal to the specified item
		  * @param item   A searched item
		  * @param equals Equals function to use (implicit)
		  * @tparam B Type of searched item
		  * @return Whether this collection contains an equal item
		  */
		def containsEqual[B >: A](item: B)(implicit equals: EqualsFunction[B]) = t.exists { equals(_, item) }
		/**
		  * @param items  Items to test
		  * @param equals Equals function to use (implicit)
		  * @tparam B Type of items collection
		  * @return Whether this collection of items contains all of the specified items
		  */
		def containsAll[B >: A](items: Iterable[B])(implicit equals: EqualsFunction[B] = EqualsFunction.default) =
			items.forall { containsEqual(_) }
	}
	
	
	// SEQ  --------------------------------------------
	
	class SeqOperations[Repr, S <: IsSeq[Repr]](coll: Repr, seq: S)
	{
		// ATTRIBUTES   ------------------------
		
		private lazy val ops = seq(coll)
		
		
		// OTHER    ----------------------------
		
		/**
		 * @param item An item to append to this collection, provided it is distinct
		 * @param buildFrom Implicit build-from
		 * @tparam B type of items in the resulting collection
		 * @tparam That Type of the resulting collection
		 * @return Copy of this collection that contains the specified item
		 */
		def appendIfDistinct[B >: seq.A, That](item: B)(implicit buildFrom: BuildFrom[Repr, B, That]): That = {
			if (ops.contains(item))
				buildFrom.fromSpecific(coll)(ops)
			else
				buildFrom.fromSpecific(coll)(ops :+ item)
		}
		/**
		 * Appends 0-n items to this collection, but only those which don't already appear in this collection
		 * @param items Items to append, if distinct
		 * @param buildFrom Implicit build-from
		 * @tparam B type of items in the resulting collection
		 * @tparam That Type of the resulting collection
		 * @return Copy of this collection where all elements from 'items', which don't yet appear in this collection,
		 *         have been appended.
		 */
		def appendAllIfDistinct[B >: seq.A, That](items: IterableOnce[B])
		                                         (implicit buildFrom: BuildFrom[Repr, B, That]): That =
		{
			val iter = items.iterator
			if (iter.hasNext) {
				val builder = buildFrom.newBuilder(coll)
				builder ++= iter.filterNot(ops.contains)
				builder.result()
			}
			else
				buildFrom.fromSpecific(coll)(seq(coll))
		}
		
		/**
		  * Maps a single item in this sequence
		  * @param index     The index that should be mapped
		  * @param f         A mapping function
		  * @param buildFrom A can build from (implicit)
		  * @return A copy of this sequence with the specified index mapped
		  */
		def mapIndex[B >: seq.A, That](index: Int)(f: seq.A => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That =
		{
			buildFrom.fromSpecific(coll)(new AbstractView[B] {
				override def iterator: AbstractIterator[B] = new AbstractIterator[B] {
					val it = ops.iterator
					var nextIndex = 0
					
					override def hasNext = it.hasNext
					
					// Passes items from the original iterator, except at specified index
					override def next() = {
						val result = {
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
		  * @param end Targeted collection end-point
		  * @param f Mapping function to apply to that end of this collection
		  * @param buildFrom Implicit build-from
		  * @tparam B Type of mapping result
		  * @tparam That Type of resulting collection
		  * @return Copy of this collection with the first or the last item mapped (unless empty)
		  */
		def mapEnd[B >: seq.A, That](end: End)(f: seq.A => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That = {
			if (ops.isEmpty)
				buildFrom.fromSpecific(coll)(Iterator.empty)
			else {
				val index = end match {
					case First => 0
					case Last => ops.size - 1
				}
				mapIndex[B, That](index)(f)
			}
		}
		/**
		  * @param f         Mapping function to apply to the first item of this collection
		  * @param buildFrom Implicit build-from
		  * @tparam B    Type of mapping result
		  * @tparam That Type of resulting collection
		  * @return Copy of this collection with the first item mapped (unless empty)
		  */
		def mapFirst[B >: seq.A, That](f: seq.A => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That =
			mapIndex[B, That](0)(f)
		/**
		  * @param f         Mapping function to apply to last item of this collection
		  * @param buildFrom Implicit build-from
		  * @tparam B    Type of mapping result
		  * @tparam That Type of resulting collection
		  * @return Copy of this collection with the last item mapped (unless empty)
		  */
		def mapLast[B >: seq.A, That](f: seq.A => B)(implicit buildFrom: BuildFrom[Repr, B, That]): That =
			mapEnd[B, That](Last)(f)
		
		/**
		  * Maps the first item that matches provided condition, leaves the other items as they were
		  * @param find      A function for finding the mapped item
		  * @param map       A mapping function for that item
		  * @param buildFrom A can build from for resulting collection (implicit)
		  * @return A copy of this sequence with specified item mapped. Returns this if no such item was found.
		  */
		def mapFirstWhere(find: seq.A => Boolean)(map: seq.A => seq.A)(implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): Repr =
		{
			ops.indexWhere(find) match {
				case index if index >= 0 => mapIndex(index)(map)
				case _ => coll
			}
		}
		
		/**
		  * Finds and removes an item from this sequence. If no item is found, no removal happens either.
		  * @param f A find function to find the item to remove.
		  * @param buildFrom A buildfrom for the filtered copy of this collection
		  * @return Either:
		  *             a) (Some(result), other items), if a result was found, or
		  *             b) (None, this collection), if no result was found
		  */
		def findAndPop[B >: seq.A](f: seq.A => Boolean)(implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): (Option[B], Repr) =
		{
			ops.indexWhere(f) match {
				case index if index >= 0 => Some(ops(index)) -> _withoutIndex(ops.iterator, index)
				case _ => None -> coll
			}
		}
		
		/**
		  * @param index     Targeted index
		  * @param buildFrom A build from (implicit)
		  * @return A copy of this sequence without specified index
		  */
		def withoutIndex(index: Int)(implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): Repr = {
			if (index < 0)
				coll
			else {
				if (ops.sizeCompare(index) <= 0)
					coll
				else
					_withoutIndex(ops.iterator, index)
			}
		}
		private def _withoutIndex(iter: Iterator[seq.A], index: Int)
		                         (implicit buildFrom: BuildFrom[Repr, seq.A, Repr]): Repr =
			buildFrom.fromSpecific(coll)(iter.zipWithIndex.flatMap { case (a, i) => if (i == index) None else Some(a) })
		
		/**
		  * Performs specified operation for each item in this sequence. Called function will also receive item index
		  * in this sequence
		  * @param f A function called for each item
		  * @tparam U Arbitrary result type
		  */
		def foreachWithIndex[U](f: (seq.A, Int) => U) = {
			val seqOps = seq(coll)
			seqOps.zipWithIndex.foreach { case (item, index) => f(item, index) }
		}
		
		/**
		  * Takes items from right to left as long as the specified condition holds
		  * @param f         A function that determines whether an item is accepted to the final collection
		  * @param buildFrom A build from (implicit)
		  * @tparam That Resulting collection type
		  * @return A collection that contains the collected items in the same order as they appear in this
		  *         collection (left to right)
		  */
		def takeRightWhile[That](f: seq.A => Boolean)(implicit buildFrom: BuildFrom[Repr, seq.A, That]): That = {
			val seqOps = seq(coll)
			// Collects the items to a buffer first in order to reverse the order afterwards
			val bufferBuilder = new VectorBuilder[seq.A]()
			seqOps.reverseIterator.takeWhile(f).foreach { bufferBuilder += _ }
			buildFrom.fromSpecific(coll)(bufferBuilder.result().reverse)
		}
		
		/**
		  * Groups consecutive items together (from left to right) based on a custom inclusion function.
		  * May only join consecutive items together.
		  * @param f A function used for determining whether the next item should be included in the specified group.
		  *          Whenever this function returns false, a new group is started.
		  * @param bf A build-from for the resulting collection containing the collected groups
		  * @tparam To Type of the resulting collection
		  * @return Collected groups
		  */
		def groupConsecutiveWith[To](f: (Seq[seq.A], seq.A) => Boolean)
		                            (implicit bf: BuildFrom[Repr, Seq[seq.A], To]) =
		{
			// Case: This sequence contains less than 2 items => No grouping is required
			if (ops.sizeCompare(2) < 0)
				bf.fromSpecific(coll)(ops.headOption.map { Single(_) })
			// Case: 2 or more items available => Performs grouping
			else {
				// Collects here the completed groups
				val groupsBuilder = bf.newBuilder(coll)
				// Tests one item at a time
				val lastGroup = ops.view.tail.foldLeft[IndexedSeq[seq.A]](Single(ops.head)) { (building, next) =>
					// Case: Item may be added to this group => Continues building it
					if (f(building, next))
						building :+ next
					// Case: Item may not be added => Completes the group and starts a new one
					else {
						groupsBuilder += building
						Single(next)
					}
				}
				groupsBuilder += lastGroup
				groupsBuilder.result()
			}
		}
	}
	
	implicit def seqOperations[Repr](coll: Repr)(implicit seq: IsSeq[Repr]): SeqOperations[Repr, seq.type] =
		new SeqOperations(coll, seq)
	
	implicit class RichSeqLike[A, CC[_], Repr](val seq: SeqOps[A, CC, Repr]) extends AnyVal
	{
		/**
		  * @param ord Implicit ordering to use after it has been reversed
		  * @return A copy of this collection that's implicitly ordered, but in reverse
		  */
		def reverseSorted(implicit ord: Ordering[A]) = seq.sorted(ord.reverse)
		
		/**
		  * @return A version of this seq with consecutive items paired. Each item will be present twice in the returned
		  *         collection, except the first and the last item. The first item will be presented once as the first
		  *         argument. The last item will be presented once as the second argument. If this sequence
		  *         contains less than two items, an empty seq is returned.
		  */
		def paired =
			(1 until seq.size).view.map { i => Pair(seq(i - 1), seq(i)) }.toOptimizedSeq
		
		/**
		  * Same as apply except returns None on non-existing indices
		  * @param index Target index
		  * @return Value from index or None if no such index exists
		  */
		def getOption(index: Int) = if (seq.isDefinedAt(index)) Some(seq(index)) else None
		/**
		  * Same as apply except returns a default value on non-existing indices
		  * @param index   Target index
		  * @param default Default value
		  * @return Value from index or default value if no such index exists
		  */
		def getOrElse(index: Int, default: => A) = if (seq.isDefinedAt(index)) seq(index) else default
		
		/**
		  * Sorts this collection using mapping results.
		  * Reverses the applied implicit ordering.
		  * @param f A mapping function
		  * @param ord Implicit ordering to use for mapping results (will be reversed)
		  * @tparam B Type of mapping results
		  * @return A sorted copy of this collection
		  */
		def reverseSortBy[B](f: A => B)(implicit ord: Ordering[B]) = seq.sortBy(f)(ord.reverse)
		
		/**
		  * @param end The targeted end and length
		  * @return The first or last n items of this collection
		  */
		def take(end: EndingSequence): Repr = end.end match {
			case First => seq.take(end.length)
			case Last => seq.takeRight(end.length)
		}
		/**
		  * @param end The targeted end and length
		  * @return This collection without the first or last n items of this collection
		  */
		def drop(end: EndingSequence): Repr = end.end match {
			case First => seq.drop(end.length)
			case Last => seq.dropRight(end.length)
		}
		
		/**
		  * Drops items from the right as long as the specified condition returns true
		  * @param f A function that tests whether items should be dropped
		  * @return A copy of this collection with rightmost items (that satisfy provided predicate) removed
		  */
		def dropRightWhile(f: A => Boolean) = seq.lastIndexWhere { !f(_) } match {
			case index if index >= 0 => seq.take(index + 1)
			case _ => seq.take(0)
		}
		
		/**
		  * @param range Range to slice from this sequence
		  * @return Slice of this sequence
		  */
		def slice(range: Range): Repr = {
			if (range.nonEmpty)
				seq.slice(range.head, range.last + 1)
			else
				seq.empty
		}
		/**
		 * @param range The start and the end indices
		 * @return A sequence within this sequence that matches the specified range
		 */
		def slice(range: HasEnds[Int]): Repr =
			seq.slice(range.start, if (range.isInclusive) range.end + 1 else range.end)
		
		/**
		  * Forms pairs based on the contents of this collection
		  * @param start The first element of the first pair
		  * @tparam B Type of the items returned
		  * @return A collection that contains the consecutive items of this collection as pairs.
		  *         E.g. If this collection contains elements A, B and C and 'start' is defined as O,
		  *         the resulting collection would be: [OA, AB, BC].
		  */
		def pairedFrom[B >: A](start: => B) = seq.headOption match {
			case Some(first) => Pair(start, first) +: paired
			case None => Empty
		}
		/**
		  * Forms pairs based on the contents of this collection
		  * @param end The last element of the last pair
		  * @tparam B Type of the items returned
		  * @return A collection that contains the consecutive items of this collection as pairs.
		  *         E.g. If this collection contains elements A, B and C and 'end' is defined as E,
		  *         the resulting collection would be: [AB, BC, CE].
		  */
		def pairedTo[B >: A](end: => B) = seq.lastOption match {
			case Some(last) => paired :+ Pair(last, end)
			case None => Empty
		}
		/**
		  * Forms pairs based on the contents of this collection
		  * @param ends The first element of the first pair and the last element of the last pair to return
		  * @tparam B Type of the elements in the resulting collection
		  * @return A collection that contains consecutive items of this collections as pairs.
		  *         E.g. If this collection contains the elements A, B and C and 'ends' are defined as S and E,
		  *         the resulting collection would be: [SA, AB, BC, CE].
		  */
		def pairedBetween[B >: A](ends: Pair[B]) = {
			if (seq.isEmpty)
				Single(ends)
			else
				Pair(ends.first, seq.head) +: paired :+ Pair(seq.last, ends.second)
		}
		
		/**
		  * Sorts this collection based on multiple orderings (second ordering is only used if first one fails to
		  * differentiate the items, then third and so on)
		  * @param firstOrdering  The first ordering to use
		  * @param secondOrdering The second ordering to use
		  * @param moreOrderings  More orderings to use
		  * @return A sorted copy of this collection
		  */
		def sortedWith(firstOrdering: Ordering[A], secondOrdering: Ordering[A], moreOrderings: Ordering[A]*) =
			seq.sorted(new CombinedOrdering[A](Pair(firstOrdering, secondOrdering) ++ moreOrderings))
		
		/**
		  * Performs a map operation until a non-empty value is returned. Returns both the mapped value and the mapped index.
		  * @param f A mapping function
		  * @tparam B Type of map result
		  * @return The first non-empty map result, along with the index of the mapped item. None if all items were
		  *         mapped to None.
		  */
		def findMapAndIndex[B](f: A => Option[B]) =
			seq.indices.view.flatMap { i => f(seq(i)).map { _ -> i } }.headOption
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
		  * @param item  An item to insert
		  * @param index Index where the item is inserted, where 0 is the first position
		  * @tparam B Type of resulting collection's items
		  * @return A copy of this collection with the item inserted
		  */
		def inserted[B >: A](item: B, index: Int): CC[B] = {
			if (index <= 0)
				seq.prepended(item)
			else if (index >= seq.size)
				seq.appended(item)
			else {
				val (beginning, end) = seq.splitAt(index)
				(beginning :+ item) ++ end
			}
		}
	}
	
	implicit class RichSeq[A](val s: Seq[A]) extends AnyVal
	{
		/**
		  * @return A random item from this collection
		  */
		def random = s.apply(Random.nextInt(s.size))
		
		/**
		  * @param end Targeted end of this collection
		  * @return The index of the element at that end of this collection. Out of bounds if this collection is empty.
		  */
		def indexOf(end: End) = end.indexFrom(s)
		/**
		 * Finds the index of the specified item
		 * @param item Searched item
		 * @tparam B Item type
		 * @return The index of specified item or none if no such index was found
		 */
		def findIndexOf[B >: A](item: B) = {
			val result = s.indexOf(item)
			if (result >= 0)
				Some(result)
			else
				None
		}
		/**
		 * Finds the index of the specified item
		 * @param item Searched item
		 * @tparam B Item type
		 * @return The index of specified item or none if no such index was found
		 */
		@deprecated("Renamed to .findIndexOf(...)", "v2.2")
		def optionIndexOf[B >: A](item: B) = findIndexOf(item)
		
		/**
		 * @param f A function that returns true for the targeted item
		 * @return Index for which the specified function returned true.
		 *         None if no the function didn't return true.
		 */
		def findIndexWhere(f: A => Boolean) =
			s.iterator.zipWithIndex.find { case (a, _) => f(a) }.map { _._2 }
		/**
		 * @param f A function that returns true for the targeted item
		 * @return Index of the last / rightmost item that satisfies the specified condition.
		 *         None if no such item was found.
		 */
		def findLastIndexWhere(f: A => Boolean) = Some(s.lastIndexWhere(f)).filter { _ >= 0 }
		/**
		 * Finds the index of the first item that matches the predicate
		 * @param find a function for finding the correct item
		 * @return The index of the item in this seq or None if no such item was found
		 */
		@deprecated("Renamed to .findIndexWhere(...)", "v2.2")
		def indexWhereOption(find: A => Boolean) = findIndexWhere(find)
		/**
		 * Finds the index of the last item that matches the predicate
		 * @param find a function for finding the correct item
		 * @return The index of the item in this seq or None if no such item was found
		 */
		@deprecated("Renamed to .findLastIndexWhere(...)", "v2.2")
		def lastIndexWhereOption(find: A => Boolean) = findLastIndexWhere(find)
		
		/**
		  * @param f     A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the largest value when mapped
		  * @throws NoSuchElementException If this sequence is empty
		  */
		@throws[NoSuchElementException]("Throws when called for an empty sequence")
		def maxIndexBy[B](f: A => B)(implicit order: Ordering[B]) = {
			var maxIndex = 0
			var maxResult = f(s.head)
			s.indices.drop(1).foreach { index =>
				val result = f(s(index))
				if (order.compare(result, maxResult) > 0) {
					maxIndex = index
					maxResult = result
				}
			}
			maxIndex
		}
		/**
		  * @param f     A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the smallest value when mapped
		  * @throws NoSuchElementException If this sequence is empty
		  */
		@throws[NoSuchElementException]("Throws when called for an empty sequence")
		def minIndexBy[B](f: A => B)(implicit order: Ordering[B]) = maxIndexBy(f)(order.reverse)
		/**
		  * @param f     A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the largest value when mapped.
		  *         None if this sequence is empty.
		  */
		def maxOptionIndexBy[B](f: A => B)(implicit order: Ordering[B]) =
			if (s.isEmpty) None else Some(maxIndexBy(f))
		/**
		  * @param f     A mapping function
		  * @param order Ordering for mapped values
		  * @tparam B Type of map result
		  * @return The index in this sequence that contains the smallest value when mapped.
		  *         None if this sequence is empty.
		  */
		def minOptionIndexBy[B](f: A => B)(implicit order: Ordering[B]) =
			maxOptionIndexBy(f)(order.reverse)
	}
	
	implicit class RichIndexedSeq[A](val s: IndexedSeq[A]) extends AnyVal
	{
		/**
		  * @param f A mapping function
		  * @tparam B Type of the map results
		  * @return A copy of this collection with lazily mapped items
		  */
		def lazyMap[B](f: A => B) = LazyVector(s.map { a => Lazy(f(a)) })
	}
	
	
	// ITERATOR ------------------------------------------
	
	implicit class RichIterator[A](val i: Iterator[A]) extends AnyVal
	{
		/**
		  * @return This iterator if not empty (i.e. has more elements), otherwise None.
		  */
		def notEmpty = if (i.hasNext) Some(i) else None
		
		/**
		  * Enables polling on this iterator. This method yields a new iterator.
		  * This iterator shouldn't be used after the copy has been acquired. Only the pollable copy of this
		  * iterator should be used afterwards.
		  * @return A copy of this iterator that allows polling (checking of the next item without advancing)
		  */
		def pollable = PollingIterator.from[A](i)
		
		/**
		  * @return Copy of this iterator which skips values
		  *         that would have been identical between two consecutive iterations.
		  *         E.g. If the original form of this iterator would have returned [1, 1, 2, 1, 1],
		  *         this resulting iterator would return [1, 2, 1].
		  */
		def consecutivelyDistinct = ConsecutivelyDistinctIterator[A](i)
		
		/**
		  * Retrieves the first and the last item from this iterator.
		  * This consumes this whole iterator.
		  * Will not terminate for infinite iterators.
		  * @return The first and the last item from this iterator
		  */
		@throws[IllegalStateException]("If this iterator is empty")
		def ends = {
			val first = i.next()
			if (i.hasNext)
				Pair(first, i.last)
			else
				Pair.twice(first)
		}
		/**
		  * Retrieves the first and the last item from this iterator.
		  * This consumes this whole iterator.
		  * Will not terminate for infinite iterators.
		  * @return The first and the last item from this iterator.
		  *         None if this iterator was empty.
		  */
		def endsOption = if (i.hasNext) Some(ends) else None
		/**
		  * Finds the minimum and the maximum values from this iterator.
		  * NB: Consumes all items within this iterator. Will not terminate for infinite iterators.
		  * @param ord Implicit ordering
		  * @return The minimum and the maximum value found from this iterator
		  */
		def minMax(implicit ord: Ordering[A]) = {
			val first = i.next()
			var currentMin = first
			var currentMax = first
			i.foreach { a =>
				if (a < currentMin)
					currentMin = a
				else if (a > currentMax)
					currentMax = a
			}
			Pair(currentMin, currentMax)
		}
		/**
		  * Finds the minimum and the maximum values from this iterator.
		  * NB: Consumes all items within this iterator. Will not terminate for infinite iterators.
		  * @param ord Implicit ordering
		  * @return The minimum and the maximum value found from this iterator.
		  *         None if this iterator didn't contain any more items.
		  */
		def minMaxOption(implicit ord: Ordering[A]) = if (i.hasNext) Some(minMax) else None
		/**
		  * Finds the minimum and the maximum values from this iterator.
		  * NB: Consumes all items within this iterator. Will not terminate for infinite iterators.
		  * @param f A mapping function that determines the values for ordering
		  * @param ord Implicit ordering
		  * @return The minimum and the maximum value found from this iterator
		  */
		def minMaxBy[B](f: A => B)(implicit ord: Ordering[B]) = minMax(Ordering.by(f))
		/**
		  * Finds the minimum and the maximum values from this iterator.
		  * NB: Consumes all items within this iterator. Will not terminate for infinite iterators.
		  * @param f   A mapping function that determines the values for ordering
		  * @param ord Implicit ordering
		  * @return The minimum and the maximum value found from this iterator.
		  *         None if this iterator is empty.
		  */
		def minMaxByOption[B](f: A => B)(implicit ord: Ordering[B]) =
			if (i.hasNext) Some(minMaxBy(f)) else None
		
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
		def paired = PairingIterator(i)
		
		/**
		 * Creates a copy of this iterator that asynchronously buffers the next n items before they're requested
		 * @param prePollCount Number of items to poll in advance, at maximum
		 * @param exc Implicit execution context used in asynchronous polling
		 * @param logger Implicit logger used for recording non-critical failures
		 * @return A buffering / pre-polling copy of this iterator
		 */
		def prePollingAsync(prePollCount: Int)(implicit exc: ExecutionContext, logger: Logger) =
			new PrePollingIterator(i, prePollCount)
		
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
		  * @param f     A function for testing each item
		  * @return Whether 'f' returned true for 'count' items. Doesn't test whether 'f' would return true for more
		  *         than 'count' items.
		  */
		def existsCount(count: Int)(f: A => Boolean) = {
			var found = 0
			while (found < count && i.hasNext) {
				if (f(i.next()))
					found += 1
			}
			found >= count
		}
		
		/**
		  * Skips the next 'n' items in this iterator
		  * @param n Number of items to skip (default = 1)
		  */
		def skip(n: Int = 1) = {
			var skipped = 0
			while (skipped < n && i.hasNext) {
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
		  * @param n         The maximum number of iterations / items handled
		  * @param operation Operation called for each handled item
		  * @tparam U Arbitrary operation result type (not used)
		  * @return Whether the full 'n' items were handled. If false, the end of this iterator was reached.
		  */
		def forNext[U](n: Int)(operation: A => U) = {
			var consumed = 0
			while (i.hasNext && consumed < n) {
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
		def collectNext(n: Int) = {
			var consumed = 0
			val builder = new VectorBuilder[A]()
			while (i.hasNext && consumed < n) {
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
		def collectTo(stopCondition: A => Boolean) = {
			val builder = new VectorBuilder[A]()
			var found = false
			while (i.hasNext && !found) {
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
		def nextWhere(condition: A => Boolean) = {
			if (i.hasNext) {
				var current = i.next()
				var foundResult = condition(current)
				while (!foundResult && i.hasNext) {
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
		def findMapNext[B](map: A => Option[B]) = {
			var current: Option[B] = None
			while (current.isEmpty && i.hasNext) {
				current = map(i.next())
			}
			current
		}
		
		/**
		  * Groups this iterator and performs the specified operation for each of the collected groups.
		  * Differs from .group(...).foreach(...) in that this method acts on all of the items in this iterator
		  * without discarding the possible smaller group at the end
		  * @param maxGroupSize Maximum number of items for a function call
		  * @param f            A function that is called for each group of items
		  */
		def foreachGroup(maxGroupSize: Int)(f: Vector[A] => Unit) = {
			while (i.hasNext) {
				f(collectNext(maxGroupSize))
			}
		}
		
		/**
		  * Maps the items in this iterator, one group at a time
		  * @param groupSize The maximum size of an individual group of items to map
		  * @param map       a mapping function applied to groups of items
		  * @tparam B Type of map result
		  * @return All map results in order
		  */
		def groupMap[B](groupSize: Int)(map: Vector[A] => B) = {
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
		  * @param f      A mapping function
		  * @param logger A logger that will receive possibly thrown exceptions
		  * @tparam B Type of successful map result
		  * @return Iterator of the mapped items
		  */
		def mapCatching[B](f: A => Try[B])(implicit logger: Logger) = {
			i.flatMap { original =>
				f(original) match {
					case Success(item) => Some(item)
					case Failure(error) =>
						logger(error)
						None
				}
			}
		}
		
		/**
		  * @param start The prepended pair start point (call-by-name).
		  *              This will never be called if this iterator is empty.
		  * @tparam B Type of pair parts
		  * @return A copy of this iterator that returns items as pairs, with the 'start' prepended.
		  *         E.g. If this iterator contained items [A, B, C] and start was X, the resulting iterator would
		  *         return [XA, AB, BC]
		  */
		def pairedFrom[B >: A](start: => B) = new PairingIterator[B](start, i)
		/**
		  * @param end The appended pair end point (call-by-name). I.e. the last value of the last returned pair.
		  *            This will never be called if this iterator is empty.
		  * @tparam B Type of pair parts
		  * @return A copy of this iterator that returns items as pairs, with the 'end' appended.
		  *         E.g. If this iterator contained items [A, B, C] and end was X, the resulting iterator would
		  *         return [AB, BC, CX]
		  */
		def pairedTo[B >: A](end: => B) = PairingIterator.to(i, end)
		/**
		  * Creates an iterator that returns the consecutive items in this collection as pairs.
		  * @param start  The first element of the first returned Pair
		  * @param end    The second element of last returned Pair
		  * @tparam B Type of the returned items
		  * @return A new pairing iterator based on the elements of this collection.
		  *         E.g. If this collection contains elements A, B and C, 'start' is S and 'end' is E,
		  *         the resulting iterator would return SA, AB, BC and CE.
		  */
		def pairedBetween[B >: A](start: => B, end: => B) = PairingIterator.between(start, i, end)
		/**
		  * Creates an iterator that returns the consecutive items in this collection as pairs.
		  * @param ends The first element of the first pair and the second element of the last pair
		  * @tparam B Type of the returned items
		  * @return A new pairing iterator based on the elements of this collection.
		  *         E.g. If this collection contains elements A, B and C, and 'ends' is SE,
		  *         the resulting iterator would return SA, AB, BC and CE.
		  */
		def pairedBetween[B >: A](ends: Pair[B]): Iterator[Pair[B]] = pairedBetween(ends.first, ends.second)
		
		/**
		  * Zips this iterator with another, possibly padding one of them.
		  * Neither of these two source iterators should be used afterwards.
		  * @param other        Another iterator
		  * @param myPadding    Padding to use for this iterator (call-by-name)
		  * @param theirPadding Padding to use for the other iterator (call-by-name)
		  * @tparam B Type of items in the other iterator
		  * @return An iterator that takes from both of these iterators and zips the results,
		  *         padding if one depletes before the other
		  */
		def zipPad[B](other: Iterator[B], myPadding: => A, theirPadding: => B) =
			ZipPadIterator(i, other, myPadding, theirPadding)
		
		/**
		  * Zips this iterator with another, possibly padding one of them.
		  * Neither of these two source iterators should be used afterwards.
		  * @param other   Another iterator
		  * @param padding Padding to use for the iterator that depletes first (call-by-name)
		  * @return An iterator that takes from both of these iterators and zips the results,
		  *         padding if one depletes before the other
		  */
		def zipPad(other: Iterator[A], padding: => A) = ZipPadIterator(i, other, padding)
		
		/**
		  * Creates a copy of this iterator that supports change events
		  * @param initialValue The value of the resulting iterator until next() is called for the first time
		  * @tparam B Type of items in the resulting iterator
		  * @return A copy of this iterator that supports change events
		  */
		def eventful[B >: A](initialValue: B)(implicit log: Logger) =
			EventfulIterator[B](initialValue, i)
		/**
		  * Creates a copy of this iterator that supports change events
		  * @param initialValue The value of the resulting iterator until next() is called for the first time
		  * @tparam B Type of items in the resulting iterator
		  * @return A copy of this iterator that supports change events
		  */
		@deprecated("Renamed to .eventful(B)", "v2.2")
		def withEvents[B >: A](initialValue: B)(implicit log: Logger) = new EventfulIterator[B](initialValue, i)
	}
	
	implicit class TryIterator[A](val i: Iterator[Try[A]]) extends AnyVal
	{
		/**
		 * Iterates until the first success is encountered.
		 * If this iterator doesn't contain a single success, iterates over all items.
		 * @return The first success that was encountered,
		 *         including any failures that were encountered before that success.
		 *         If no successes were encountered, returns the last encountered failure.
		 *         Also fails if this iterator is empty
		 */
		def trySucceedOnce: TryCatch[A] = {
			val results = i.iterator.collectTo { _.isSuccess }
			results.lastOption match {
				case Some(lastResult) =>
					lastResult match {
						case Success(a) => TryCatch.Success(a, results.flatMap { _.failure })
						case Failure(error) => TryCatch.Failure(error)
					}
				case None => TryCatch.Failure(new IllegalStateException("trySucceedOnce called for an empty iterator"))
			}
		}
		
		/**
		 * @param f A mapping function performed for successful elements
		 * @tparam B Mapping result type
		 * @return Copy of this iterator where all successful elements are mapped (lazily)
		 */
		def mapSuccesses[B](f: A => B): Iterator[Try[B]] = i.map { _.map(f) }
		/**
		 * @param f A mapping function that may yield a failure
		 * @tparam B Type of mapping results, when successful
		 * @return Copy of this iterator where successful results are mapped using the specified function.
		 *         The mapping is performed on-call only.
		 */
		def flatMapSuccesses[B](f: A => Try[B]): Iterator[Try[B]] = i.map {
			case Success(item) => f(item)
			case Failure(error) => Failure(error)
		}
	}
	
	
	// OTHER    ------------------------------------------
	
	implicit class RichOption[A](val o: Option[A]) extends AnyVal
	{
		/**
		  * @return Either a collection that contains a single value, or one which contains no values
		  */
		def emptyOrSingle = o match {
			case Some(v) => Single(v)
			case None => Empty
		}
		
		/**
		  * Converts this option to a try
		  * @param generateFailure A function for generating a throwable for a failure if one is needed
		  * @return Success with this option's value or failure if this option was empty
		  */
		def toTry(generateFailure: => Throwable) = o match {
			case Some(v) => Success(v)
			case None => Failure(generateFailure)
		}
		
		/**
		  * Merges this option with another option using the following logic:
		  * 1) If neither of these options are defined, returns None
		  * 2) If only one of these options is defined, returns that option
		  * 3) If both of these options are defined, merges the two values into a new option
		  * @param other Another option
		  * @param merge Function to use when both of these options contain a value
		  * @tparam B Type of the other option (super-type of this option)
		  * @return Merge result, as described above
		  */
		def mergeWith[B >: A](other: Option[B])(merge: (A, B) => B) = o match {
			case Some(a) =>
				other match {
					case Some(b) => Some(merge(a, b))
					case None => Some(a)
				}
			case None => other
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
		 * @return A TryCatch instance based on this Try
		 */
		def toTryCatch: TryCatch[A] = t match {
			case Success(a) => TryCatch.Success(a)
			case Failure(e) => TryCatch.Failure(e)
		}
		
		/**
		 * Logs the captured failure, if applicable
		 * @param log Logging implementation to use
		 */
		def logFailure(implicit log: Logger) = failure.foreach { log(_) }
		/**
		 * Logs the captured failure, if applicable
		 * @param message Message to record with the failure (call-by-name)
		 * @param log Logging implementation to use
		 */
		def logFailureWithMessage(message: => String)(implicit log: Logger) = failure.foreach { log(_, message) }
		
		/**
		 * Converts this try into an option. Logs possible failure state.
		 * @param log Implicit logger to use to log the potential failure.
		 * @return Some if success, None otherwise
		 */
		def logToOption(implicit log: Logger) = t match {
			case Success(a) => Some(a)
			case Failure(error) =>
				log(error)
				None
		}
		/**
		 * Converts this try into an option. Logs possible failure state.
		 * @param message Message to log in case of a failure (call-by-name)
		 * @param log Implicit logger to use to log the potential failure.
		 * @return Some if success, None otherwise
		 */
		def logToOptionWithMessage(message: => String)(implicit log: Logger) = t match {
			case Success(a) => Some(a)
			case Failure(error) =>
				log(error, message)
				None
		}
		
		/**
		  * @param f A mapping function for possible failure
		  * @tparam B Result type
		  * @return Contents of this try on success, mapped error on failure
		  */
		def getOrMap[B >: A](f: Throwable => B): B = t match {
			case Success(item) => item
			case Failure(error) => f(error)
		}
		/**
		 * Returns the success value or logs the error and returns a placeholder value
		 * @param f A function for generating the returned value in case of a failure
		 * @param log Implicit logging implementation for encountered errors
		 * @tparam B Result type
		 * @return Successful contents of this try, or the specified placeholder value
		 */
		def getOrElseLog[B >: A](f: => B)(implicit log: Logger): B = getOrMap { error =>
			log(error)
			f
		}
		
		/**
		  * @param f A function called if this is a success
		  * @param log Implicit logger to record a possible failure with
		  * @tparam U Arbitrary function result type
		  */
		def foreachOrLog[U](f: A => U)(implicit log: Logger): Unit = t match {
			case Success(a) => f(a)
			case Failure(error) => log(error)
		}
		
		/**
		  * Converts this Try into an Option.
		  * Handles the possible failure case using the specified function.
		  * @param f A function that handles the possible failure case
		  * @tparam U Arbitrary function result type
		  * @return Some if this was a success, None of failure.
		  */
		def handleFailure[U](f: Throwable => U) = t match {
			case Success(v) => Some(v)
			case Failure(error) => f(error); None
		}
		
		/**
		  * Maps the value of this Try, if successful.
		  * @param f A mapping function that accepts a successfully acquired value and returns a
		  *          TryCatch instance.
		  * @tparam B Type of the success value in the map function result
		  * @return Success containing the mapping result and the possible non-critical failures,
		  *         or a failure.
		  */
		def flatMapCatching[B](f: A => TryCatch[B]) = t match {
			case Success(v) => f(v)
			case Failure(e) => TryCatch.Failure(e)
		}
		
		/**
		 * @param error A (secondary) error
		 * @tparam B Type of failure to yield
		 * @return This if failure, otherwise a failure based on the specified error
		 */
		def failWith[B](error: Throwable) = t match {
			case Success(_) => Failure[B](error)
			case Failure(e) => Failure[B](e)
		}
		/**
		 * @param error A potential error (call-by-name, not called if this is a failure already)
		 * @return Success only if this is a success and the specified error is None.
		 *         Failure otherwise, preferring an existing failure in this Try, if applicable.
		 */
		def failIf(error: => Option[Throwable]) = {
			t match {
				case Success(v) =>
					error match {
						// Case: This was a success but the specified function failed => Fails
						case Some(e) => Failure(e)
						// Case: This was a success and the specified function didn't fail => Success
						case None => Success(v)
					}
				// Case: This was already a failure => Fails
				case Failure(e) => Failure(e)
			}
		}
	}
	
	implicit class RichTryTryCatch[A](val t: Try[TryCatch[A]]) extends AnyVal
	{
		/**
		 * @return Flattened copy of this 2-level try into a single TryCatch instance
		 */
		def flattenCatching: TryCatch[A] = t.getOrMap { TryCatch.Failure(_) }
	}
	
	implicit class RichEither[L, R](val e: Either[L, R]) extends AnyVal
	{
		/**
		  * @return This either's left value or None if this either is right
		  */
		def leftOption = e match {
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
		def mapLeft[B](f: L => B) = e match {
			case Right(r) => Right(r)
			case Left(l) => Left(f(l))
		}
		/**
		  * If this either is right, maps it
		  * @param f A mapping function for left side
		  * @tparam B New type for right side
		  * @return A mapped version of this either
		  */
		def mapRight[B](f: R => B) = e match {
			case Right(r) => Right(f(r))
			case Left(l) => Left(l)
		}
		
		/**
		  * @param f A mapping function applied if this is left.
		  *          Returns a new either.
		  * @tparam L2 Type of left in the mapping result.
		  * @tparam R2 Type of the resulting right type.
		  * @return This if right, mapping result if left
		  */
		def divergeMapLeft[L2, R2 >: R](f: L => Either[L2, R2]) = e match {
			case Right(r) => Right(r)
			case Left(l) => f(l)
		}
		/**
		  * @param f A mapping function applied if this is right.
		  *          Returns a new either.
		  * @tparam L2 Type of the resulting left type.
		  * @tparam R2 Type of right in the mapping result.
		  * @return This if left, mapping result if right
		  */
		def divergeMapRight[L2 >: L, R2](f: R => Either[L2, R2]) = e match {
			case Right(r) => f(r)
			case Left(l) => Left(l)
		}
		
		/**
		  * @param f A mapping function for left values
		  * @tparam B Type of map result
		  * @return Right value or the mapped left value
		  */
		def rightOrMap[B >: R](f: L => B) = e match {
			case Right(r) => r
			case Left(l) => f(l)
		}
		/**
		  * @param f A mapping function for right values
		  * @tparam B Type of map result
		  * @return Left value or the mapped right value
		  */
		def leftOrMap[B >: L](f: R => B) = e match {
			case Right(r) => f(r)
			case Left(l) => l
		}
		
		/**
		  * Maps the value of this either to a single value, whichever side this is
		  * @param leftMap  Mapping function used when left value is present
		  * @param rightMap Mapping function used when right value is present
		  * @tparam B Resulting item type
		  * @return Mapped left or mapped right
		  */
		def mapToSingle[B](leftMap: L => B)(rightMap: R => B) = e match {
			case Right(r) => rightMap(r)
			case Left(l) => leftMap(l)
		}
		/**
		  * Maps this either, no matter which side it is
		  * @param leftMap  Mapping function used when this either is left
		  * @param rightMap Mapping function used when this either is right
		  * @tparam L2 New left type
		  * @tparam R2 New right type
		  * @return A mapped version of this either (will have same side)
		  */
		def mapBoth[L2, R2](leftMap: L => L2)(rightMap: R => R2) = e match {
			case Right(r) => Right(rightMap(r))
			case Left(l) => Left(leftMap(l))
		}
	}
	
	implicit class RichSingleTypeEither[A](val e: Either[A, A]) extends AnyVal
	{
		/**
		  * @return Left or right side value, whichever is defined
		  */
		def either = e match {
			case Left(l) => l
			case Right(r) => r
		}
		/**
		  * @return The left or the right side value, plus the side from which the item was found.
		  *         First represents the left side and Last represents the right side.
		  */
		def eitherAndSide: (A, End) = e match {
			case Left(l) => l -> First
			case Right(r) => r -> Last
		}
		
		/**
		  * @return A pair based on this either, where the non-occupied side receives None and the occupied side
		  *         receives Some
		  */
		def toPair = e match {
			case Left(l) => Pair(Some(l), None)
			case Right(r) => Pair(None, Some(r))
		}
		
		/**
		  * @param f A mapping function
		  * @tparam B Mapping result type
		  * @return Mapping result, keeping the same side
		  */
		def mapEither[B](f: A => B) = e match {
			case Left(l) => Left(f(l))
			case Right(r) => Right(f(r))
		}
		/**
		  * Maps the value in this either, but only if the the value resides on the specified side
		  * @param side The side to map (if applicable), where First represents left and Last represents Right
		  * @param f A mapping function to use, if applicable
		  * @tparam B Type of mapping result
		  * @return Either this either, if the value resided on the opposite side, or a mapped copy of this either
		  */
		def mapSide[B >: A](side: End)(f: A => B) = e match {
			case Left(l) => if (side == First) Left(f(l)) else e
			case Right(r) => if (side == Last) Right(f(r)) else e
		}
		/**
		  * @param f A mapping function
		  * @tparam B Mapping result type
		  * @return Mapping function result, whether from left or from right
		  */
		def mapEitherToSingle[B](f: A => B) = e match {
			case Left(l) => f(l)
			case Right(r) => f(r)
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
		  * Maps an individual key within this map
		  * @param key Targeted key (must exist)
		  * @param f   A mapping function for the value in the specified key
		  * @tparam V2 Map function result type
		  * @return A copy of this map with that key mapped
		  */
		@throws[NoSuchElementException]("If this map didn't contain the specified key")
		def mapValue[V2 >: V](key: K)(f: V => V2) = m + (key -> f(m(key)))
		
		/**
		  * Merges this map with another map. If value is present only in one map, it is preserved as is.
		  * @param another Another map
		  * @param merge   A merge function used when both maps contain a value
		  * @tparam V2 The resulting value type
		  * @return A map with merged values
		  */
		def mergeWith[V2 >: V](another: Map[K, V2])(merge: (V, V2) => V2) = {
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
		  * Appends or merges a single key value pair into this map
		  * @param key   Key to add or modify
		  * @param value Value to add
		  * @param merge A merge function called if this map already contained a value for that key.
		  *              Accepts the existing map value and the new value and returns the merged (i.e. resulting) value.
		  * @tparam V2 Type of resulting values
		  * @return A modified copy of this map
		  */
		def appendOrMerge[V2 >: V](key: K, value: V2)(merge: (V, V2) => V2) = {
			m.get(key) match {
				case Some(existing) => m + (key -> merge(existing, value))
				case None => m + (key -> value)
			}
		}
	}
	
	implicit class RichIterableOnceEithers[L, R](val i: IterableOnce[Either[L, R]]) extends AnyVal
	{
		/**
		  * Divides this collection to two separate collections, one for left items and one for right items
		  * @return The Left items (1) and then the Right items (2)
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
	
	/**
	  * This extension allows tuple lists to be transformed into multi maps directly
	  */
	implicit class RichTupleVector[K, V](val list: Vector[(K, V)]) extends AnyVal
	{
		/**
		  * @return This collection as a multi map
		  */
		@deprecated("Deprecated for removal. Please use .groupMap(...) instead", "v2.4")
		def asMultiMap: Map[K, Vector[V]] = list.toMultiMap[K, V, Vector[V]] { t => t }
	}
	
	implicit class RichRange(val range: Range) extends AnyVal
	{
		/**
		  * @return The first index that is outside of this range
		  */
		def exclusiveEnd = range match {
			case r: Range.Exclusive => r.end
			case r: Range.Inclusive => if (r.step > 0) r.end + 1 else r.end - 1
		}
		
		/**
		  * This function works like foldLeft, except that it stores each step (including the start) into a vector
		  * @param start   The starting step
		  * @param map     A function for calculating the next step, takes the previous result + the next item in this range
		  * @param factory A factory for final collection (implicit)
		  * @tparam B The type of steps
		  * @return All of the steps mapped into a collection
		  */
		def foldMapToVector[B](start: B)(map: (B, Int) => B)(implicit factory: Factory[B, Vector[B]]): Vector[B] = {
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
			if (currentIterator.forall { !_.hasNext })
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
			val actualEnd = {
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

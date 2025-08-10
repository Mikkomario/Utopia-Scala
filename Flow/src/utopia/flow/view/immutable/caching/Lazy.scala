package utopia.flow.view.immutable.caching

import utopia.flow.collection.immutable.caching.iterable.LazySeq
import utopia.flow.collection.mutable.iterator.{LazyInitIterator, PollableOnce}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.ListenableLazy
import utopia.flow.view.mutable.caching.DeprecatingLazy

object Lazy
{
	// OTHER    ------------------------
	
	/**
	  * Creates a new lazily initialized container
	  * @param make A function for filling this lazy container when a value is requested the first time
	  * @tparam A Type of the wrapped value
	  * @return A new lazily initialized container
	  */
	def apply[A](make: => A): Lazy[A] = new _Lazy[A](make)
	
	/**
	  * Creates a new lazy container that fires an event when it is first initialized
	  * @param make A function for generating the stored value when it is first requested
	  * @tparam A Type of wrapped value
	  * @return A new lazily initialized container
	  */
	def listenable[A](make: => A)(implicit log: Logger) = ListenableLazy(make)
	/**
	  * @param make A function for creating an item when it is requested
	  * @tparam A Type of the item in this wrapper
	  * @return A lazily initialized wrapper that only holds a weak reference to the
	  *         generated item. A new item may be generated if the previous one is collected.
	  */
	def weak[A <: AnyRef](make: => A) = WeakLazy(make)
	/**
	  * Creates a new lazy container which may auto-reset itself under certain conditions
	  * @param make A function for generating a new value
	  * @param testValidity A function for testing whether a stored value is still valid (should be kept)
	  * @tparam A Type of stored item
	  * @return A new lazy container
	  */
	def deprecating[A](make: => A)(testValidity: A => Boolean) = DeprecatingLazy(make)(testValidity)
	/**
	  * @param make A function for creating an item when it is requested
	  * @param test A function that returns whether a generated value (accepted as a parameter) should be stored (true)
	  *             or whether a new value should be generated on the next call (false)
	  * @tparam A Type of stored / generated value
	  * @return A lazy container that only caches the value if it fulfills the specified test function
	  */
	def conditional[A](make: => A)(test: A => Boolean) = ConditionalLazy(make)(test)
	/**
	  * Creates a pre-initialized lazy container
	  * @param value A pre-initialized value
	  * @tparam A Type of the specified value
	  * @return A no-op lazy that simply wraps the value
	  */
	def initialized[A](value: A) = PreInitializedLazy(value)
	/**
	  * @param options Available lazily initialized containers
	  * @param cache Whether caching is allowed.
	  *              If true, once a value is acquired, it is cached and returned on all subsequent calls to value.
	  *              If false (default), the value may change based on how the containers become available.
	  * @tparam A Type of the wrapped / yielded value
	  * @return A lazy container selecting from one of the specified options
	  */
	// If only one container is specified, yields that
	def anyOf[A](options: Seq[Lazy[A]], cache: Boolean = false) = options.only.getOrElse {
		// Case: Caching enabled => Checks whether a value is immediately available
		if (cache)
			options.findMap { _.current } match {
				case Some(value) => initialized(value)
				case None => new FirstAvailableLazy(options)
			}
		else
			new PrioritizingLazy(options)
	}
	
	
	// EXTENSIONS   --------------------
	
	implicit class DeepLazy[+A](val l: Lazy[Lazy[A]]) extends AnyVal
	{
		/**
		  * @return A flattened version of this lazy container
		  */
		def flatten = new FlatteningLazy[A](l)
	}
	
	
	// NESTED   ------------------------
	
	private class _Lazy[+A](generator: => A) extends Lazy[A]
	{
		// ATTRIBUTES	-------------------------
		
		private lazy val _value = generator
		private var initialized = false
		
		
		// IMPLEMENTED	-------------------------
		
		override def isInitialized = initialized
		override def nonInitialized = !initialized
		
		override def current = if (initialized) Some(_value) else None
		
		override def value = {
			if (!initialized)
				initialized = true
			_value
		}
		override def valueIterator = PollableOnce(value)
		
		override def mapValue[B](f: A => B) = Lazy { f(value) }
	}
}

/**
  * A common trait for lazily initialized value wrappers
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1.6.1
  * @tparam A Type of wrapped value
  */
trait Lazy[+A] extends View[A]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Current value of this lazily initialized container
	  */
	def current: Option[A]
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Whether this lazily initialized wrapper has already been initialized
	  */
	def isInitialized = current.nonEmpty
	/**
	  * @return Whether this lazily initialized wrapper hasn't been initialized yet
	  */
	def nonInitialized = current.isEmpty
	
	
	// IMPLEMENTED	---------------------
	
	override def toString = current.map(c => s"Lazy($c)") getOrElse "Lazy"
	
	
	// OTHER    -------------------------
	
	/**
	  * Lazily maps this container's content
	  * @param f A mapping function applied when the new content is requested
	  * @tparam B Type of mapping result
	  * @return Lazily initialized mapping results
	  */
	def map[B](f: A => B): Lazy[B] = Lazy { f(value) }
	/**
	  * Maps the contents of this container.
	  * If the current value has already been calculated, performs the
	  * mapping immediately.
	  * Otherwise performs the mapping lazily.
	  * @param f A mapping function to apply
	  * @tparam B Type of mapping results
	  * @return A new possibly lazily initialized container that contains
	  *         the mapping results.
	  */
	@deprecated("Renamed to .lightMap(...)", "v2.7")
	def mapCurrent[B](f: A => B) = lightMap(f)
	/**
	  * Maps the contents of this container.
	  * Unlike [[map]], the mapping function call is not wrapped in a Lazy by itself and may be called immediately.
	  * @param f A mapping function to perform
	  * @tparam B Type of mapping results
	  * @return A lazily initialized container that has [[current]] as soon as this container has,
	  *         but yields a mapped value.
	  */
	def lightMap[B](f: A => B): Lazy[B] = current match {
		case Some(value) => Lazy.initialized(f(value))
		case None => new MappingLazyView(this)(f)
	}
	/**
	  * @param f A mapping function that yields a lazily acquired result
	  * @tparam B Type of mapping results, once acquired
	  * @return A lazily initialized container that contains the result of 'f'
	  */
	def flatMap[B](f: A => Lazy[B]): Lazy[B] = new FlatteningLazy(lightMap(f))
	/**
	 * Merges this lazy with another lazy instance.
	 * The merging function is not necessarily called lazily, however.
	 *
	 * Note: The merge results are cached, meaning that resets in either source
	 *       are not necessarily reflected in the result.
	 *
	 * @param other Another lazy container
	 * @param f A merge function that combines values from both containers
	 *          once they're requested or once they become available.
	 * @tparam B Type of the value in the other container
	 * @tparam R Type of merge results
	 * @return A lazy container that will contain the merge results
	 */
	def mergeWith[B, R](other: Lazy[B])(f: (A, B) => R): Lazy[R] = current match {
		case Some(leftV) =>
			other.current match {
				case Some(rightV) => Lazy.initialized(f(leftV, rightV))
				case None => other.lightMap { f(value, _) }
			}
			
		case None =>
			if (other.isInitialized)
				lightMap { f(_, other.value) }
			else
				MergingLazy(this, other)(f)
	}
	
	/**
	  * Lazily maps this container's content
	  * @param f A mapping function applied lazily. Yields 0-n values (via iterator).
	  * @tparam B Type of individual map results
	  * @return A lazily initialized collection based on the mapping results
	  */
	def mapToSeq[B](f: A => IterableOnce[B]) = LazySeq.from[B](LazyInitIterator { f(value) })
	
	/**
	  * Combines this with another lazy container, taking the first available value
	  * @param other Another lazy container
	  * @param cache Whether the acquired value should be cached and kept the same.
	  *              If false (default), the returned value may change to that of this lazy container
	  *              in instances where 'other' is first initialized and this becomes initialized later.
	  * @tparam B Type of values acquired from the resulting container
	  * @return A lazy container that takes its value from either of these containers
	  */
	def or[B >: A](other: Lazy[B], cache: Boolean = false) = {
		if (cache)
			current.orElse { other.current } match {
				case Some(value) => Lazy.initialized(value)
				case None => FirstAvailableLazy(this, other)
			}
		else
			PrioritizingLazy(this, other)
	}
	/**
	  * An alias for [[or]]
	  */
	def ||[B >: A](other: Lazy[B]) = or(other)
}

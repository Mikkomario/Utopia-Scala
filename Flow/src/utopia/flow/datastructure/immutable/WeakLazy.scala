package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.LazyLike

import scala.ref.WeakReference

object WeakLazy
{
	/**
	  * @param make A function for creating an item when it is requested
	  * @tparam A Type of the item in this wrapper
	  * @return A lazily initialized wrapper that only holds a weak reference to the
	  *         generated item. A new item may be generated if the previous one is collected.
	  */
	def apply[A <: AnyRef](make: => A) = new WeakLazy[A](make)
}

/**
  * A lazily initialized container that only holds a weak reference to the generated items
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class WeakLazy[A <: AnyRef](generator: => A) extends LazyLike[A]
{
	// ATTRIBUTES   ------------------------
	
	private var cached: Option[WeakReference[A]] = None
	
	
	// IMPLEMENTED  ------------------------
	
	override def current = cached.flatMap { _.get }
	
	// Uses cached value if one is available
	override def value = current.getOrElse {
		// Generates and stores a new value
		val newValue = generator
		cached = Some(WeakReference(newValue))
		// Returns the newly generated value
		newValue
	}
}

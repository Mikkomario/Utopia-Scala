package utopia.flow.view.immutable

object MutatingOnce
{
	/**
	  * Creates a new container
	  * @param initialValue Initially stored value
	  * @param nextValue Value returned after the initial value has been consumed (lazy / call-by-name)
	  * @tparam A Type of the values held in this container
	  * @return A new container
	  */
	def apply[A](initialValue: A)(nextValue: => A) = new MutatingOnce[A](initialValue, nextValue)
}

/**
  * Contains a value which can be retrieved once only. After the value has been retrieved, uses a lazily calculated
  * backup value.
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.14
  */
class MutatingOnce[+A](initialValue: A, nextValue: => A) extends View[A]
{
	// ATTRIBUTES   ----------------------------
	
	private lazy val cachedNext = nextValue
	private var consumed = false
	
	
	// COMPUTED --------------------------------
	
	/**
	  * @return Whether the initial value in this container has already been consumed
	  */
	def isConsumed = consumed
	/**
	  * @return Whether the initial value in this container hasn't yet been consumed
	  */
	def nonConsumed = !consumed
	
	
	// IMPLEMENTED  ----------------------------
	
	override def value = {
		if (isConsumed)
			cachedNext
		else {
			consumed = true
			initialValue
		}
	}
}

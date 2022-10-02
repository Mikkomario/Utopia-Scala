package utopia.flow.view.immutable

object MappingView
{
	/**
	  * Creates a new mapping view
	  * @param origin The originally viewed item
	  * @param f A mapping function applied to viewed values.
	  *          Called on every request
	  * @tparam O Type of original view's value
	  * @tparam R Type of mapping result
	  * @return A new view that yields the mapped value
	  */
	def apply[O, R](origin: View[O])(f: O => R) = new MappingView[O, R](origin)(f)
}

/**
  * A view that wraps another view and maps the result every time a value is requested.
  * This class is mostly useful in situations where one doesn't know whether the mirrored value changes or not.
  * If you know the original value is not changing, it might be more appropriate to use Lazy,
  * depending on the mapping function intensity.
  * @author Mikko Hilpinen
  * @since 1.10.2022, v2.0
  */
class MappingView[-O, +R](origin: View[O])(mirror: O => R) extends View[R]
{
	override def value = mirror(origin.value)
}

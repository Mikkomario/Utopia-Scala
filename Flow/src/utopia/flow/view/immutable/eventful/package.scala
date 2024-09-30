package utopia.flow.view.immutable

/**
  * @author Mikko Hilpinen
  * @since 29.09.2024, v2.5
  */
package object eventful
{
	/**
	  * @param value Fixed boolean value
	  * @return A flag that always contains the specified value
	  */
	def Always(value: Boolean) = if (value) AlwaysTrue else AlwaysFalse
}

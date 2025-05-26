package utopia.firmament.localization

import utopia.flow.collection.immutable.Single
import utopia.flow.view.immutable.eventful.Fixed

/**
  * Common trait for factory classes that construct strings with language information
  * @author Mikko Hilpinen
  * @since 11.05.2025, v1.5
  */
trait LocalStringFactory[+A]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param string A string to wrap
	  * @return A wrapped copy of the specified string in this factory's default language
	  */
	def apply(string: String): A
	/**
	  * @param string A string to wrap
	  * @param language Language in which the string is given (optional)
	  * @return A string in the specified language,
	  *         or with this factory's default language, if no language was specified
	  */
	def apply(string: String, language: Language): A
	
	/**
	 * @param string A string to convert to the desired local string type
	 * @return The specified string in the correct type
	 */
	def from(string: LocalString): A
	
	/**
	  * @param string A string to interpolate
	  * @param params Interpolation parameters.
	  *               Each matches %s, %S, %i or %d in 'string'
	  * @return An interpolated string
	  */
	def interpolate(string: String, params: Seq[Any]): A
	/**
	  * @param string A string to interpolate
	  * @param params Named interpolation parameters.
	  *               Each matches a ${key} element in 'string', where 'key' matches the map key.
	  * @return An interpolated string
	  */
	def interpolate(string: String, params: Map[String, Any]): A
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return An empty string
	  */
	def empty = apply("")
	/**
	  * @return An item that always contains an empty string
	  */
	def alwaysEmpty = Fixed(empty)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param strings Strings to combine
	  * @return Combined string
	  */
	def concat(strings: String*) = apply(strings.mkString)
	
	/**
	  * @param string A string to interpolate
	  * @param firstParam First interpolation parameter.
	  * @param moreParams More interpolation parameters.
	  *                   Each matches %s, %S, %i or %d in 'string'
	  * @return An interpolated string
	  */
	def interpolate(string: String)(firstParam: Any, moreParams: Any*): A =
		interpolate(string, Single(firstParam) ++ moreParams)
	
	/**
	  * @param f A mapping function applied to this factory's results
	  * @tparam B Type of mapping function results
	  * @return A new factory that applies the specified mapping function to this factory's results
	  */
	def mapResult[B](f: A => B): LocalStringFactory[B] = LocalStringFactoryWrapper(this)(f)
}

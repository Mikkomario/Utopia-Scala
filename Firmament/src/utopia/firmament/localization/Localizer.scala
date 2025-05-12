package utopia.firmament.localization

import scala.language.implicitConversions

object Localizer
{
	// OTHER    --------------------
	
	/**
	  * @param f A localizer function
	  * @return A localizer wrapping that function
	  */
	implicit def apply(f: LocalString => LocalizedString): Localizer = new _Localizer(f)
	
	
	// NESTED   --------------------
	
	private class _Localizer(f: LocalString => LocalizedString) extends Localizer
	{
		override def apply(string: LocalString): LocalizedString = f(string)
	}
}

/**
  * Localizers are used for localizing string content
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait Localizer
{
	// ABSTRACT	------------------
	
	/**
	  * @param string A string to localize
	  * @return A localized version of the specified string
	  */
	def apply(string: LocalString): LocalizedString
	
	
	// OTHER	------------------
	
	/**
	  * Localizes a string, using interpolation (segments marked with %s, %S, %i or %d will be replaced with provided
	  * arguments)
	  * @param string A string to be localized
	  * @param arguments Arguments inserted to the string
	  * @return A localized, interpolated version of the provided string
	  */
	@deprecated("Deprecated for removal. Please use .apply(LocalString).interpolateAll(Seq) instead", "v1.5")
	def localizeWith(string: LocalString)(arguments: Seq[Any]) =
		apply(string).interpolateAll(arguments)
	/**
	  * Localizes a string using interpolation (segments marked with ${key} will be replaced with matching values in
	  * the specified map)
	  * @param string String to localize
	  * @param arguments Arguments passed when interpolating the string
	  * @return A localized, interpolated string
	  */
	@deprecated("Deprecated for removal. Please use .apply(LocalString).interpolateNamed(Map) instead", "v1.5")
	def localizeWith(string: LocalString)(arguments: Map[String, Any]) =
		apply(string).interpolateNamed(arguments)
	
	/**
	  * Localizes a string
	  * @param string A string to be localized
	  * @return A localized version of the string
	  */
	@deprecated("Please use .apply(LocalString2) instead", "v1.5")
	def localize(string: LocalString): LocalizedString = apply(string)
}
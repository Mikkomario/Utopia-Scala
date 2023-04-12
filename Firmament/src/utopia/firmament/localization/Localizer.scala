package utopia.firmament.localization

/**
  * Localizers are used for localizing string content
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait Localizer
{
	// ABSTRACT	------------------
	
	/**
	  * Localizes a string
	  * @param string A string to be localized
	  * @return A localized version of the string
	  */
	def localize(string: LocalString): LocalizedString
	
	
	// OTHER	------------------
	
	/**
	  * Localizes a string, using interpolation (segments marked with %s, %S, %i or %d will be replaced with provided
	  * arguments)
	  * @param string A string to be localized
	  * @param arguments Arguments inserted to the string
	  * @return A localized, interpolated version of the provided string
	  */
	def localizeWith(string: LocalString)(arguments: Seq[Any]) = localize(string).interpolated(arguments)
	
	/**
	  * Localizes a string using interpolation (segments marked with ${key} will be replaced with matching values in
	  * the specified map)
	  * @param string String to localize
	  * @param arguments Arguments passed when interpolating the string
	  * @return A localized, interpolated string
	  */
	def localizeWith(string: LocalString)(arguments: Map[String, Any]) = localize(string).interpolated(arguments)
}
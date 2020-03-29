package utopia.reflection.localization

/**
  * Localizers are used for localizing string content
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
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
	  * @param firstArg The first interpolation argument
	  * @param moreArgs More interpolation arguments
	  * @return A localized, interpolated version of the provided string
	  */
	def localizeWith(string: LocalString)(firstArg: Any, moreArgs: Any*) = localize(string).interpolate(firstArg, moreArgs)
}
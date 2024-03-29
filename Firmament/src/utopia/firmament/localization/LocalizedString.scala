package utopia.firmament.localization

import utopia.flow.view.immutable.eventful.Fixed

import scala.language.implicitConversions

object LocalizedString
{
	/**
	  * An empty localized string
	  */
	val empty = LocalizedString(LocalString.empty, LocalString.empty)
	/**
	 * A pointer that always contains an empty localized string
	 */
	val alwaysEmpty = Fixed(empty)
	
	/**
	  * Creates a new localized string
	  * @param original The local string
	  * @param localized The localized version
	  * @return A new localized string
	  */
	def apply(original: LocalString, localized: LocalString) = new LocalizedString(original, Some(localized))
	
	/**
	  * Automatically converts a local string to localized string using an implicit localizer
	  * @param local A local string
	  * @param localizer A localizer (implicit)
	  * @return A localized version of the string
	  */
	implicit def autoLocalize(local: LocalString)(implicit localizer: Localizer): LocalizedString = localizer.localize(local)
	
	/**
	  * Automatically converts a raw string to localized using an implicit localizer + default language code
	  * @param str A raw string
	  * @param defaultLanguageCode The default language ISO-code (implicit)
	  * @param localizer A localizer (implicit)
	  * @return A localized version of the string
	  */
	implicit def autoLocalize(str: String)(implicit defaultLanguageCode: String, localizer: Localizer): LocalizedString =
		localizer.localize(LocalString(str, defaultLanguageCode))
}

/**
  * A localized string is a string that has been localized for user context
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  * @param original The non-localized version of this string
  * @param localized the localized version of this string (if available, None otherwise)
  */
case class LocalizedString(original: LocalString, localized: Option[LocalString]) extends LocalStringLike[LocalizedString]
{
	// COMPUTED	------------------------
	
	/**
	  * @return A string that will be displayed when using this localized string (uses localized if available and
	  *         original as backup)
	  */
	def displayed = localized getOrElse original
	
	/**
	  * @return A string representation of this localized string
	  */
	def string = displayed.string
	
	/**
	  * @return The ISO code for the source language
	  */
	def sourceLanguageCode = original.languageCode
	
	/**
	  * @return The ISO code for the target language
	  */
	def targetLanguageCode = displayed.languageCode
	
	/**
	  * @return Whether localized data is available
	  */
	def isLocalized = localized.isDefined
	
	/**
	  * @return A local string based on this localized string (same as original string if no localization was used)
	  */
	def local = localized.getOrElse(original)
	
	
	// IMPLEMENTED	------------------------
	
	override def self = this
	
	override def languageCode = targetLanguageCode orElse sourceLanguageCode
	
	override def modify(f: String => String) = LocalizedString(original.modify(f), localized.map { _.modify(f) })
	
	override def +(other: LocalizedString) = LocalizedString(original + other.original, displayed + other.displayed)
	
	override def split(regex: String) =
	{
		val originalSplits = original.split(regex)
		
		if (isLocalized)
		{
			val localizedSplits = localized.get.split(regex)
			
			if (originalSplits.size == localizedSplits.size)
				originalSplits.zip(localizedSplits).map { case (orig, loc) => LocalizedString(orig, Some(loc)) }
			else
				localizedSplits.map { LocalizedString(_, None) }
		}
		else
			originalSplits.map { LocalizedString(_, None) }
	}
	
	override def interpolated(args: Map[String, Any]) = LocalizedString(original.interpolated(args),
		localized.map { _.interpolated(args) })
	
	override def interpolated(args: Seq[Any]) = LocalizedString(original.interpolated(args), localized.map { _.interpolated(args) })
	
	
	// OPERATORS	--------------------------
	
	/**
	  * Appends this localized string with another localization
	  * @param str A string
	  * @param localizer A localizer that will loalize the string
	  * @return a combined localized string
	  */
	def +(str: String)(implicit localizer: Localizer) = LocalizedString(original + str,
		displayed + localizer.localize(LocalString(str, sourceLanguageCode)).string)
}

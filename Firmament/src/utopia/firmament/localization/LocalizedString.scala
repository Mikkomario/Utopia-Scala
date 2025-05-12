package utopia.firmament.localization

import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.eventful.Fixed

import scala.language.implicitConversions

object LocalizedString
{
	// ATTRIBUTES ------------------------
	
	/**
	  * A factory for constructing localized strings without language information
	  */
	lazy val noLanguage = in(Language.none)
	
	/**
	  * @return An empty localized string
	  */
	lazy val empty = noLanguage("")
	/**
	  * @return An item that always contains an empty localized string
	  */
	lazy val alwaysEmpty = Fixed(empty)
	
	
	// COMPUTED --------------------------
	
	/**
	  * @param language Language of the specified input strings
	  * @return A factory for constructing strings of the specified language, where localization has been skipped
	  */
	def skipLocalization(implicit language: Language) = in(language)
	
	
	// IMPLICIT --------------------------
	
	implicit def autoLocalize(local: LocalString)(implicit localizer: Localizer): LocalizedString = localizer(local)
	
	implicit def autoLocalize(local: String)(implicit language: Language, localizer: Localizer): LocalizedString =
		localizer(LocalString.in(language)(local))
	
	
	// OTHER    --------------------------
	
	/**
	  * Wraps a local string, marking it as localized
	  * @param string A local string
	  * @return A localized copy of the specified string
	  */
	def wrap(string: LocalString): LocalizedString = string match {
		case localized: LocalizedString => localized
		case local => Wrapper(local)
	}
	
	/**
	  * @param language Targeted language
	  * @return A factory for constructing localized strings in the specified language
	  */
	def in(language: Language): LocalStringFactory[LocalizedString] =
		LocalString.in(language).mapResult(Wrapper.apply)
	
	
	// NESTED   --------------------------
	
	private case class Wrapper(_localized: LocalString) extends LocalizedString
	{
		// ATTRIBUTES   ------------------
		
		override lazy val factory: LocalStringFactory[LocalizedString] =
			_localized.factory.mapResult(LocalizedString.wrap)
		
		
		// IMPLEMENTED  ------------------
		
		override def wrapped: String = _localized.wrapped
		override def raw: String = _localized.raw
		override def language: Language = _localized.language
	}
}

/**
  * Common trait for strings that have been marked as localized for user context
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait LocalizedString extends LocalString with LocalStringLike[LocalizedString]
{
	// COMPUTED	------------------------
	
	/**
	  * @return The ISO code for the target language
	  */
	@deprecated("Deprecated for removal", "v1.5")
	def targetLanguageCode = language.code.ifNotEmpty
	
	@deprecated("Deprecated for removal", "v1.5")
	def local = this
	@deprecated("Deprecated for removal", "v1.5")
	def localized = Some(this)
	@deprecated("Deprecated for removal", "v1.5")
	def displayed = this
	
	
	// IMPLEMENTED	------------------------
	
	override def self = this
	
	override def isLocalized = true
	override def skipLocalization: LocalizedString = self
	override def localized(implicit localizer: Localizer): LocalizedString = self
}

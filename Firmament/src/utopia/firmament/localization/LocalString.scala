package utopia.firmament.localization

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty

import scala.annotation.unused
import scala.language.implicitConversions

object LocalString
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A factory for constructing strings explicitly without language information
	  */
	lazy val noLanguage = in(Language.none)
	
	
	// IMPLICIT --------------------------
	
	/**
	  * @param string String to wrap
	  * @param language Language of the specified string
	  * @return A new local string
	  */
	implicit def apply(string: String)(implicit language: Language): LocalString = _LocalString(string, language)
	
	implicit def objectToFactory(@unused o: LocalString.type)
	                            (implicit language: Language): LocalStringFactory[LocalString] =
		in(language)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param language Applied language
	  * @return A string factory using that language (as the default)
	  */
	def in(language: Language): LocalStringFactory[LocalString] = _LocalStringFactory(language)
	
	private def _interpolate(string: String, params: Seq[Any]) = {
		if (params.isEmpty)
			string
		else {
			val builder = new StringBuilder()
			
			var cursor = 0
			var nextArgIndex = 0
			
			while (cursor < string.length) {
				// Finds the next argument position
				val nextArgumentPosition = string.indexOf('%', cursor)
				
				// After all arguments have been parsed, adds the remaining part of the string
				if (nextArgumentPosition < 0) {
					builder.append(string.substring(cursor))
					cursor = string.length
				}
				else {
					// The part between the arguments is kept as is
					builder.append(string.substring(cursor, nextArgumentPosition))
					
					// Sometimes '%' is used without argument type, in which case it is copied as is
					// This also happens when there aren't enough arguments provided
					if (string.length < nextArgumentPosition || params.hasSize <= nextArgIndex) {
						builder.append('%')
						cursor = nextArgumentPosition + 1
					}
					else {
						// Checks the argument type
						val argType = StringArgumentType(string.charAt(nextArgumentPosition + 1))
						
						// Parses argument and inserts it to string
						builder.append(argType.parse(params(nextArgIndex)))
						nextArgIndex += 1
						cursor = nextArgumentPosition + 2
					}
				}
			}
			builder.toString()
		}
	}
	private def _interpolate(string: String, params: Map[String, Any]) = {
		if (params.isEmpty)
			string
		else
			params.foldLeft(string) { case (str, (paramName, value)) => str.replace(s"$${$paramName}", value.toString) }
	}
	
	
	// EXTENSIONS	-----------------------------
	
	implicit class StringLocal(val s: String) extends AnyVal
	{
		/**
		  * @return A local version of string with no language information
		  */
		def noLanguage = in(Language.none)
		/**
		  * @return A local version of string with no language information and localization skipped
		  */
		@deprecated("Please use .noLanguage.skipLocalization instead", "v1.5")
		def noLanguageLocalizationSkipped = noLanguage.skipLocalization
		
		/**
		  * @param language Local language (implicit)
		  * @return A local version of string
		  */
		def local(implicit language: Language) = in(language)
		/**
		  * @param language Local language (implicit)
		  * @param localizer A localizer (implicit)
		  * @return A localized version of this string
		  */
		def autoLocalized(implicit language: Language, localizer: Localizer): LocalizedString = in(language).localized
		
		/**
		  * @param language Language of this string
		  * @return A local string wrapping this string
		  */
		def in(language: Language) = LocalString.in(language)(s)
	}
	
	
	// NESTED   --------------------------
	
	private case class _LocalStringFactory(language: Language) extends LocalStringFactory[LocalString]
	{
		// IMPLEMENTED  ------------------
		
		override def apply(string: String) = _LocalString(string, language)
		override def apply(string: String, language: Language): LocalString =
			_LocalString(string, language.nonEmptyOrElse(this.language))
		
		override def from(string: LocalString): LocalString = string
		
		override def interpolate(string: String, params: Seq[Any]): LocalString =
			if (params.isEmpty) apply(string) else InterpolatedWrapper(apply(string), params)
		override def interpolate(string: String, params: Map[String, Any]): LocalString = {
			if (params.isEmpty)
				apply(string)
			else
				InterpolatedWrapper(apply(string), namedParams = params)
		}
	}
	
	private class InterpolatingFactory(override protected val wrapped: LocalStringFactory[LocalString],
	                                   unnamedParams: Seq[Any], namedParams: Map[String, Any])
		extends LocalStringFactoryWrapper[LocalString, LocalString]
	{
		override def from(string: LocalString): LocalString = string
		
		override protected def wrap(string: LocalString): LocalString =
			InterpolatedWrapper(string, unnamedParams, namedParams)
	}
	
	private case class _LocalString(wrapped: String, language: Language) extends LocalString
	{
		override val isLocalized: Boolean = false
		
		override def self: LocalString = this
		override def factory: LocalStringFactory[LocalString] = LocalString.in(language)
		override def raw: String = wrapped
		
		override def skipLocalization: LocalizedString = LocalizedString.wrap(this)
		override def localized(implicit localizer: Localizer): LocalizedString = localizer(this)
	}
	
	private case class InterpolatedWrapper(original: LocalString, unnamedParams: Seq[Any] = Empty,
	                                       namedParams: Map[String, Any] = Map())
		extends LocalString
	{
		// Lazily generates the interpolated version
		override lazy val wrapped: String = _interpolate(_interpolate(original.wrapped, unnamedParams), namedParams)
		override lazy val factory: LocalStringFactory[LocalString] =
			new InterpolatingFactory(original.factory, unnamedParams, namedParams)
		
		override def self: LocalString = this
		
		override def raw: String = original.raw
		override def language: Language = original.language
		
		override def isLocalized: Boolean = original.isLocalized
		override def skipLocalization: LocalizedString = LocalizedString.wrap(this)
		override def localized(implicit localizer: Localizer): LocalizedString = {
			if (isLocalized)
				this
			else
				LocalizedString.wrap(copy(original = localizer(original)))
		}
	}
}

/**
  * Represents a string in some language
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait LocalString extends LocalStringLike[LocalString]

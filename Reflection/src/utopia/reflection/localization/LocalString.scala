package utopia.reflection.localization

import scala.language.implicitConversions

object LocalString
{
	// ATTRIBUTES	-----------------------------
	
	/**
	  * An empty local string
	  */
	val empty = LocalString("")
	
	
	// IMPLICIT CONVERSIONS	---------------------
	
	/**
	  * Converts a string into a local string using the default language code (implicit)
	  * @param string A string
	  * @param defaultLanguageCode An implicit default language code
	  * @return A string as a local string in default language
	  */
	implicit def stringToLocal(string: String)(implicit defaultLanguageCode: String): LocalString =
		LocalString(string, defaultLanguageCode)
	
	
	// OPERATORS	-----------------------------
	
	def apply(string: String, languageCode: String) = new LocalString(string, Some(languageCode))
	
	
	// EXTENSIONS	-----------------------------
	
	implicit class StringLocal(val s: String) extends AnyVal
	{
		/**
		  * @param languageCode ISO code of the string's language (implicit)
		  * @return A local version of string
		  */
		def local(implicit languageCode: String) = LocalString(s, languageCode)
		
		/**
		  * @return A local version of string with no language information
		  */
		def noLanguage = LocalString(s)
		
		/**
		 * @param languageCode Language code for this string (implicit)
		 * @param localizer A localizer (implicit)
		 * @return A localized version of this string
		 */
		def autoLocalized(implicit languageCode: String, localizer: Localizer): LocalizedString = s
		
		/**
		  * @param languageCode ISO code of the string's language (implicit)
		  * @return A local version of string with localization skipped
		  */
		def localizationSkipped(implicit languageCode: String) = local.localizationSkipped
		
		/**
		  * @return A local version of string with no language information and localization skipped
		  */
		def noLanguageLocalizationSkipped = noLanguage.localizationSkipped
	}
}

/**
  * LocalStrings are simple strings that know the language of their contents
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  * @param string A source string
  * @param languageCode The 2-character ISO code for the language of the string
  */
case class LocalString(override val string: String, override val languageCode: Option[String] = None) extends LocalStringLike[LocalString]
{
	// COMPUTED	--------------------------
	
	/**
	  * @return A version of this string where localization has been skipped
	  */
	def localizationSkipped = LocalizedString(this, None)
	
	/**
	 * @param localizer An implicit localizer
	 * @return A localized version of this local string
	 */
	def localized(implicit localizer: Localizer) = localizer.localize(this)
	
	
	// IMPLEMENTED	----------------------
	
	override def +(other: LocalString) =
	{
		val newCode =
		{
			if (languageCode.isDefined)
			{
				if (other.languageCode.forall { _ == languageCode.get }) languageCode else None
			}
			else
				other.languageCode
		}
		
		LocalString(string + other.string, newCode)
	}
	
	override def split(regex: String) = string.split(regex).toVector.map { LocalString(_, languageCode) }
	
	override def interpolate(args: Seq[Any]) = LocalString(parseArguments(string, args), languageCode)
	
	
	// OPERATORS	----------------------
	
	/**
	  * Appends a string at the end of this string
	  * @param str A string to append
	  * @return An appended local string
	  */
	def +(str: String) = LocalString(string + str, languageCode)
	
	
	// OTHER	--------------------------
	
	private def parseArguments(field: String, args: Seq[Any]) =
	{
		val str = new StringBuilder()
		
		var cursor = 0
		var nextArgIndex = 0
		
		while (cursor < field.length)
		{
			// Finds the next argument position
			val nextArgumentPosition = cursor + field.substring(cursor).indexOf('%')
			
			// After all arguments have been parsed, adds the remaining part of the string
			if (nextArgumentPosition < cursor)
			{
				str.append(field.substring(cursor))
				cursor = field.length
			}
			else
			{
				// The part between the arguments is kept as is
				str.append(field.substring(cursor, nextArgumentPosition))
				
				// The field may end in '%', in which case the following checks cannot be made
				if (field.length <= nextArgumentPosition + 1)
				{
					str.append('%')
					cursor = field.length
				}
				else
				{
					// Checks the argument type
					val argType = StringArgumentType(field.charAt(nextArgumentPosition + 1))
					
					// Sometimes '%' is used without argument type, in which case it is copied as is
					// This also happens when there aren't enough arguments provided
					if (argType.isEmpty || nextArgIndex >= args.size)
					{
						str.append('%')
						cursor = nextArgumentPosition + 1
					}
					else
					{
						// Parses argument and inserts it to string
						str.append(argType.get.parse(args(nextArgIndex)))
						nextArgIndex += 1
						cursor = nextArgumentPosition + 2
					}
				}
			}
		}
		
		str.toString()
	}
}

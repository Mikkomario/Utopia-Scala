package utopia.logos.model.stored.url

import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.logos.database.access.url.domain.AccessDomain
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.logos.model.factory.url.DomainFactoryWrapper
import utopia.logos.model.partial.url.DomainData
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}

object Domain extends StandardStoredFactory[DomainData, Domain]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * A regular expression that matches a forward slash (/)
	  */
	val forwardSlashRegex = Regex.escape('/')
	private val colonRegex = Regex.escape(':')
	/**
	 * A regular expression that accepts characters that may appear in a domain name part
	 */
	val domainCharacterRegex = (Regex.letterOrDigit || Regex.anyOf("-.")).withinParentheses
	/**
	 * A regular expression that identifies "http://" and "https://"
	 */
	val httpRegex = (Regex("http") + Regex("s").noneOrOnce + colonRegex + forwardSlashRegex.times(2)).withinParentheses
	/**
	 * A regular expression that finds "www."
	 */
	val wwwRegex = ((Regex("w").times(3) || Regex("W").times(3)) + Regex.escape('.')).withinParentheses
	private val portNumberRegex = (colonRegex + Regex.digit.times(1 to 6)).withinParentheses
	
	/**
	  * A regular expression that matches a domain part of a link.
	  * For example, matches: "https://api.example.com", "http://128.0.0.1:8080" and "www.palvelu.fi"
	  */
	val regex =
		((httpRegex + wwwRegex.noneOrOnce).withinParentheses || wwwRegex).withinParentheses +
			domainCharacterRegex.oneOrMoreTimes + Regex.escape('.') + domainCharacterRegex.oneOrMoreTimes +
			portNumberRegex.noneOrOnce
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = DomainData
		
	
	// OTHER    ------------------------
	
	/**
	 * Standardizes a URL, so that it includes: "http://" or "https://"
	 * @param url URL to standardize
	 * @param preferHttps Whether to prepend "https://" instead of "http://". Default = false.
	 * @return A standardized copy of the specified URL. If the specified URL was empty, returns it as it is.
	 */
	def standardize(url: String, preferHttps: Boolean = false) = {
		// Case: Empty string => No changes
		if (url.isEmpty)
			url
		// Looks for the http(s) part
		else {
			val noControl = url.stripControlCharacters.trim
			// Case: Http(s) found => Valid
			val modified = {
				if (httpRegex.existsIn(noControl))
					noControl
				// Case: No http(s) => Adds it
				else {
					val httpPart = s"http${ if (preferHttps) "s" else "" }://"
					s"$httpPart$noControl"
				}
			}
			// Removes the trailing forward slash, also
			if (modified.endsWith("/"))
				modified.dropRight(1)
			else
				modified
		}
	}
	
	/**
	 * Includes www. in the specified URL, unless it's based on a numeric IP address
	 * @param url URL to process
	 * @return Copy of the specified URL, including www, if appropriate
	 */
	def includeWww(url: String) = {
		if (wwwRegex.existsIn(url))
			url
		else
			httpRegex.endIndexIteratorIn(url).nextOption() match {
				case Some(httpEndIndex) =>
					val (toHttp, afterHttp) = url.splitAt(httpEndIndex)
					if (afterHttp.headOption.exists { _.isDigit })
						url
					else
						s"${toHttp}www.$afterHttp"
				case None =>
					if (url.headOption.exists { _.isDigit })
						url
					else
						s"www.$url"
			}
	}
}

/**
  * Represents a domain that has already been stored in the database
  * @param id id of this domain in the database
  * @param data Wrapped domain data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class Domain(id: Int, data: DomainData) 
	extends StoredModelConvertible[DomainData] with FromIdFactory[Int, Domain] 
		with DomainFactoryWrapper[DomainData, Domain]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this domain in the database
	  */
	def access = AccessDomain(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def toString = data.toString
	
	override def withId(id: Int) = copy(id = id)
	override protected def wrap(data: DomainData) = copy(data = data)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param path Path to wrap
	 * @return Specified path with this domain attached
	 */
	def apply(path: RequestPath) = DetailedRequestPath(path, this)
}


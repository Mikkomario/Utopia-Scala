package utopia.logos.model.stored.url

import utopia.flow.parse.string.Regex
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
	lazy val forwardSlashRegex = Regex.escape('/')
	private lazy val colonRegex = Regex.escape(':')
	private lazy val domainCharacterRegex = (Regex.letterOrDigit || Regex.anyOf("-.")).withinParentheses
	/**
	 * A regular expression that identifies "http://" and "https://"
	 */
	lazy val httpRegex =
		(Regex("http") + Regex("s").noneOrOnce + colonRegex + forwardSlashRegex.times(2)).withinParentheses
	private lazy val wwwRegex = (Regex("w").times(3) + Regex.escape('.')).withinParentheses
	private lazy val portNumberRegex = (colonRegex + Regex.digit.times(1 to 6)).withinParentheses
	
	/**
	  * A regular expression that matches a domain part of a link.
	  * For example, matches: "https://api.example.com", "http://128.0.0.1:8080" and "www.palvelu.fi"
	  */
	// TODO: Should this accept something like "home.com"?
	lazy val regex = 
		((httpRegex + wwwRegex.noneOrOnce).withinParentheses || wwwRegex).withinParentheses +
			domainCharacterRegex.oneOrMoreTimes + Regex.escape('.') + domainCharacterRegex.oneOrMoreTimes +
			portNumberRegex.noneOrOnce
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = DomainData
		
	
	// OTHER    ------------------------
	
	/**
	 * Standardizes a URL, so that it includes:
	 *      - "http://" or "https://"
	 *      - "www.", unless based on a numeric address
	 * @param url URL to standardize
	 * @param preferHttps Whether to prepend "https://" instead of "http://". Default = false.
	 * @return A standardized copy of the specified URL. If the specified URL was empty, returns it as it is.
	 */
	def standardize(url: String, preferHttps: Boolean = false) = {
		// Case: Empty string => No changes
		if (url.isEmpty)
			url
		// Looks for the http(s) part
		else
			httpRegex.endIndexIteratorIn(url).nextOption() match {
				// Case: Http(s) found => Looks for www.
				case Some(httpEndIndex) =>
					// Case: www also found => Yields the same URL
					if (wwwRegex.existsIn(url))
						url
					// Case: No www => Checks whether needed (not used on number-based URLs)
					else {
						val (toHttp, afterHttp) = url.splitAt(httpEndIndex)
						if (afterHttp.headOption.exists { _.isDigit })
							url
						else
							s"${toHttp}www.$url"
					}
					
				// Case: No http(s) => Adds it and also looks, whether www. should be added
				case None =>
					val httpPart = s"http${ if (preferHttps) "s" else "" }://"
					if (wwwRegex.existsIn(url) || url.headOption.exists { _.isDigit })
						s"$httpPart$url"
					else
						s"${httpPart}www.$url"
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


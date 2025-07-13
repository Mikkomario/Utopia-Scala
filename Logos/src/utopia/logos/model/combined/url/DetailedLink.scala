package utopia.logos.model.combined.url

import utopia.flow.util.StringExtensions._
import utopia.flow.view.template.Extender
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.{Domain, StoredLink, RequestPath}

object DetailedLink
{
	// OTHER    --------------------------
	
	/**
	 * Creates a new link
	 * @param link Link to wrap
	 * @param domain Associated domain
	 * @param path Associated request path
	 * @return A new link containing all the specified data
	 */
	def apply(link: StoredLink, domain: Domain, path: RequestPath): DetailedLink =
		apply(link, DetailedRequestPath(path, domain))
	/**
	 * @param link Link to wrap
	 * @param path Associated request path, including domain
	 * @return A link containing full link information
	 */
	def apply(link: StoredLink, path: DetailedRequestPath): DetailedLink = _DetailedLink(link, path)
		
	
	// NESTED   -------------------------
	
	private case class _DetailedLink(stored: StoredLink, path: DetailedRequestPath) extends DetailedLink
	{
		override protected def wrap(factory: StoredLink): DetailedLink = copy(stored = factory)
	}
}

/**
 * Attaches request path and domain information to a link
 * @author Mikko Hilpinen
 * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v0.2 11.3.2024
 */
trait DetailedLink extends CombinedLink[DetailedLink]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Request path -part of this link, including domain information
	 */
	def path: DetailedRequestPath
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return The domain part of this link
	 */
	def domain = path.domain
	
	@deprecated("Renamed to path", "0.6")
	def requestPath: DetailedRequestPath = path
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = {
		val paramsPart = stored.queryParameters.properties
			.map { c => s"${c.name}${c.value.getString.mapIfNotEmpty { v => s"=$v" }}" }.mkString("&")
			.mapIfNotEmpty { str => s"?$str" }
		s"$path$paramsPart"
	}
}

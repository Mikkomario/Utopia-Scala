package utopia.logos.model.combined.url

import utopia.flow.util.StringExtensions._
import utopia.flow.view.template.Extender
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.{Domain, StoredLink, RequestPath}

object DetailedLink
{
	/**
	 * Creates a new link
	 * @param link Link to wrap
	 * @param domain Associated domain
	 * @param path Associated request path
	 * @return A new link containing all of the specified data
	 */
	def apply(link: StoredLink, domain: Domain, path: RequestPath): DetailedLink =
		apply(link, DetailedRequestPath(path, domain))
}

/**
 * Attaches request path and domain information to a link
 * @author Mikko Hilpinen
 * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v0.2 11.3.2024
 */
case class DetailedLink(link: StoredLink, requestPath: DetailedRequestPath) extends Extender[LinkData]
{
	// COMPUTED -------------------------
	
	/**
	 * @return Id of this link
	 */
	def id = link.id
	
	
	// IMPLEMENTED  ---------------------
	
	override def wrapped: LinkData = link.data
	
	override def toString = {
		val paramsPart = link.queryParameters.properties
			.map { c => s"${c.name}${c.value.getString.mapIfNotEmpty { v => s"=$v" }}" }.mkString("&")
			.mapIfNotEmpty { str => s"?$str" }
		s"$requestPath$paramsPart"
	}
}

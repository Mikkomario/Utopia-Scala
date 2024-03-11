package utopia.logos.model.combined.url

import utopia.flow.view.template.Extender
import utopia.flow.util.StringExtensions._
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.{Domain, RequestPath}

/**
  * Includes textual domain information in a request path
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class DetailedRequestPath(requestPath: RequestPath, domain: Domain) extends Extender[RequestPathData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this request path in the database
	  */
	def id = requestPath.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = requestPath.data
	
	override def toString = s"$domain${requestPath.path.mapIfNotEmpty { p => s"/$p" }}"
}


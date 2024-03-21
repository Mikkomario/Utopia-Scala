package utopia.logos.model.combined.url

import utopia.flow.util.StringExtensions._
import utopia.flow.view.template.Extender
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.{Domain, RequestPath}

/**
  * Includes textual domain information in a request path
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class DetailedRequestPath(requestPath: RequestPath, domain: Domain) extends Extender[RequestPathData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this request path in the database
	  */
	def id = requestPath.id
	
	
	// IMPLEMENTED	--------------------
	
	override def toString = s"$domain${requestPath.path.mapIfNotEmpty { p => s"/$p" }}"
	
	override def wrapped = requestPath.data
}


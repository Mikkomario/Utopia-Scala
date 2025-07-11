package utopia.logos.model.combined.url

import utopia.flow.util.StringExtensions._
import utopia.flow.view.template.Extender
import utopia.logos.model.factory.url.RequestPathFactoryWrapper
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.{Domain, RequestPath}
import utopia.vault.model.template.HasId

object DetailedRequestPath
{
	// OTHER	--------------------
	
	/**
	  * @param requestPath request path to wrap
	  * @param domain domain to attach to this request path
	  * @return Combination of the specified request path and domain
	  */
	def apply(requestPath: RequestPath, domain: Domain): DetailedRequestPath = 
		_DetailedRequestPath(requestPath, domain)
	
	
	// NESTED	--------------------
	
	/**
	  * @param requestPath request path to wrap
	  * @param domain domain to attach to this request path
	  */
	private case class _DetailedRequestPath(requestPath: RequestPath, domain: Domain) extends DetailedRequestPath
	{
		// IMPLEMENTED	--------------------
		
		override protected def wrap(factory: RequestPath) = copy(requestPath = factory)
	}
}

/**
  * Includes textual domain information in a request path
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait DetailedRequestPath 
	extends Extender[RequestPathData] with HasId[Int] 
		with RequestPathFactoryWrapper[RequestPath, DetailedRequestPath]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped request path
	  */
	def requestPath: RequestPath
	/**
	  * The domain that is attached to this request path
	  */
	def domain: Domain
	
	
	// IMPLEMENTED	--------------------
	
	/**
	  * Id of this request path in the database
	  */
	override def id = requestPath.id
	
	override def wrapped = requestPath.data
	override protected def wrappedFactory = requestPath
	
	override def toString = s"$domain${requestPath.path.mapIfNotEmpty { p => s"/$p" }}"
}


package utopia.annex.model.request

import utopia.access.http.Method
import utopia.flow.generic.model.immutable.Value

/**
  * Represents a relatively simple request that may be sent multiple times if need be
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait ApiRequest extends Retractable
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Request method
	  */
	def method: Method
	/**
	  * @return Request path (root path not included)
	  */
	def path: String
	/**
	  * @return Request body value. Empty value if no body should be sent
	  */
	def body: Value
	
	
	// COMPUTED ---------------------------
	
	@deprecated("Deprecated for removal. Renamed to .deprecated.", "v1.7")
	def isDeprecated = deprecated
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = s"$method $path"
}

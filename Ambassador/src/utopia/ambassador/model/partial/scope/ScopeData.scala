package utopia.ambassador.model.partial.scope

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.StyledModelConvertible

/**
  * Contains infromation about an access scope
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  * @param serviceId Id of the service that uses this scope
  * @param officialName Name of this scope in the applicable service
  * @param clientSideName Name of this scope for client side context (optional)
  */
case class ScopeData(serviceId: Int, officialName: String, clientSideName: Option[String] = None)
	extends StyledModelConvertible
{
	// COMPUTED -----------------------------------
	
	/**
	  * @return Name to display for this scope (in client side context)
	  */
	def displayName = clientSideName.getOrElse(officialName)
	
	
	// IMPLEMENTED  -------------------------------
	
	override def toSimpleModel = Model(Vector("name" -> displayName))
	
	override def toModel = Model(Vector("service_id" -> serviceId, "official_name" -> officialName,
		"display_name" -> clientSideName))
}
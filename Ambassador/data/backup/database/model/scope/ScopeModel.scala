package utopia.ambassador.database.model.scope

import utopia.ambassador.database.factory.scope.ScopeFactory
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

object ScopeModel extends DataInserter[ScopeModel, Scope, ScopeData]
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * Name of the property that refers to the scope's granting service
	  */
	val serviceIdAttName = "serviceId"
	/**
	  * Name of the property that contains scope name in the 3rd party service
	  */
	val serviceSideNameAttName = "serviceSideName"
	
	
	// COMPUTED --------------------------------
	
	/**
	  * @return The factory used by this class
	  */
	def factory = ScopeFactory
	
	/**
	  * Column that refers to the scope's granting service
	  */
	def serviceIdColumn = table(serviceIdAttName)
	/**
	  * @return Column that contains scope name in the 3rd party service
	  */
	def serviceSideNameColumn = table(serviceSideNameAttName)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def table = factory.table
	
	override def apply(data: ScopeData) =
		apply(None, Some(data.serviceId), Some(data.officialName), data.clientSideName, data.priority)
	
	override protected def complete(id: Value, data: ScopeData) = Scope(id.getInt, data)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param serviceId A 3rd party service id
	  * @return A model with that service id
	  */
	def withServiceId(serviceId: Int) = apply(serviceId = Some(serviceId))
}

/**
  * Used for interacting with scope data in the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
case class ScopeModel(id: Option[Int] = None, serviceId: Option[Int] = None, serviceSideName: Option[String] = None,
                      clientSideName: Option[String] = None, priority: Option[Int] = None,
                      created: Option[Instant] = None)
	extends StorableWithFactory[Scope]
{
	import ScopeModel._
	
	override def factory = ScopeModel.factory
	
	override def valueProperties = Vector("id" -> id, serviceIdAttName -> serviceId,
		serviceSideNameAttName -> serviceSideName, "clientSideName" -> clientSideName, "created" -> created)
}

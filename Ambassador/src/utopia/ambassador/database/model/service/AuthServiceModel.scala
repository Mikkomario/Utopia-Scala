package utopia.ambassador.database.model.service

import utopia.ambassador.database.factory.service.AuthServiceFactory
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory

import java.time.Instant

object AuthServiceModel
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * Name of the property that contains service name
	  */
	val nameAttName = "name"
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return The factory used by this model type
	  */
	def factory = AuthServiceFactory
	/**
	  * @return The table used by this model type
	  */
	def table = factory.table
	
	/**
	  * @return The column that contains service name
	  */
	def nameColumn = table(nameAttName)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param serviceName A service name
	  * @return a model with that name
	  */
	def withName(serviceName: String) = apply(name = Some(serviceName))
}

/**
  * Used for interacting with 3rd party authentication services in the DB
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
case class AuthServiceModel(id: Option[Int] = None, name: Option[String] = None, created: Option[Instant] = None)
	extends StorableWithFactory[AuthService]
{
	import AuthServiceModel._
	
	override def factory = AuthServiceModel.factory
	
	override def valueProperties = Vector("id" -> id, nameAttName -> name, "created" -> created)
}

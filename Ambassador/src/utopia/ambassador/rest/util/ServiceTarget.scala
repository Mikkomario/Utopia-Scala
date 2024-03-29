package utopia.ambassador.rest.util

import utopia.ambassador.database.access.single.service.{DbAuthService, DbAuthServiceSettings, DbServiceId}
import utopia.vault.database.Connection

object ServiceTarget
{
	/**
	  * @param serviceName A service name
	  * @return A new service target based on service name
	  */
	def name(serviceName: String) = apply(Left(serviceName))
	/**
	  * @param serviceId A service id
	  * @return A new service target based on service id
	  */
	def id(serviceId: Int) = apply(Right(serviceId))
}

/**
  * Represents a service selection by the requesting user.
  * The selection may be made using the service id or service name.
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
case class ServiceTarget(target: Either[String, Int])
{
	// COMPUTED ------------------------
	
	/**
	  * Converts this target to a service id. Doesn't check the validity of the id, however.
	  * @param connection Implicit DB connection (used for swapping a service name into a service id)
	  * @return Id of this service. None if this is not a valid service.
	  */
	def id(implicit connection: Connection) = target match
	{
		case Left(name) => idForName(name)
		case Right(id) => Some(id)
	}
	/**
	  * @param connection Implicit DB connection
	  * @return Id of this service. None if this is not a valid service.
	  */
	def validId(implicit connection: Connection) = target match
	{
		case Left(name) => idForName(name)
		case Right(id) => if (DbAuthService(id).nonEmpty) Some(id) else None
	}
	
	/**
	  * @return An access point to this service's data in the DB
	  */
	def access = target match
	{
		case Left(name) => DbAuthService.withName(name)
		case Right(id) => DbAuthService(id)
	}
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Settings of this authentication service
	  */
	def settings(implicit connection: Connection) = target match
	{
		case Left(name) => DbAuthService.withName(name).withSettings.map { _.settings }
		case Right(id) => DbAuthServiceSettings.forServiceWithId(id).pull
	}
	/**
	  * @param connection Implicit DB Connection
	  * @return This service's data, along with related settings
	  */
	def withSettings(implicit connection: Connection) = access.withSettings
	
	
	// IMPLEMENTED  --------------------
	
	override def toString = target match
	{
		case Left(name) => name
		case Right(id) => s"Service $id"
	}
	
	
	// OTHER    ------------------------
	
	private def idForName(name: String)(implicit connection: Connection) =
		DbServiceId.forName(name.replaceAll("%", ""))
}

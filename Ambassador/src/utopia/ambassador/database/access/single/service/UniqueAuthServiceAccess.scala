package utopia.ambassador.database.access.single.service

import java.time.Instant
import utopia.ambassador.database.factory.service.{AuthServiceFactory, AuthServiceWithSettingsFactory}
import utopia.ambassador.database.model.service.AuthServiceModel
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthServices.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthServiceAccess 
	extends SingleRowModelAccess[AuthService] 
		with DistinctModelAccess[AuthService, Option[AuthService], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Name of this service (from the customer's perspective). None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	/**
	  * Time when this AuthService was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthServiceModel
	
	/**
	  * @param connection Implicit DB Connection
	  * @return This service, along with its settings
	  */
	def withSettings(implicit connection: Connection) = globalCondition match
	{
		case Some(c) => AuthServiceWithSettingsFactory.find(c)
		case None => AuthServiceWithSettingsFactory.any
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthService instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthService instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the name of the targeted AuthService instance(s)
	  * @param newName A new name to assign
	  * @return Whether any AuthService instance was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}


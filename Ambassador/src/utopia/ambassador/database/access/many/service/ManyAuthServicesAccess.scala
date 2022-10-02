package utopia.ambassador.database.access.many.service

import java.time.Instant
import utopia.ambassador.database.factory.service.AuthServiceFactory
import utopia.ambassador.database.model.service.AuthServiceModel
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyAuthServicesAccess
{
	// NESTED	--------------------
	
	private class ManyAuthServicesSubView(override val parent: ManyRowModelAccess[AuthService], 
		override val filterCondition: Condition) 
		extends ManyAuthServicesAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthServices at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthServicesAccess
	extends ManyRowModelAccess[AuthService] with Indexed with FilterableView[ManyAuthServicesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * names of the accessible AuthServices
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn)
		.flatMap { value => value.string }
	
	/**
	  * creationTimes of the accessible AuthServices
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthServiceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceFactory
	
	override def filter(additionalCondition: Condition): ManyAuthServicesAccess = 
		new ManyAuthServicesAccess.ManyAuthServicesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthService instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthService instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the name of the targeted AuthService instance(s)
	  * @param newName A new name to assign
	  * @return Whether any AuthService instance was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}


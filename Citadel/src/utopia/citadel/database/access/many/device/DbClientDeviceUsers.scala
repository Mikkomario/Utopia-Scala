package utopia.citadel.database.access.many.device

import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple ClientDeviceUsers at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbClientDeviceUsers extends ManyClientDeviceUsersAccess with NonDeprecatedView[ClientDeviceUser]
{
	// COMPUTED --------------------
	
	/**
	  * @return An access point to client device users where historical (deprecated) connections are also included
	  */
	def includingHistory = DbAllClientDeviceUsers
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted ClientDeviceUsers
	  * @return An access point to ClientDeviceUsers with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbClientDeviceUsersSubset(ids)
	
	
	// NESTED	--------------------
	
	object DbAllClientDeviceUsers extends ManyClientDeviceUsersAccess with UnconditionalView
	
	class DbClientDeviceUsersSubset(targetIds: Set[Int]) extends ManyClientDeviceUsersAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}


package utopia.exodus.database.access.many.auth

import utopia.exodus.model.stored.auth.DeviceToken
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple DeviceTokens at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbDeviceTokens extends ManyDeviceTokensAccess with NonDeprecatedView[DeviceToken]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted DeviceTokens
	  * @return An access point to DeviceTokens with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbDeviceTokensSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbDeviceTokensSubset(targetIds: Set[Int]) extends ManyDeviceTokensAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}


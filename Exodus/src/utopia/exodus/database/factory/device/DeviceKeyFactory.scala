package utopia.exodus.database.factory.device

import utopia.exodus.database.Tables
import utopia.exodus.model.partial.DeviceKeyData
import utopia.exodus.model.stored.DeviceKey
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading device key data from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
object DeviceKeyFactory extends FromValidatedRowModelFactory[DeviceKey] with Deprecatable
{
	// IMPLEMENTED	------------------------------
	
	override val nonDeprecatedCondition = table("deprecatedAfter").isNull
	
	override protected def fromValidatedModel(model: Model[Constant]) = DeviceKey(model("id").getInt,
		DeviceKeyData(model("userId").getInt, model("deviceId").getInt, model("key").getString))
	
	override def table = Tables.deviceAuthKey
}

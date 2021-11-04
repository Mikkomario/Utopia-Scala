package utopia.exodus.database.factory.device

import utopia.exodus.database.{ExodusTables, Tables}
import utopia.exodus.model.partial.DeviceKeyData
import utopia.exodus.model.stored.DeviceKey
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading device key data from the database
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
object DeviceKeyFactory extends FromValidatedRowModelFactory[DeviceKey] with Deprecatable
{
	// IMPLEMENTED	------------------------------
	
	override val nonDeprecatedCondition = table("deprecatedAfter").isNull
	
	override protected def fromValidatedModel(model: Model) = DeviceKey(model("id").getInt,
		DeviceKeyData(model("userId").getInt, model("deviceId").getInt, model("key").getString))
	
	override def table = ExodusTables.deviceAuthKey
}

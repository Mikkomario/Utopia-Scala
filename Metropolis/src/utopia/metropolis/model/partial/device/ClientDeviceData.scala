package utopia.metropolis.model.partial.device

import utopia.flow.collection.template.typeless

import java.time.Instant
import utopia.flow.datastructure.template
import utopia.flow.generic.model
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.time.Now

import scala.util.Success

@deprecated("This class will be removed in a future release", "v2.1")
object ClientDeviceData extends FromModelFactory[ClientDeviceData]
{
	override def apply(model: model.template.Model[Property]) =
		Success(ClientDeviceData(model("creator_id").int, model("created").getInstant))
}

/**
  * Represents a device (e.g. a browser or a computer) a user uses to interact with this service
  * @param creatorId Id of the user who added this device, if known
  * @param created Time when this ClientDevice was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
case class ClientDeviceData(creatorId: Option[Int] = None, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("creator_id" -> creatorId, "created" -> created))
}


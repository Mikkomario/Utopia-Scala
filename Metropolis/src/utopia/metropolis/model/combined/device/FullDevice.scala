package utopia.metropolis.model.combined.device

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.view.template.Extender
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.device.ClientDevice

@deprecated("Replaced with DetailedClientDevice", "v2.0")
object FullDevice extends FromModelFactory[FullDevice]
{
	// IMPLEMENTED	----------------------
	
	override def apply(model: template.ModelLike[Property]) =
		DescribedClientDevice(model).map { device =>
			FullDevice(device, model("user_ids").getVector.flatMap { _.int }.toSet)
		}
}

/**
  * Contains basic device information with descriptions and associated user ids
  * @author Mikko Hilpinen
  * @since 19.6.2020, v1
  */
@deprecated("Replaced with DetailedClientDevice", "v2.0")
case class FullDevice(describedDevice: DescribedClientDevice, userIds: Set[Int])
	extends Extender[ClientDevice] with ModelConvertible with SimplyDescribed
{
	// COMPUTED -----------------------------
	
	/**
	  * @return The wrapped device
	  */
	def device = describedDevice.wrapped
	
	
	// IMPLEMENTED	-------------------------
	
	override def wrapped = describedDevice.wrapped
	
	override def descriptions = describedDevice.descriptions
	
	override def toModel = describedDevice.toModel + Constant("user_ids", userIds.toVector)
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = Model(Vector(
		"id" -> device.id, "user_ids" -> userIds.toVector
	))
}

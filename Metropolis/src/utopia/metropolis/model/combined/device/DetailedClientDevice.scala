package utopia.metropolis.model.combined.device

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.device.ClientDeviceUser

@deprecated("This class will be removed in a future release", "v2.1")
object DetailedClientDevice extends FromModelFactory[DetailedClientDevice]
{
	// IMPLEMENTED	----------------------
	
	override def apply(model: template.Model[Property]) =
		DescribedClientDevice(model).map { device =>
			apply(device,
				model("user_links").getVector.flatMap { _.model }.flatMap { ClientDeviceUser(_).toOption }.toSet)
		}
}

/**
  * Contains basic device information with descriptions and associated user ids
  * @author Mikko Hilpinen
  * @since 19.6.2020, v1
  */
@deprecated("This class will be removed in a future release", "v2.1")
case class DetailedClientDevice(describedDevice: DescribedClientDevice, userLinks: Set[ClientDeviceUser])
	extends Extender[DescribedClientDevice] with ModelConvertible with DescribedSimpleModelConvertible
{
	// COMPUTED -----------------------------
	
	/**
	  * @return Id of this device
	  */
	def id = device.id
	/**
	  * @return The wrapped device
	  */
	def device = describedDevice.wrapped
	
	/**
	  * @return Ids of the users who are using this device
	  */
	def userIds = userLinks.filter { _.isValid }.map { _.userId }
	
	
	// IMPLEMENTED	-------------------------
	
	override def wrapped = describedDevice
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		wrapped.toSimpleModelUsing(descriptionRoles) + Constant("user_ids", userIds.toVector.sorted)
	
	override def toModel =
		describedDevice.toModel + Constant("user_links", userLinks.toVector.map { _.toModel })
}

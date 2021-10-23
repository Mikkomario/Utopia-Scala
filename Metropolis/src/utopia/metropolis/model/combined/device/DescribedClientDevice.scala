package utopia.metropolis.model.combined.device

import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.device.ClientDevice

object DescribedClientDevice extends DescribedFromModelFactory[ClientDevice, DescribedClientDevice]
{
	override protected def undescribedFactory = ClientDevice
}

/**
  * Combines ClientDevice with the linked descriptions
  * @param clientDevice ClientDevice to wrap
  * @param descriptions Descriptions concerning the wrapped ClientDevice
  * @since 2021-10-23
  */
case class DescribedClientDevice(clientDevice: ClientDevice, descriptions: Set[LinkedDescription])
	extends DescribedWrapper[ClientDevice] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = clientDevice
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}


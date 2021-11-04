package utopia.citadel.database.access.many.device

import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.metropolis.model.combined.device.DescribedClientDevice
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple ClientDevices at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbClientDevices extends ManyClientDevicesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted ClientDevices
	  * @return An access point to ClientDevices with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbClientDevicesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbClientDevicesSubset(override val ids: Set[Int]) 
		extends ManyClientDevicesAccess with ManyDescribedAccessByIds[ClientDevice, DescribedClientDevice]
}


package utopia.citadel.model.enumeration

import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper

/**
  * A utility object for accessing the description role ids introduced in the Citadel project
  * @author Mikko Hilpinen
  * @since 12.10.2021, v1.3
  */
object CitadelDescriptionRole
{
	/**
	  * Name description role id wrapper
	  */
	case object Name extends DescriptionRoleIdWrapper
	{
		override val id = 1
	}
}

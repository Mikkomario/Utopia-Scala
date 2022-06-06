package utopia.ambassador.model.enumeration

import utopia.exodus.model.enumeration.ScopeIdWrapper

/**
  * An enumeration for (client side) scopes introduced in the Ambassador module
  * @author Mikko Hilpinen
  * @since 22.2.2022, v2.1
  */
trait AmbassadorScope extends ScopeIdWrapper

object AmbassadorScope
{
	/**
	  * Scope that allows the user to initiate and interact with the OAuth process
	  */
	case object OAuth extends AmbassadorScope { override val id = 16 }
}

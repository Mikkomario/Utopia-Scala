package utopia.exodus.model.enumeration

/**
  * An enumeration for some standard (but not all) user roles. Used in the Exodus request handling.
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
sealed trait StandardUserRole
{
	/**
	  * @return Row id of this role in the DB
	  */
	def id: Int
}

@deprecated("Please use the Citadel version instead", "v2.0")
object StandardUserRole
{
	/**
	  * Owners have read/write access to all data in an organization
	  */
	@deprecated("Please use the Citadel version instead", "v2.0")
	case object Owner extends StandardUserRole
	{
		override val id = 1
	}
	
	/**
	  * Admins have most of the same rights as the owners, except that they can't change ownership or delete
	  * an organization
	  */
	@deprecated("Please use the Citadel version instead", "v2.0")
	case object Admin extends StandardUserRole
	{
		override val id = 2
	}
}

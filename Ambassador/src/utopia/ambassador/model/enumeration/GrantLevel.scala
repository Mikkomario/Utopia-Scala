package utopia.ambassador.model.enumeration

/**
  * An enumeration for different levels of access the user may have provided
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
sealed trait GrantLevel
{
	// ABSTRACT -------------------
	
	/**
	  * @return Whether the user granted access to the 3rd party system (at least partially)
	  */
	def grantedAccess: Boolean
	/**
	  * @return Whether access was actually achieved (tokens were acquired)
	  */
	def enablesAccess: Boolean
	/**
	  * @return Whether this grant level provided access to all requested scopes
	  */
	def isFull: Boolean
}

object GrantLevel
{
	/**
	  * Used when user grants access that matches the requested scopes
	  */
	case object FullAccess extends GrantLevel
	{
		override def grantedAccess = true
		override def enablesAccess = true
		override def isFull = true
	}
	/**
	  * Used when user grants access but the granted access doesn't contain all requested scopes
	  */
	case object PartialAccess extends GrantLevel
	{
		override def grantedAccess = true
		override def enablesAccess = true
		override def isFull = false
	}
	/**
	  * Used when user denies access
	  */
	case object AccessDenied extends GrantLevel
	{
		override def grantedAccess = false
		override def enablesAccess = false
		override def isFull = false
	}
	/**
	  * Used when user provides (some) access, but it couldn't be acquired due to some other problem
	  */
	case object AccessFailed extends GrantLevel
	{
		override def grantedAccess = true
		override def enablesAccess = false
		override def isFull = false
	}
}

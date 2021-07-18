package utopia.ambassador.model.enumeration

/**
  * An enumeration for different types of completion redirect urls / results the client may request
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
sealed trait AuthCompletionType
{
	/**
	  * @return Name of a json key this completion redirect url is read from
	  */
	def keyName: String
	/**
	  * @return Whether this redirect is restricted to successes (true), failures (false) or neither (None)
	  */
	def successFilter: Option[Boolean]
	/**
	  * @return Whether this redirect is restricted to failures due to user denying access (true)
	  */
	def deniedFilter: Boolean
	/**
	  * @return A relative priority of this completion type. Types with higher priority are used when possible.
	  */
	def priorityIndex: Int
}

object AuthCompletionType
{
	// ATTRIBUTES   -----------------------------
	
	/**
	  * All values of this enumeration
	  */
	lazy val values = Vector[AuthCompletionType](Default, Success, PartialSuccess, Failure, DenialOfAccess)
	
	
	// NESTED   ---------------------------------
	
	/**
	  * The default redirect url type used when other cases haven't been covered
	  */
	case object Default extends AuthCompletionType
	{
		override def keyName = "default"
		override def priorityIndex = 0
		
		override def successFilter = None
		override def deniedFilter = false
	}
	/**
	  * Redirect url type that covers all successful authentications
	  */
	case object Success extends AuthCompletionType
	{
		override def keyName = "success"
		override def priorityIndex = 1
		
		override def successFilter = Some(true)
		override def deniedFilter = false
	}
	/**
	  * Redirect url type that is limited to successful cases where the user denied access to some of
	  * the requested scopes
	  */
	case object PartialSuccess extends AuthCompletionType
	{
		override def keyName = "partial_success"
		override def successFilter = Some(true)
		override def deniedFilter = true
		override def priorityIndex = 2
	}
	/**
	  * Redirect url type that covers all failures regardless of type, although
	  * DenialOfAccess may be used instead if specified and applicable
	  */
	case object Failure extends AuthCompletionType
	{
		override def keyName = "failure"
		override def priorityIndex = 3
		override def successFilter = Some(false)
		override def deniedFilter = false
	}
	/**
	  * Redirect url type that specifically targets failures due to user denying
	  * access to requested scopes
	  */
	case object DenialOfAccess extends AuthCompletionType
	{
		override def keyName = "denial_of_access"
		override def priorityIndex = 4
		override def successFilter = Some(false)
		override def deniedFilter = true
	}
}

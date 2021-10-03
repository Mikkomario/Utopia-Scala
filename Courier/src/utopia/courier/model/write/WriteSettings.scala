package utopia.courier.model.write

import utopia.courier.model.{Authentication, EmailSettings}

trait WriteSettings extends EmailSettings
{
	// ABSTRACT ---------------------------------
	
	/**
	  * @return Port used when connecting to the service (optional)
	  */
	def port: Option[Int]
	/**
	  * @return Authentication used when connecting to the service (optional)
	  */
	def authentication: Option[Authentication]
	
	/**
	  * @return Modifiers applied in addition to the common write modifiers (host, port, use auth)
	  */
	protected def customModifiers: Map[String, AnyRef]
	
	
	// COMPUTED ----------------------------------
	
	/**
	  * @return Whether these settings authenticate the connection
	  */
	def isAuthenticated = authentication.isDefined
	
	/**
	  * @return The standard property modifiers to use with these settings
	  */
	private def baseModifiers: Map[String, AnyRef] =
	{
		val base = Map("mail.smtp.host" -> hostAddress) ++ port.map { p => "mail.smtp.port" -> (p: java.lang.Integer) }
		if (isAuthenticated)
			base + ("mail.smtp.auth" -> (true: java.lang.Boolean))
		else
			base
	}
	
	
	// IMPLEMENTED  ------------------------------
	
	override def properties = baseModifiers ++ customModifiers
}

/**
  * Used for setting properties for message writing
  * @author Mikko Hilpinen
  * @since 10.9.2021, v0.1
  */
object WriteSettings
{
	// OTHER    ---------------------------------
	
	/**
	  * Creates a new secure write settings instance (SMPTS)
	  * @param hostAddress Address of the emailing service
	  * @param port Port number used when accessing the service
	  * @param authentication Authentication used
	  * @return A new settings instance
	  */
	def apply(hostAddress: String, port: Int, authentication: Authentication) =
		SecureWriteSettings(hostAddress, port, authentication)
	
	/**
	  * Creates a new basic write settings instance (SMTP)
	  * @param hostAddress Address of the emailing service
	  * @param port Port number used when accessing the service (optional)
	  * @param authentication Authentication used (optional)
	  * @return A new settings instance
	  */
	def simple(hostAddress: String, port: Option[Int] = None, authentication: Option[Authentication] = None) =
		SimpleWriteSettings(hostAddress, port, authentication)
	
	
	// NESTED   ---------------------------------
	
	/**
	  * Used for sending email through SMTP
	  * @param hostAddress Email service address
	  * @param port Port used for accessing the service (optional)
	  * @param authentication Authentication used when sending messages (optional)
	  */
	case class SimpleWriteSettings(hostAddress: String, port: Option[Int] = None,
	                               authentication: Option[Authentication] = None) extends WriteSettings
	{
		override protected def customModifiers = Map()
		
		override def removedProperties = Set()
	}
	
	/**
	  * Used for sending email through SMTPS
	  * @param hostAddress Email service address
	  * @param accessPort Port used for accessing the service
	  * @param auth Authentication used when accessing the service
	  */
	case class SecureWriteSettings(hostAddress: String, accessPort: Int, auth: Authentication) extends WriteSettings
	{
		override def port = Some(accessPort)
		
		override def authentication = Some(auth)
		
		override protected def customModifiers = Map(
			"mail.smtp.ssl.enable" -> (true: java.lang.Boolean),
			"mail.smtp.starttls.enable" -> (true: java.lang.Boolean),
			"mail.transport.protocol" -> "smtps", "mail.smtp.socketFactory.fallback" -> (true: java.lang.Boolean))
		
		override def removedProperties = Set("mail.smtp.socketFactory.class")
	}
}
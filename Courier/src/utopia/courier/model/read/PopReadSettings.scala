package utopia.courier.model.read

import utopia.courier.model.Authentication

/**
  * Represents email reading settings used with the POP3 -protocol
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
case class PopReadSettings(hostAddress: String, authentication: Authentication)
	extends ReadSettings
{
	override def storeName = "pop3"
	
	override def properties = Map(
		"mail.pop3.host" -> hostAddress,
		"mail.pop3.user" -> authentication.user,
		"mail.pop3.socketFactory" -> (995: java.lang.Integer),
		"mail.pop3.socketFactory.class" -> "javax.net.ssl.SSLSocketFactory",
		"mail.pop3.port" -> (995: java.lang.Integer)
	)
}

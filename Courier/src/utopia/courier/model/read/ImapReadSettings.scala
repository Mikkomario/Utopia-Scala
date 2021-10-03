package utopia.courier.model.read

import com.sun.mail.util.MailSSLSocketFactory
import utopia.courier.model.Authentication
import utopia.courier.model.read.ImapReadSettings.socketFactory

import scala.util.Try

object ImapReadSettings
{
	private lazy val socketFactory = Try {
		val sf = new MailSSLSocketFactory()
		sf.setTrustAllHosts(true)
		sf
	}
}

/**
  * Represents email reading settings used in the IMAP protocol
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @param hostAddress Address of the email service
  * @param authentication Authentication used when accessing the emailing service
  */
case class ImapReadSettings(hostAddress: String, authentication: Authentication)
	extends ReadSettings
{
	override def storeName = "imap"
	
	// NB: May throw if socketFactory instance couldn't be created
	override def properties = Map(
		"mail.imap.ssl.trust" -> (true: java.lang.Boolean),
		"mail.imap.ssl.socketFactory" -> socketFactory.get,
		"mail.imap.socketFactory.port" -> (993: java.lang.Integer),
		"mail.imap.socketFactory.class" -> "javax.net.ssl.SSLSocketFactory",
		"mail.imap.socketFactory.fallback" -> (false: java.lang.Boolean),
		"mail.imap.com" -> hostAddress,
		"mail.imap.starttls.enable" -> (true: java.lang.Boolean),
		"mail.imap.auth" -> (true: java.lang.Boolean)
	)
}

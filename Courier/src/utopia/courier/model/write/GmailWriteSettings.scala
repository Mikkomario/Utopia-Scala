package utopia.courier.model.write

import utopia.courier.model.Authentication

object GmailWriteSettings
{
	/**
	  * @param email Your gmail email address
	  * @param password Your gmail password
	  * @return A new set of settings
	  */
	def apply(email: String, password: String): GmailWriteSettings = apply(Authentication(email, password))
}

/**
  * Email write settings used when sending mail through gmail. Please note that, if you're using
  * 2-factor authentication, you need to follow the instructions on this page and use app-passwords:
  * https://www.gmass.co/blog/gmail-smtp/
  * If you're not using 2-factor authentication, you may still have to enable less secure applications in your
  * gmail / Google settings (you will receive a warning email when attempting to send email otherwise)
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
case class GmailWriteSettings(auth: Authentication) extends WriteSettings
{
	override def port = Some(465)
	
	override def authentication = Some(auth)
	
	override def hostAddress = "smtp.gmail.com"
	
	override def removedProperties = Set()
	
	override protected def customModifiers = Map(
		"mail.smtp.socketFactory.port" -> "465",
		"mail.smtp.socketFactory.class" -> "javax.net.ssl.SSLSocketFactory"
	)
}

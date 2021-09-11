package utopia.courier.model.write

import utopia.courier.model.Authentication

/**
  * Email write settings used when sending mail through gmail
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

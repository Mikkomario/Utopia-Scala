package utopia.courier.model

import utopia.flow.time.Now

import java.time.Instant
import javax.mail.Message.RecipientType

/**
  * Represents email context information
  * @author Mikko Hilpinen
  * @since 10.9.2021, v0.1
  * @param sender Email address of the sender of the described message
  * @param recipients Message recipient email addresses, grouped by recipient type (default = empty)
  * @param sendTime Time when this message was sent / created (default = now)
  * @param receiveTime Time when this message was received (default = now)
  */
case class EmailHeaders(sender: String, recipients: Map[RecipientType, Vector[String]] = Map(),
                        customReplyTo: Option[String] = None, sendTime: Instant = Now, receiveTime: Instant = Now)
{
	/**
	  * @return The email address that is listed as the one to receive the replies of the message
	  */
	def replyTo = customReplyTo.getOrElse(sender)
}

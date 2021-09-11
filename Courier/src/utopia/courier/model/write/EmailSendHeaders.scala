package utopia.courier.model.write

import javax.mail.Message.RecipientType

/**
  * Represents email headers used when sending messages
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @param sender Email address of the sender of the described message
  * @param recipients Message recipient email addresses, grouped by recipient type (default = empty)
  */
case class EmailSendHeaders(sender: String, recipients: Map[RecipientType, Vector[String]] = Map(),
                            customReplyTo: Option[String] = None)
{
	/**
	  * @return Email address that should receive the replies to this message
	  */
	def replyTo = customReplyTo.getOrElse(sender)
}

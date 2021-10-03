package utopia.courier.model

import utopia.courier.model.write.Recipients
import utopia.flow.time.Now

import utopia.flow.util.StringExtensions._

import java.time.Instant

object EmailHeaders
{
	/**
	  * Creates a new set of headers for outgoing mail
	  * @param sender Message sender (your email address)
	  * @param recipients Message recipients
	  * @param subject Message subject (default = "No Subject")
	  * @param customReplyTo A custom reply-to address (default = empty = same as sender)
	  * @return A new set of email headers
	  */
	def outgoing(sender: String, recipients: Recipients, subject: String = "No Subject", customReplyTo: String = "") =
		apply(sender, recipients, subject, customReplyTo.notEmpty)
	
	/**
	  * Creates a new set of headers for incoming mail
	  * @param sender Message sender
	  * @param subject Message subject
	  * @param sendTime Message send time
	  * @param recipients Message recipients (default = empty)
	  * @param replyTo Reply-to address (default = empty = same as sender)
	  * @param receiveTime Message receive time (default = now)
	  * @return A new set of email headers
	  */
	def incoming(sender: String, subject: String, sendTime: Instant, recipients: Recipients = Recipients.empty,
	             replyTo: String = "", receiveTime: Instant = Now) =
		apply(sender, recipients, subject, replyTo.notEmpty.filterNot { _ == sender }, sendTime, receiveTime)
}

/**
  * Contains information about an email without including the message body
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
case class EmailHeaders(sender: String, recipients: Recipients = Recipients.empty, subject: String = "No Subject",
                        customReplyTo: Option[String] = None, sendTime: Instant = Now, receiveTime: Instant = Now)
	extends EmailHeadersLike
{
	/**
	  * @return The reply-to address in these headers
	  */
	override def replyTo = customReplyTo.getOrElse(sender)
}

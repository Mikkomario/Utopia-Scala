package utopia.courier.model

import utopia.courier.model.write.Recipients
import utopia.flow.collection.immutable.Empty
import utopia.flow.time.Now

import java.time.Instant
import java.util.UUID

object EmailHeaders
{
	// OTHER    ------------------------------
	
	/**
	  * Creates a new set of email headers
	  * @param sender Email sender address
	  * @param recipients Email recipients
	  * @param subject Email subject (default = "No Subject")
	  * @param messageId Unique message id (default = random UUID)
	  * @param inReplyTo Id of the message this message is a reply to. Empty if not a reply.
	  * @param references Ids of the referenced parent messages, from oldest to most recent.
	  * @param replyTo Custom reply-to field.
	  * @param sendTime Time when this message was sent
	  * @param receiveTime Time when this message was received
	  * @return A new set of email headers
	  */
	def apply(sender: EmailAddress, recipients: Recipients = Recipients.empty, subject: String = "No Subject",
	          messageId: String = UUID.randomUUID().toString, inReplyTo: String = "",
	          references: Seq[String] = Empty,
	          replyTo: Option[EmailAddress] = None, sendTime: Instant = Now, receiveTime: Instant = Now): EmailHeaders =
		_EmailHeaders(sender, recipients, subject, messageId, inReplyTo, references, replyTo, sendTime, receiveTime)
	
	/**
	  * Creates a new set of headers for outgoing mail
	  * @param sender Message sender (your email address)
	  * @param recipients Message recipients
	  * @param subject Message subject (default = "No Subject")
	  * @param messageId Unique message id (default = empty = do not assign a custom id)
	  * @param inReplyTo Id of the message this message is a reply to. Empty if not a reply.
	  * @param references Ids of the referenced parent messages, from oldest to most recent.
	  * @param replyTo Custom reply-to field.
	  * @return A new set of email headers
	  */
	def outgoing(sender: EmailAddress, recipients: Recipients, subject: String = "No Subject",
	             messageId: String = "", inReplyTo: String = "",
	             references: Seq[String] = Empty, replyTo: Option[EmailAddress] = None) =
		apply(sender, recipients, subject, messageId, inReplyTo, references, replyTo)
	
	/**
	  * Creates a new set of headers for incoming mail
	  * @param sender Message sender
	  * @param subject Message subject
	  * @param messageId Unique message id
	  * @param sendTime Message send time
	  * @param recipients Message recipients (default = empty)
	  * @param inReplyTo Id of the message this message is a reply to. Empty if not a reply.
	  * @param references Ids of the referenced parent messages, from oldest to most recent.
	  * @param replyTo Reply-to address (default = empty = same as sender)
	  * @param receiveTime Message receive time (default = now)
	  * @return A new set of email headers
	  */
	def incoming(sender: EmailAddress, subject: String, messageId: String, sendTime: Instant,
	             recipients: Recipients = Recipients.empty, inReplyTo: String = "",
	             references: Seq[String] = Empty,
	             replyTo: Option[EmailAddress] = None, receiveTime: Instant = Now) =
		apply(sender, recipients, subject, messageId, inReplyTo, references, replyTo.filterNot { _ == sender },
			sendTime, receiveTime)
	
	
	// NESTED   ----------------------------
	
	case class _EmailHeaders(sender: EmailAddress, recipients: Recipients, subject: String,
	                        messageId: String = UUID.randomUUID().toString, inReplyTo: String,
	                        references: Seq[String],
	                        customReplyTo: Option[EmailAddress], sendTime: Instant, receiveTime: Instant)
		extends EmailHeaders
	{
		override def replyTo = customReplyTo.getOrElse(sender)
	}
}

/**
  * A common trait for email header set implementations
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
trait EmailHeaders
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Unique id of this message
	  */
	def messageId: String
	
	/**
	  * @return Email sender email address
	  */
	def sender: EmailAddress
	
	/**
	  * @return Email recipients
	  */
	def recipients: Recipients
	
	/**
	  * @return Reply-to email address
	  */
	def replyTo: EmailAddress
	
	/**
	  * @return Id of the message to which this message is a reply.
	  *         Empty if this is not a reply.
	  */
	def inReplyTo: String
	/**
	  * @return Ids of the messages referenced by this message, from oldest to most recent
	  */
	def references: Seq[String]
	
	/**
	  * @return Email subject
	  */
	def subject: String
	
	/**
	  * @return Email send time
	  */
	def sendTime: Instant
	
	/**
	  * @return Email receive time
	  */
	def receiveTime: Instant
}
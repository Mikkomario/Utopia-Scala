package utopia.courier.controller.read

import utopia.courier.model.EmailHeaders
import utopia.courier.model.write.Recipients
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._

import java.time.Instant

/**
  * A view to email headers, which is initialized lazily
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
class LazyEmailHeadersView(getSender: => String, getSubject: => String, getMessageId: => String, getSendTime: => Instant,
                           getRecipients: => Recipients = Recipients.empty, getInReplyTo: => String,
                           getReferences: => Vector[String], geReplyTo: => String = "",
                           override val receiveTime: Instant = Now)
	extends EmailHeaders
{
	// ATTRIBUTES   ---------------------------------
	
	override lazy val sender = getSender
	override lazy val recipients = getRecipients
	override lazy val subject = getSubject
	override lazy val replyTo = geReplyTo.notEmpty.getOrElse(sender)
	override lazy val sendTime = getSendTime
	override lazy val messageId: String = getMessageId
	override lazy val inReplyTo: String = getInReplyTo
	override lazy val references: Vector[String] = getReferences
	
	
	// COMPUTED ------------------------------------
	
	/**
	  * @return A fully cached / read headers instance based on this view
	  */
	def toHeaders = EmailHeaders.incoming(sender, subject, messageId, sendTime, recipients, inReplyTo, references,
		replyTo, receiveTime)
}

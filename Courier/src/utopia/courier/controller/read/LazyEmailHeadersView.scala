package utopia.courier.controller.read

import utopia.courier.model.write.Recipients
import utopia.courier.model.{EmailHeaders, EmailHeadersLike}
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._

import java.time.Instant

/**
  * A view to email headers, which is initialized lazily
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
class LazyEmailHeadersView(getSender: => String, getSubject: => String, getSendTime: => Instant,
                           getRecipients: => Recipients = Recipients.empty, geReplyTo: => String = "",
                           override val receiveTime: Instant = Now)
	extends EmailHeadersLike
{
	// ATTRIBUTES   ---------------------------------
	
	override lazy val sender = getSender
	override lazy val recipients = getRecipients
	override lazy val subject = getSubject
	override lazy val replyTo = geReplyTo.notEmpty.getOrElse(sender)
	override lazy val sendTime = getSendTime
	
	
	// COMPUTED ------------------------------------
	
	/**
	  * @return A fully cached / read headers instance based on this view
	  */
	def toHeaders = EmailHeaders.incoming(sender, subject, sendTime, recipients, replyTo, receiveTime)
}

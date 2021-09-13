package utopia.courier.model

import utopia.courier.model.write.Recipients

import java.time.Instant

/**
  * A common trait for email header set implementations
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
trait EmailHeadersLike
{
	/**
	  * @return Email sender email address
	  */
	def sender: String
	/**
	  * @return Email recipients
	  */
	def recipients: Recipients
	/**
	  * @return Email subject
	  */
	def subject: String
	/**
	  * @return Reply-to email address
	  */
	def replyTo: String
	/**
	  * @return Email send time
	  */
	def sendTime: Instant
	/**
	  * @return Email receive time
	  */
	def receiveTime: Instant
}

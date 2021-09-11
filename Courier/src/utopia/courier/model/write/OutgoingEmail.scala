package utopia.courier.model.write

import utopia.courier.model.EmailContent
import utopia.flow.util.Extender

/**
  * Represents an email being sent
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
case class OutgoingEmail(body: EmailContent, headers: EmailSendHeaders) extends Extender[EmailContent]
{
	override def wrapped = body
}

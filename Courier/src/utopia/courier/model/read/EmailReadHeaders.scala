package utopia.courier.model.read

import utopia.flow.time.Now

import java.time.Instant

/**
  * Represents email message headers that apply to received messages
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @param sender Email address of the sender of the described message
  * @param sendTime Time when this message was sent / created
  * @param receiveTime Time when this message was received (default = now)
  */
case class EmailReadHeaders(sender: String, sendTime: Instant, receiveTime: Instant = Now)

package utopia.echo.model.response

import utopia.flow.time.Now

import java.time.Instant

object BufferedReply
{
	def empty = apply("", ResponseStatistics.empty, Now)
}

/**
  * A reply from an LLM that has been completely read
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
case class BufferedReply(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now)

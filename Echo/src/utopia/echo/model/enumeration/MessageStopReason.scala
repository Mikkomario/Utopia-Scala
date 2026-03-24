package utopia.echo.model.enumeration

/**
 * An enumeration for different causes of a message completion or end
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.5
 */
sealed trait MessageStopReason
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Key used for representing this stop reason
	 */
	def key: String
	
	
	// IMPLEMENTED  -------------------
	
	override def toString = key
}

object MessageStopReason
{
	// ATTRIBUTES   -------------------
	
	/**
	 * All message stop reason values
	 */
	val values = Vector[MessageStopReason](MessageCompleted, LengthLimitReached, ToolCalled, Censored, ServerError)
	
	
	// VALUES   -----------------------
	
	/**
	 * Indicates that a stop sequence was encountered, and the message completed naturally.
	 */
	case object MessageCompleted extends MessageStopReason
	{
		override val key: String = "stop"
	}
	/**
	 * Indicates that the maximum context size was reached
	 */
	case object LengthLimitReached extends MessageStopReason
	{
		override val key: String = "length"
	}
	/**
	 * Indicates that the message stopped in order to call a tool
	 */
	case object ToolCalled extends MessageStopReason
	{
		override val key: String = "tool_calls"
	}
	/**
	 * Indicates that a message was blocked by server-side content moderation mechanism
	 */
	case object Censored extends MessageStopReason
	{
		override val key: String = "content_filter"
	}
	/**
	 * Indicates that the server was unable to complete the message for technical reasons
	 */
	case object ServerError extends MessageStopReason
	{
		override val key: String = "error"
	}
}

package utopia.reach.context

import utopia.firmament.context.TextContext

object PopupContext
{
	// IMPLICIT -------------------------
	
	/**
	  * @param textContext A text context to wrap
	  * @return A Pop-up creation context that uses the specified text context and default settings
	  */
	implicit def apply(textContext: TextContext): PopupContext =
		apply(ReachWindowContext(textContext.actorHandler, textContext.background).borderless.windowed, textContext)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param window Window creation context to wrap
	  * @param text Text context to wrap
	  * @return A new pop-up context
	  */
	def apply(window: ReachWindowContext, text: TextContext): PopupContext = _PopupContext(window, text)
	
	
	// NESTED   -------------------------
	
	private case class _PopupContext(reachWindowContext: ReachWindowContext, textContext: TextContext)
		extends PopupContext
	{
		override def self: PopupContext = this
		
		override def withTextContext(base: TextContext): PopupContext =
			if (base == textContext) self else copy(textContext = base)
		override def withReachWindowContext(base: ReachWindowContext): PopupContext =
			if (base == reachWindowContext) self else copy(reachWindowContext = base)
		
		override def *(mod: Double): PopupContext = withTextContext(textContext * mod)
	}
}

/**
  * Common trait for context items that are used for creating Reach Popup windows
  * (that contain or are related with textual elements)
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait PopupContext extends PopupContextLike[PopupContext] with TextContext with ReachWindowContext

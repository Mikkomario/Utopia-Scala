package utopia.reach.context

import utopia.firmament.context.text.{StaticTextContext, VariableTextContext}
import utopia.paradigm.color.Color

import scala.language.implicitConversions

object ReachContentWindowContext
{
	// IMPLICIT -------------------------
	
	/**
	  * @param textContext A text context to wrap
	  * @return A Pop-up creation context that uses the specified text context and default settings
	  */
	implicit def apply(textContext: StaticTextContext): ReachContentWindowContext =
		apply(ReachWindowContext(textContext.actorHandler, textContext.background).borderless.windowed, textContext)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param window Window creation context to wrap
	  * @param text Text context to wrap
	  * @return A new pop-up context
	  */
	def apply(window: ReachWindowContext, text: StaticTextContext): ReachContentWindowContext =
		_ReachContentWindowContext(window, text)
	
	
	// NESTED   -------------------------
	
	private case class _ReachContentWindowContext(reachWindowContext: ReachWindowContext, base: StaticTextContext)
		extends ReachContentWindowContext
	{
		override def self: ReachContentWindowContext = this
		
		override def current: StaticTextContext = this
		override def toVariableContext: VariableTextContext = base.toVariableContext
		
		override def withBase(base: StaticTextContext): ReachContentWindowContext = copy(base = base)
		override def withReachWindowContext(base: ReachWindowContext): ReachContentWindowContext =
			if (base == reachWindowContext) self else copy(reachWindowContext = base)
		
		override def *(mod: Double): ReachContentWindowContext = mapBase { _ * mod }
		
		override def withWindowBackground(bg: Color) = withBackground(bg)
		override def withBackground(background: Color) =
			copy(reachWindowContext.withWindowBackground(background), base.withBackground(background))
	}
}

/**
  * Common trait for context items that are used for creating Reach Popup windows
  * (that contain or are related with textual elements)
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContext
	extends ReachContentWindowContextLike[ReachContentWindowContext] with StaticTextContext with ReachWindowContext

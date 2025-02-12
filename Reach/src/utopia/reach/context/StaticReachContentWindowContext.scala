package utopia.reach.context

import utopia.firmament.context.text.{StaticTextContext, StaticTextContextLike, StaticTextContextWrapper}
import utopia.paradigm.color.Color

import scala.language.implicitConversions

object StaticReachContentWindowContext
{
	// IMPLICIT -------------------------
	
	/**
	  * @param textContext A text context to wrap
	  * @return A Pop-up creation context that uses the specified text context and default settings
	  */
	implicit def apply(textContext: StaticTextContext): StaticReachContentWindowContext =
		apply(ReachWindowContext(textContext.actorHandler, textContext.background).borderless.windowed, textContext)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param window Window creation context to wrap
	  * @param text Text context to wrap
	  * @return A new pop-up context
	  */
	def apply(window: ReachWindowContext, text: StaticTextContext): StaticReachContentWindowContext =
		_ReachContentWindowContext(window, text)
	
	
	// NESTED   -------------------------
	
	private case class _ReachContentWindowContext(windowContext: ReachWindowContext, base: StaticTextContext)
		extends StaticReachContentWindowContext
			with StaticTextContextWrapper[StaticTextContext, StaticReachContentWindowContext]
			with ReachWindowContextWrapper[ReachWindowContext, StaticReachContentWindowContext, StaticReachContentWindowContext]
	{
		override def self = this
		
		override def actorHandler = super[StaticTextContextWrapper].actorHandler
		override def backgroundPointer = super[StaticTextContextWrapper].backgroundPointer
		override def textColorPointer = super[StaticTextContextWrapper].textColorPointer
		override def hintTextColorPointer = super[StaticTextContextWrapper].hintTextColorPointer
		
		override def current = this
		override def toVariableContext = VariableReachContentWindowContext(windowContext, base.toVariableContext)
		
		override def withBase(base: StaticTextContext) = copy(base = base)
		override def withWindowContext(base: ReachWindowContext): StaticReachContentWindowContext =
			copy(windowContext = base)
		override def withContentContext(textContext: StaticTextContext): StaticReachContentWindowContext =
			copy(base = textContext)
		
		override def *(mod: Double) = mapBase { _ * mod }
		
		override def withWindowBackground(bg: Color) = withBackground(bg)
		override def withBackground(background: Color) =
			copy(windowContext.withWindowBackground(background), base.withBackground(background))
	}
}

/**
  * Provides for creating Reach windows as well as their contents.
  * This is a static implementation, where the context properties always remain fixed.
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.5
  */
trait StaticReachContentWindowContext
	extends StaticTextContext with StaticTextContextLike[StaticReachContentWindowContext]
		with ReachContentWindowContext
		with ReachContentWindowContextCopyable[StaticReachContentWindowContext, StaticReachContentWindowContext]
package utopia.reflection.component.context

/**
  * A common trait for contexts that wrap a text context
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
@deprecated("Moved to Firmament", "v2.0")
trait TextContextWrapper extends TextContextLike with ColorContextWrapper
{
	// ABSTRACT	----------------------------
	
	override def base: TextContextLike
	
	
	// IMPLEMENTED	------------------------
	
	override def betweenLinesMargin = base.betweenLinesMargin
	
	override def allowLineBreaks = base.allowLineBreaks
	
	override def allowTextShrink = base.allowTextShrink
	
	override def font = base.font
	
	override def promptFont = base.promptFont
	
	override def textAlignment = base.textAlignment
	
	override def textInsets = base.textInsets
	
	override def textColor = base.textColor
	
	override def localizer = base.localizer
}

package utopia.reflection.component.template.text

import java.awt.FontMetrics

import utopia.reflection.localization.LocalizedString
import utopia.reflection.text.{FontMetricsContext, MeasuredText}

/**
  * A common trait for components that present text and allow outside modifications to both content and styling
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
trait MutableTextComponent extends MutableStyleTextComponent
{
	// ABSTRACT	------------------------
	
	protected def measuredText_=(newText: MeasuredText): Unit
	
	/**
	  * @return Whether line breaks should be respected by default
	  */
	def allowLineBreaksByDefault: Boolean
	
	/**
	  * @return Font metrics used in this component
	  */
	def fontMetrics: FontMetrics
	
	
	// COMPUTED	------------------------
	
	def text_=(newText: LocalizedString): Unit = measuredText = MeasuredText(newText,
		FontMetricsContext(fontMetrics, drawContext.betweenLinesMargin), drawContext.alignment, allowLineBreaksByDefault)
}

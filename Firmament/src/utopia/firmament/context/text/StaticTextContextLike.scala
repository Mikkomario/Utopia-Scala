package utopia.firmament.context.text

import utopia.firmament.context.color.StaticColorContextLike
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font

/**
  * Common trait for static text context implementations
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.4
  */
trait StaticTextContextLike[+Repr] extends StaticColorContextLike[Repr, Repr] with TextContextCopyable[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Font used in prompts & possibly some hint elements
	  */
	def promptFont: Font
	/**
	  * @return Insets placed around text inside components
	  */
	def textInsets: StackInsets
	/**
	  * @return A threshold after which automatic line-splitting will be applied.
	  *         None if no automatic splitting should be applied.
	  */
	def lineSplitThreshold: Option[Double]
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Settings for drawing text
	  */
	def textDrawContext = TextDrawContext(font, textColor, textAlignment, textInsets, lineSplitThreshold,
		betweenLinesMargin.optimal, allowLineBreaks)
	/**
	  * @return Settings for drawing text hints
	  */
	def hintTextDrawContext = textDrawContext.withColor(hintTextColor)
	
	
	// IMPLEMENTED  -------------------
	
	override def promptFontPointer: Changing[Font] = Fixed(promptFont)
	override def textInsetsPointer: Changing[StackInsets] = Fixed(textInsets)
	override def lineSplitThresholdPointer: Option[Changing[Double]] = lineSplitThreshold.map(Fixed.apply)
	
	override def textDrawContextPointer: Changing[TextDrawContext] = Fixed(textDrawContext)
	override def hintTextDrawContextPointer: Changing[TextDrawContext] = Fixed(hintTextDrawContext)
	
	override def mapTextInsets(f: StackInsets => StackInsets): Repr = withTextInsets(f(textInsets))
	
	
	// OTHER    ----------------------
	
	/**
	  * @param hint Whether drawing a hint text
	  * @return Text draw context applicable for the specified purpose (hint (true) or default (false))
	  */
	def textDrawContextFor(hint: Boolean) = if (hint) hintTextDrawContext else textDrawContext
}

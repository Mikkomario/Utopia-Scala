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
  * @since 05.10.2024, v1.3.2
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
	
	
	// IMPLEMENTED  -------------------
	
	override def promptFontPointer: Changing[Font] = Fixed(promptFont)
	override def textInsetsPointer: Changing[StackInsets] = Fixed(textInsets)
	override def lineSplitThresholdPointer: Option[Changing[Double]] = lineSplitThreshold.map(Fixed.apply)
	
	override def textDrawContextPointer: Changing[TextDrawContext] = Fixed(textDrawContext)
	
	override def mapTextInsets(f: StackInsets => StackInsets): Repr = withTextInsets(f(textInsets))
}

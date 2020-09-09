package utopia.reflection.component.swing.display

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.{X, Y}
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.template.text.TextComponent
import utopia.reflection.container.stack.StackLayout.{Center, Leading, Trailing}
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.{AlignFrame, SwitchPanel}
import utopia.reflection.localization.{LocalString, LocalizedString}
import utopia.reflection.shape._
import utopia.reflection.shape.stack.modifier.StackSizeModifier
import utopia.reflection.shape.stack.{StackInsets, StackLength, StackSize}
import utopia.reflection.text.Font

object MultiLineTextView
{
	/**
	  * Creates a new contextual multi line text view (uses context.normalWidth as line split threshold)
	  * @param text Text displayed on the new view
	  * @param useLowPriorityForScalingSides Whether aligned sides should have low stack length priority (default = false)
	 *  @param isHint Whether prompt/hint styling should be used (default = false)
	  * @param context Component creation context
	  * @return A new multi line text view
	  */
	def contextual(text: LocalizedString, lineSplitThreshold: Double, useLowPriorityForScalingSides: Boolean = false,
	               isHint: Boolean = false)(implicit context: TextContextLike) =
		new MultiLineTextView(text, if (isHint) context.promptFont else context.font, lineSplitThreshold,
		context.textInsets, StackLength(0, context.margins.verySmall, context.margins.small), useLowPriorityForScalingSides,
		context.textAlignment, if (isHint) context.hintTextColor else context.textColor)
}

/**
  * Presents text using multiple lines
  * @author Mikko Hilpinen
  * @since 10.12.2019, v1+
  * @param initialText Initially displayed text
  * @param initialFont Initially used font
  * @param initialLineSplitThreshold Maximum line length before splitting text to another line (single word lines
  *                                  may still exceed threshold)
  * @param initialInsets insets placed around the text in this view (default = any, preferring 0)
  * @param betweenLinesMargin Margin placed between each line of text (default = fixed to 0)
  * @param useLowPriorityForScalingSides Whether low stack length priority should be used for sides which are affected
  *                                      by alignment (for example right side with left alignment) (default = false)
  * @param initialAlignment Initially used text and content alignment (default = left)
  * @param initialTextColor Initially used text color (default = slightly opaque black)
  */
class MultiLineTextView(initialText: LocalizedString, initialFont: Font, initialLineSplitThreshold: Double,
						initialInsets: StackInsets = StackInsets.any,
						val betweenLinesMargin: StackLength = StackLength.fixed(0),
						useLowPriorityForScalingSides: Boolean = false, initialAlignment: Alignment = Alignment.Left,
						initialTextColor: Color = Color.textBlack)
	extends StackableAwtComponentWrapperWrapper with TextComponent with CustomDrawableWrapper with AwtContainerRelated
{
	// ATTRIBUTES	------------------------
	
	private var _drawContext = TextDrawContext(initialFont, initialTextColor, initialAlignment, initialInsets)
	private var _text = initialText
	private var _lineSplitThreshold = initialLineSplitThreshold
	
	private val panel = new SwitchPanel[AlignFrame[Stack[TextLabel]]]
	
	
	// INITIAL CODE	------------------------
	
	component.setFont(initialFont.toAwt)
	
	// Sets initial content
	panel.set(makeNewContent())
	
	if (useLowPriorityForScalingSides)
		panel.addConstraint(new LowPriorityLengthConstraint)
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Maximum length of a multiple word line (in pixels)
	  */
	def lineSplitThreshold = _lineSplitThreshold
	def lineSplitThreshold_=(newThreshold: Double) =
	{
		_lineSplitThreshold = newThreshold
		resetContent()
	}
	
	
	// IMPLEMENTED	------------------------
	
	override def component = wrapped.component
	
	override def drawContext = _drawContext
	
	override def drawContext_=(newContext: TextDrawContext) =
	{
		if (newContext != _drawContext)
		{
			component.setFont(newContext.font.toAwt)
			// Updates the colors of each line label
			if (newContext.color != _drawContext.color)
				panel.content.foreach { _.content.foreach { _.components.foreach { _.textColor = newContext.color } } }
			
			_drawContext = newContext
			resetContent()
		}
	}
	
	override def drawable = panel
	
	override protected def wrapped = panel
	
	override def text = _text
	def text_=(newText: LocalizedString) =
	{
		if (_text != newText)
		{
			if (_text.string != newText.string)
			{
				_text = newText
				resetContent()
			}
			else
				_text = newText
		}
	}
	
	
	// OTHER	----------------------------
	
	private def resetContent() = panel.set(makeNewContent())
	
	private def makeNewContent() =
	{
		val stack =
		{
			if (text.string.isEmpty)
				Stack.column[TextLabel](cap = insets.vertical / 2)
			else
			{
				// Splits the text whenever target width is exeeded. The resulting lines determine the size constraints
				val lines = text.lines.flatMap { s => split(s.string) }
				// val maxLineWidth = lines.flatMap { textWidth(_) }.max
				
				// Creates new line components
				val language = text.languageCode
				val lineComponents = lines.map { line =>
					new TextLabel(LocalizedString(LocalString(line, language), None), font, textColor, insets.onlyHorizontal,
						alignment.horizontal) }
				
				// Places the lines in a stack
				// Stack layout depends from current alignment (horizontal)
				val stackLayout = alignment.horizontal match
				{
					case Alignment.Left => Leading
					case Alignment.Right => Trailing
					case _ => Center
				}
				
				Stack.columnWithItems(lineComponents, betweenLinesMargin, insets.vertical / 2, stackLayout)
			}
		}
		stack.aligned(alignment)
	}
	
	private def split(text: String) =
	{
		val threshold = _lineSplitThreshold
		
		var lineSplitIndices = Vector[Int]()
		var currentLineStartIndex = 0
		var lastCursorIndex = 0
		
		// Finds line split indices (NB: Splits are positioned in front of white space characters)
		text.indexOfIterator(" ").foreach { cursorIndex =>
			// Checks whether threshold was exeeded
			// (Cannot split twice at the same point, however)
			if (lastCursorIndex != currentLineStartIndex &&
				textWidth(text.substring(currentLineStartIndex, cursorIndex)).exists { _ > threshold })
			{
				lineSplitIndices :+= lastCursorIndex
				currentLineStartIndex = lastCursorIndex
			}
			lastCursorIndex = cursorIndex
		}
		
		// Splits the string
		if (lineSplitIndices.isEmpty)
			Vector(text)
		else if (lineSplitIndices.size == 1)
		{
			val (first, second) = text.splitAt(lineSplitIndices.head)
			Vector(first, second.trim)
		}
		else
			(-1 +: lineSplitIndices :+ text.length).paired.map { case (start, end) => text.substring(start + 1, end) }
	}
	
	
	// NESTED   -----------------------------
	
	private class LowPriorityLengthConstraint extends StackSizeModifier
	{
		override def apply(size: StackSize) =
		{
			// Sets low priority length to one side of the size, based on alignment
			val targetSide = if (alignment.isHorizontal) X else Y
			size.mapSide(targetSide) { _.withLowPriority }
		}
	}
}

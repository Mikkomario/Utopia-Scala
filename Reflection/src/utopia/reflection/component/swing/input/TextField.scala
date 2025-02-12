package utopia.reflection.component.swing.input

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.input.InputWithPointer
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.TextDrawerLike
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.Border
import utopia.firmament.model.stack.{StackInsets, StackLength, StackSize}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.NotEmpty
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.MeasuredText
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorSet}
import utopia.paradigm.enumeration
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.template.{CustomDrawComponent, JWrapper}
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.layout.Alignable
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackLeaf}
import utopia.reflection.text.Prompt

import java.awt.Graphics
import java.awt.event.{ActionEvent, ActionListener, FocusEvent, FocusListener}
import javax.swing.JTextField
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.{Document, PlainDocument}

object TextField
{
	/**
	  * Creates a new text field that is used for writing unfiltered strings
	  * @param targetWidth Target width of the field
	  * @param insideMargins Margins placed on each side of text
	  * @param font Font used within this field
	  * @param initialText The initially displayed text (default = empty)
	  * @param prompt A prompt displayed when this field is empty (default = None)
	  * @param textColor The text color used (default = 88% opacity black)
	  * @return A new text field that returns values as is
	  */
	def forStrings(targetWidth: StackLength, insideMargins: StackSize, font: Font, initialText: String = "",
				   prompt: Option[Prompt] = None, textColor: Color = Color.textBlack,
				   alignment: Alignment = Alignment.Left) =
		new TextField(targetWidth, insideMargins, font, initialText = initialText, prompt = prompt,
			textColor = textColor, initialAlignment = alignment)({ _.getOrElse("") })
	
	/**
	  * Creates a new text field that is used for writing positive integers
	  * @param targetWidth Target width of the field
	  * @param insideMargins Margins placed on each side of text
	  * @param font Font used within this field
	  * @param initialValue The initial value displayed (default = None)
	  * @param prompt A prompt displayed when this field is empty (default = None)
	  * @param textColor The text color used (default = 88% opacity black)
	  * @return A new text field that formats values to positive integers
	  */
	def forPositiveInts(targetWidth: StackLength, insideMargins: StackSize, font: Font, initialValue: Option[Int] = None,
						prompt: Option[Prompt] = None, textColor: Color = Color.textBlack,
						alignment: Alignment = Alignment.Left) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.digit, 10),
			initialValue.map { _.toString } getOrElse "", prompt, textColor,  alignment, Some(Regex.positiveInteger))(
			{ _.int })
	}
	
	/**
	  * Creates a new text field that is used for writing integers
	  * @param targetWidth Target width of the field
	  * @param insideMargins Margins placed on each side of text
	  * @param font Font used within this field
	  * @param initialValue The initial value displayed (default = None)
	  * @param prompt A prompt displayed when this field is empty (default = None)
	  * @param textColor The text color used (default = 88% opacity black)
	  * @return A new text field that formats values to integers
	  */
	def forInts(targetWidth: StackLength, insideMargins: StackSize, font: Font, initialValue: Option[Int] = None,
				prompt: Option[Prompt] = None, textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.integerPart, 11),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, alignment, Some(Regex.integer))(
			{ _.int })
	}
	
	/**
	  * Creates a new text field that is used for writing positive doubles
	  * @param targetWidth Target width of the field
	  * @param insideMargins Margins placed on each side of text
	  * @param font Font used within this field
	  * @param initialValue The initial value displayed (default = None)
	  * @param prompt A prompt displayed when this field is empty (default = None)
	  * @param textColor The text color used (default = 88% opacity black)
	  * @return A new text field that formats values to positive doubles
	  */
	def forPositiveDoubles(targetWidth: StackLength, insideMargins: StackSize, font: Font,
						   initialValue: Option[Double] = None,
						   prompt: Option[Prompt] = None, textColor: Color = Color.textBlack,
						   alignment: Alignment = Alignment.Left) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.positiveNumberPart, 24),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, alignment, Some(Regex.positiveNumber))(
			{ _.double })
	}
	
	// TODO: A lot of WET WET here
	
	/**
	  * Creates a new text field that is used for writing doubles
	  * @param targetWidth Target width of the field
	  * @param insideMargins Margins placed on each side of text
	  * @param font Font used within this field
	  * @param initialValue The initial value displayed (default = None)
	  * @param prompt A prompt displayed when this field is empty (default = None)
	  * @param textColor The text color used (default = 88% opacity black)
	  * @return A new text field that formats values to doubles
	  */
	def forDoubles(targetWidth: StackLength, insideMargins: StackSize, font: Font, initialValue: Option[Double] = None,
				   prompt: Option[Prompt] = None, textColor: Color = Color.textBlack,
				   alignment: enumeration.Alignment = enumeration.Alignment.Left) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.numberPart, 24),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, alignment, Some(Regex.number))(
			{ _.double })
	}
	
	/**
	  * Creates a new text field using contextual information
	  * @param targetWidth The stack width for this field
	  * @param initialText Initially displayed text (default = no text)
	  * @param prompt Prompt text displayed (default = empty = no prompt)
	  * @param document Document used for this field (default = plain document)
	  * @param resultFilter A regex used for transforming field content (default = None)
	  * @param mapResult A function for mapping result (non-empty string) to desired type
	  * @param context Component creation context
	  * @return A new text field
	  */
	def contextual[A](targetWidth: StackLength, initialText: String = "",
					  prompt: LocalizedString = LocalizedString.empty, document: Document = new PlainDocument(),
					  resultFilter: Option[Regex] = None)
					 (mapResult: Option[String] => A)
					 (implicit context: StaticTextContext) =
	{
		val field = new TextField(targetWidth, context.textInsets.total / 2, context.font, document,
			initialText, prompt.notEmpty.map { Prompt(_, context.promptFont, context.hintTextColor) }, context.textColor,
			context.textAlignment, resultFilter)(mapResult)
		field.background = context.background
		field.setSelectionHighlight(if (context.colors.secondary.contains(context.background))
			context.colors.primary else context.colors.secondary)
		field.addFocusHighlight(context.background.highlighted)
		field
	}
	
	/**
	  * Creates a new text field using contextual information
	  * @param targetWidth The stack width for this field
	  * @param initialText Initially displayed text (default = no text)
	  * @param prompt Prompt text displayed (default = empty = no prompt)
	  * @param document Document used for this field (default = plain document)
	  * @param resultFilter A regex used for transforming field content (default = None)
	  * @param context Component creation context
	  * @return A new text field
	  */
	def contextualForStrings(targetWidth: StackLength, initialText: String = "",
							 prompt: LocalizedString = LocalizedString.empty, document: Document = new PlainDocument(),
							 resultFilter: Option[Regex] = None)
							(implicit context: StaticTextContext) =
		contextual(targetWidth, initialText, prompt, document, resultFilter) { _.getOrElse("") }
	
	/**
	  * Creates a field that is used for writing positive integers. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (default = empty = no prompt)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForPositiveInts(targetWidth: StackLength, initialValue: Option[Int] = None,
	                              prompt: LocalizedString = LocalizedString.empty)
								 (implicit context: StaticTextContext) =
		contextual(targetWidth, initialValue.map { _.toString } getOrElse "", prompt,
			FilterDocument(Regex.digit, 10), Some(Regex.positiveInteger)) { _.int }
	
	/**
	  * Creates a field that is used for writing positive or negative integers. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (default = empty = no prompt)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForInts(targetWidth: StackLength, initialValue: Option[Int] = None,
	                      prompt: LocalizedString = LocalizedString.empty)
						 (implicit context: StaticTextContext) =
		contextual(targetWidth, initialValue.map { _.toString } getOrElse "", prompt,
			FilterDocument(Regex.integerPart, 11), Some(Regex.integer)) { _.int }
	
	/**
	  * Creates a field that is used for writing positive doubles. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (default = empty = no prompt)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForPositiveDoubles(targetWidth: StackLength, initialValue: Option[Double] = None,
									 prompt: LocalizedString = LocalizedString.empty)
									(implicit context: StaticTextContext) =
		contextual(targetWidth, initialValue.map { _.toString } getOrElse "", prompt,
			FilterDocument(Regex.positiveNumberPart, 24), Some(Regex.positiveNumber)) { _.double }
	
	/**
	  * Creates a field that is used for writing positive or negative doubles. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (Default = None)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForDoubles(targetWidth: StackLength, initialValue: Option[Double] = None,
	                         prompt: LocalizedString = LocalizedString.empty)
							(implicit context: StaticTextContext) =
		contextual(targetWidth, initialValue.map { _.toString } getOrElse "", prompt,
			FilterDocument(Regex.numberPart, 24), Some(Regex.number)) { _.double }
}

/**
  * Text fields are used for collecting text input from user
  * @author Mikko Hilpinen
  * @since 1.5.2019, v1+
  * @param initialTargetWidth The target width of this field
  * @param insideMargins The target margins around text in this field
  * @param font The font used in this field
  * @param document The document used in this field (default = plain document)
  * @param initialText The initially displayed text (default = "")
  * @param prompt The prompt for this field (default = None)
  * @param textColor The text color in this field (default = 88% opacity black)
  * @param initialAlignment Alignment used for the text. Default = Left.
  * @param resultFilter Filter applied when querying results from this field (determines formatting on final text
  *                     result). Default = None
  * @param resultsParser A function that converts a non-empty string to a value
  */
// TODO: Switch from margins to insets (also support proper positioning & border)
class TextField[A](initialTargetWidth: StackLength, insideMargins: StackSize, font: Font,
				   val document: Document = new PlainDocument(), initialText: String = "",
				   prompt: Option[Prompt] = None, textColor: Color = Color.textBlack,
				   initialAlignment: Alignment = Alignment.Left,
				   resultFilter: Option[Regex] = None)(resultsParser: Option[String] => A)
	extends JWrapper with CachingReflectionStackable with InputWithPointer[A, Changing[A]] with Alignable with Focusable
		with MutableCustomDrawableWrapper with ReflectionStackLeaf
{
	// ATTRIBUTES	----------------------
	
	private val _textPointer = EventfulPointer(initialText)
	override val valuePointer = _textPointer.map { text =>
		// Text is trimmed before mapping. Empty strings are treated as None
		val base = NotEmpty(text.trim)
		// Applies possible regex filtering and then converts the text into a value
		resultsParser(resultFilter match {
			case Some(regex) => base.flatMap(regex.findFirstFrom)
			case None => base
		})
	}
	
	private val field = AwtEventThread.blocking { new CustomTextField() }
	private val defaultBorder = Border(1, textColor.timesAlpha(0.625))
	
	private var isDisplayingPrompt = initialText.isEmpty && prompt.isDefined
	private var enterListeners = Vector[A => Unit]()
	private var resultListeners = Vector[A => Unit]()
	
	private var _targetWidth = initialTargetWidth
	
	
	// INITIAL CODE	----------------------
	
	field.setFont(font.toAwt)
	field.setForeground(textColor.toAwt)
	field.setCaretColor(textColor.toAwt)
	field.setDocument(document)
	
	setBorder(defaultBorder)
	field.setText(initialText)
	
	document.addDocumentListener(InputListener)
	field.addActionListener(EnterListener)
	field.addFocusListener(FocusResultHandler)
	
	{
		// TODO: Handle alignment better (take into account bottom & top alignments)
		val alignment = initialAlignment.onlyHorizontal
		if (alignment == Alignment.Left)
			alignLeft(insideMargins.width.optimal)
		else
			align(alignment)
	}
	
	if (prompt.isDefined)
		addCustomDrawer(new PromptDrawer)
	
	// Needs to convert mouse button events to their Reflection counterparts
	enableAwtMouseButtonEventConversion()
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return An immutable pointer to this field's current text
	  */
	def textPointer = _textPointer.readOnly
	
	/**
	  * @return The width (stack length) of this field
	  */
	def targetWidth = _targetWidth
	def targetWidth_=(newWidth: StackLength) =
	{
		if (_targetWidth != newWidth)
		{
			_targetWidth = newWidth
			revalidate()
		}
	}
	
	/**
	  * @return Current text in this field
	  */
	def text = _textPointer.value
	def text_=(newText: String): Unit = AwtEventThread.async { field.setText(newText) }
	
	/**
	  * @return Color currently used in this field's caret (cursor)
	  */
	def caretColor: Color = field.getCaretColor
	def caretColor_=(newColor: Color) = field.setCaretColor(newColor.toAwt)
	
	
	// IMPLEMENTED	--------------------
	
	override def drawable: CustomDrawComponent = field
	
	override protected def updateVisibility(visible: Boolean) = super[JWrapper].visible_=(visible)
	
	override def updateLayout() = ()
	
	override def component: JTextField = field
	
	override def calculatedStackSize = {
		val h = insideMargins.height * 2 + textHeightWith(font)
		StackSize(targetWidth, h)
	}
	
	override def align(alignment: Alignment) = alignment.swingComponents.get(X).foreach(field.setHorizontalAlignment)
	
	override def requestFocusInWindow() = field.requestFocusInWindow()
	
	
	// OTHER	------------------------------
	
	/**
	  * Clears all text from this field
	  */
	def clear() = text = ""
	
	/**
	  * Aligns this field to the left and adds margin
	  * @param margin The amount of margin
	  */
	def alignLeft(margin: Double): Unit = {
		alignLeft()
		if (margin > 0)
			setBorder(defaultBorder + Border(Insets.left(margin), None))
	}
	
	/**
	  * Adds a listener that will be informed when user presses enter inside this text field. Informs listener
	  * of the processed value of this text field
	  * @param listener A listener
	  */
	def addEnterListener(listener: A => Unit) = enterListeners :+= listener
	/**
	  * Adds a listener that will be informed when this field loses focus or the user presses enter inside this field.
	  * Informs the listener of the processed value of this field.
	  * @param listener A listener
	  */
	def addResultListener(listener: A => Unit) = resultListeners :+= listener
	
	/**
	  * Adds focus highlighting to this text field. The highlighting will change text field background color when
	  * it gains focus
	  * @param color The background color used when focused
	  */
	def addFocusHighlight(color: Color) = component.addFocusListener(new FocusHighlighter(background, color))
	
	/**
	  * Updates the selection color, selected text color and caret color for this field. Please note that this method
	  * is dependent on current field background color so it should be set first
	  * @param color Color set to use in this field's selection colors
	  */
	def setSelectionHighlight(color: ColorSet) =
	{
		val bg = background
		val preferredSelectionShade = if (bg.luminosity >= 0.5) Light else Dark
		val selectionColor = color.against(bg, preferredSelectionShade)
		val caretColor = color.againstMany(Vector(bg, selectionColor))
		
		field.setSelectionColor(selectionColor.toAwt)
		field.setSelectedTextColor(selectionColor.shade.defaultTextColor.toAwt)
		field.setCaretColor(caretColor.toAwt)
	}
	
	// NB: Must be called in the Awt event thread
	private def filterInAwtThread() =
	{
		val original = text
		val trimmed = original.trim
		val filtered =
		{
			if (trimmed.isEmpty)
				trimmed
			else
				resultFilter match
				{
					case Some(regex) => regex.findFirstFrom(trimmed).getOrElse("")
					case None => trimmed
				}
		}
		if (filtered != original)
			field.setText(filtered)
	}
	
	
	// NESTED CLASSES	----------------------
	
	private object EnterListener extends ActionListener
	{
		// When enter is pressed, filters field value and informs listeners
		override def actionPerformed(e: ActionEvent) =
		{
			filterInAwtThread()
			if (enterListeners.nonEmpty || resultListeners.nonEmpty)
			{
				val result = value
				enterListeners.foreach { _(result) }
				resultListeners.foreach { _(result) }
			}
		}
	}
	
	object FocusResultHandler extends FocusListener
	{
		override def focusGained(e: FocusEvent) = ()
		
		override def focusLost(e: FocusEvent) =
		{
			filterInAwtThread()
			if (resultListeners.nonEmpty)
			{
				val result = value
				resultListeners.foreach { _(result) }
			}
		}
	}
	
	private object InputListener extends DocumentListener
	{
		override def insertUpdate(e: DocumentEvent) = handleInputChange()
		
		override def removeUpdate(e: DocumentEvent) = handleInputChange()
		
		override def changedUpdate(e: DocumentEvent) = handleInputChange()
		
		private def handleInputChange() =
		{
			// Updates pointer value, replaces null with an empty string
			_textPointer.value = Option(field.getText).getOrElse("")
			
			// Updates prompt display status
			val newPromptStatus = text.isEmpty && prompt.isDefined
			if (isDisplayingPrompt != newPromptStatus)
			{
				isDisplayingPrompt = newPromptStatus
				repaint()
			}
		}
	}
	
	private class PromptDrawer extends TextDrawerLike
	{
		// ATTRIBUTES	------------------------
		
		private val measuredPromptData = prompt.map { p =>
			MeasuredText(p.text.string, component.getFontMetrics(p.font.toAwt)) -> p.font
		}
		private val measuredPrompt = measuredPromptData.map { _._1 }
		override val font = measuredPromptData match {
			case Some((_, font)) => font
			case None => TextField.this.font
		}
		private val emptyText = MeasuredText("", component.getFontMetrics(font.toAwt))
		
		override val insets = StackInsets.symmetric(insideMargins * 2)
		override val color: Color = textColor.timesAlpha(0.66)
		
		
		// IMPLEMENTED	-----------------------
		
		override def text: MeasuredText = {
			if (isDisplayingPrompt)
				measuredPrompt.getOrElse(emptyText)
			else
				emptyText
		}
		
		override def drawLevel = Normal
		
		override def alignment: Alignment = initialAlignment
	}
	
	private class FocusHighlighter(val defaultBackground: Color, val highlightBackground: Color) extends FocusListener
	{
		override def focusGained(e: FocusEvent) = background = highlightBackground
		
		override def focusLost(e: FocusEvent) = background = defaultBackground
	}
}

private class CustomTextField extends JTextField with CustomDrawComponent
{
	// IMPLEMENTED	-----------------
	
	override def drawBounds = Bounds(Point.origin, Size(getSize()))
	
	override def paintComponent(g: Graphics) = customPaintComponent(g, super.paintComponent)
	
	override def paintChildren(g: Graphics) = customPaintChildren(g, super.paintChildren)
	
	override def isPaintingOrigin = shouldPaintOrigin()
}
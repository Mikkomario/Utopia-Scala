package utopia.reflection.component.swing.input

import java.awt.Graphics
import java.awt.event.{ActionEvent, ActionListener, FocusEvent, FocusListener}

import javax.swing.JTextField
import javax.swing.event.{DocumentEvent, DocumentListener}
import javax.swing.text.{Document, PlainDocument}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.flow.generic.ValueConversions._
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.{Bounds, Insets, Point, Size}
import utopia.reflection.color.ColorSet
import utopia.reflection.color.ColorShade.{Dark, Light}
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.template.TextDrawer
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.component.template.layout.stack.{CachingStackable, StackLeaf}
import utopia.reflection.component.swing.template.{CustomDrawComponent, JWrapper}
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.layout.Alignable
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.{stack, _}
import utopia.reflection.shape.stack.{StackInsets, StackLength, StackSize}
import utopia.reflection.text.{Font, Prompt, Regex}

object TextField
{
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
						alignment: Alignment = Alignment.Left,
						valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None)) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.digit, 10),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, Some(Regex.numericPositive), alignment,
			valuePointer)
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
				prompt: Option[Prompt] = None, textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
				valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None)) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.numericParts, 11),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, Some(Regex.numeric), alignment, valuePointer)
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
	def forPositiveDoubles(targetWidth: StackLength, insideMargins: StackSize, font: Font, initialValue: Option[Double] = None,
						   prompt: Option[Prompt] = None, textColor: Color = Color.textBlack,
						   alignment: Alignment = Alignment.Left,
						   valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None)) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.decimalPositiveParts, 24),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, Some(Regex.decimalPositive), alignment, valuePointer)
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
				   alignment: Alignment = Alignment.Left,
				   valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None)) =
	{
		new TextField(targetWidth, insideMargins, font, FilterDocument(Regex.decimalParts, 24),
			initialValue.map { _.toString } getOrElse "", prompt, textColor, Some(Regex.decimal), alignment, valuePointer)
	}
	
	/**
	  * Creates a new text field using contextual information
	  * @param targetWidth The stack width for this field
	  * @param document Document used for this field (default = plain document)
	  * @param initialText Initially displayed text (default = no text)
	  * @param prompt Prompt text displayed (optional)
	  * @param resultFilter A regex used for transforming field content (default = None)
	  * @param context Component creation context
	  * @return A new text field
	  */
	def contextual(targetWidth: StackLength, document: Document = new PlainDocument(), initialText: String = "",
				   prompt: Option[LocalizedString] = None, resultFilter: Option[Regex] = None,
				   valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None))
				  (implicit context: ButtonContextLike) =
	{
		val field = new TextField(targetWidth, context.textInsets.total / 2, context.font, document,
			initialText, prompt.map { Prompt(_, context.promptFont, context.hintTextColor) }, context.textColor,
			resultFilter, context.textAlignment, valuePointer)
		field.background = context.buttonColor
		field.setSelectionHighlight(if (context.colorScheme.secondary.contains(context.buttonColor))
			context.colorScheme.primary else context.colorScheme.secondary)
		field.addFocusHighlight(context.buttonColorHighlighted)
		field
	}
	
	/**
	  * Creates a field that is used for writing positive integers. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (Default = None)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForPositiveInts(targetWidth: StackLength, initialValue: Option[Int] = None, prompt: Option[LocalizedString] = None,
								  valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None))
								 (implicit context: ButtonContextLike) = contextual(targetWidth,
		FilterDocument(Regex.digit, 10), initialValue.map { _.toString } getOrElse "", prompt, Some(Regex.numericPositive),
		valuePointer)
	
	/**
	  * Creates a field that is used for writing positive or negative integers. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (Default = None)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForInts(targetWidth: StackLength, initialValue: Option[Int] = None, prompt: Option[LocalizedString] = None,
						  valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None))
						 (implicit context: ButtonContextLike) = contextual(targetWidth,
		FilterDocument(Regex.numericParts, 11), initialValue.map { _.toString } getOrElse "", prompt, Some(Regex.numeric),
		valuePointer)
	
	/**
	  * Creates a field that is used for writing positive doubles. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (Default = None)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForPositiveDoubles(targetWidth: StackLength, initialValue: Option[Double] = None,
									 prompt: Option[LocalizedString] = None,
									 valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None))
									(implicit context: ButtonContextLike) = contextual(targetWidth,
		FilterDocument(Regex.decimalPositiveParts, 24), initialValue.map { _.toString } getOrElse "", prompt,
		Some(Regex.decimalPositive), valuePointer)
	
	/**
	  * Creates a field that is used for writing positive or negative doubles. Uses component creation context.
	  * @param targetWidth The stack width for this field
	  * @param initialValue Initially displayed value (Default = None)
	  * @param prompt Prompt text displayed, if any (Default = None)
	  * @param context Component creation context (implicit)
	  * @return A new text field
	  */
	def contextualForDoubles(targetWidth: StackLength, initialValue: Option[Double] = None, prompt: Option[LocalizedString] = None,
							 valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None))
							(implicit context: ButtonContextLike) = contextual(targetWidth,
		FilterDocument(Regex.decimalParts, 24), initialValue.map { _.toString } getOrElse "", prompt,
		Some(Regex.decimal), valuePointer)
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
  * @param resultFilter Filter applied when querying results from this field (determines formatting on final text
  *                     result). Default = None
  * @param initialAlignment Alignment used for the text. Default = Left.
  * @param valuePointer Pointer for holding the current value in this field (default = new pointer)
  */
// TODO: Switch from margins to insets (also support proper positioning & border)
class TextField(initialTargetWidth: StackLength, val insideMargins: StackSize, font: Font,
				val document: Document = new PlainDocument(), initialText: String = "",
				val prompt: Option[Prompt] = None, val textColor: Color = Color.textBlack,
				resultFilter: Option[Regex] = None, initialAlignment: Alignment = Alignment.Left,
				override val valuePointer: PointerWithEvents[Option[String]] = new PointerWithEvents(None))
	extends JWrapper with CachingStackable with InteractionWithPointer[Option[String]] with Alignable with Focusable
		with CustomDrawableWrapper with StackLeaf
{
	// ATTRIBUTES	----------------------
	
	private val field = new CustomTextField()
	private val defaultBorder = Border.square(1, textColor.timesAlpha(0.625))
	
	private var isDisplayingPrompt = initialText.isEmpty && prompt.isDefined
	private var isUpdatingText = false
	private var enterListeners = Vector[Option[String] => Unit]()
	private var resultListeners = Vector[Option[String] => Unit]()
	
	private var _targetWidth = initialTargetWidth
	
	
	// INITIAL CODE	----------------------
	
	field.setFont(font.toAwt)
	field.setForeground(textColor.toAwt)
	field.setCaretColor(textColor.toAwt)
	field.setDocument(document)
	
	setBorder(defaultBorder)
	text = initialText
	
	document.addDocumentListener(InputListener)
	field.addActionListener(EnterListener)
	field.addFocusListener(FocusResultHandler)
	valuePointer.addListener(ValueChangeListener)
	
	{
		// TODO: Handle alignment better (take into account bottom & top alignments)
		val alignment = initialAlignment.horizontal
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
	def text =
	{
		val t = field.getText
		if (t == null) "" else t
	}
	def text_=(newText: String): Unit =
	{
		if (!isUpdatingText)
		{
			// Updates text field value
			val raw = newText.trim
			value = resultFilter.map { _.findFirstFrom(raw) }.getOrElse { if (raw.isEmpty) None else Some(raw) }
			
			isUpdatingText = true
			field.setText(newText)
			isUpdatingText = false
		}
	}
	def text_=(newText: Option[String]): Unit = text_=(newText getOrElse "")
	
	/**
	  * @return Current integer value in this field. None if the text couldn't be read as an integer
	  */
	def intValue = value.int
	
	/**
	  * @return Current double value in this field. None if the text couldn't be read as a double
	  */
	def doubleValue = value.double
	
	/**
	  * @return Color currently used in this field's caret (cursor)
	  */
	def caretColor: Color = field.getCaretColor
	def caretColor_=(newColor: Color) = field.setCaretColor(newColor.toAwt)
	
	
	// IMPLEMENTED	--------------------
	
	override def drawable: CustomDrawComponent = field
	
	override protected def updateVisibility(visible: Boolean) = super[JWrapper].visible_=(visible)
	
	override def updateLayout() = ()
	
	// Empty strings and strings with only whitespaces are treated as None
	override def value =
	{
		val raw = text.trim
		resultFilter.map { _.findFirstFrom(raw) }.getOrElse { if (raw.isEmpty) None else Some(raw) }
	}
	
	override def component: JTextField = field
	
	override def calculatedStackSize =
	{
		val h = textHeight.map { insideMargins.height * 2 + _ } getOrElse 32.any
		stack.StackSize(targetWidth, h)
	}
	
	override def align(alignment: Alignment) = alignment.horizontal.swingComponents.get(X).foreach(field.setHorizontalAlignment)
	
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
	def alignLeft(margin: Double): Unit =
	{
		alignLeft()
		if (margin > 0)
			setBorder(defaultBorder + Border(Insets.left(margin), None))
	}
	
	/**
	  * Adds a listener that will be informed when user presses enter inside this text field. Informs listener
	  * of the processed value of this text field
	  * @param listener A listener
	  */
	def addEnterListener(listener: Option[String] => Unit) = enterListeners :+= listener
	/**
	  * Adds a listener that will be informed when this field loses focus or the user presses enter inside this field.
	  * Informs the listener of the processed value of this field.
	  * @param listener A listener
	  */
	def addResultListener(listener: Option[String] => Unit) = resultListeners :+= listener
	
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
		val selectionColor = color.forBackgroundPreferring(bg, preferredSelectionShade)
		val caretColor = color.bestAgainst(Vector(bg, selectionColor))
		
		field.setSelectionColor(selectionColor.toAwt)
		field.setSelectedTextColor(selectionColor.textColorStandard.defaultTextColor.toAwt)
		field.setCaretColor(caretColor.toAwt)
	}
	
	private def filter() =
	{
		val original = text
		val filtered = resultFilter.map { _.findFirstFrom(original.trim) } getOrElse Some(original.trim)
		val changed = filtered.filter { _ != original }
		
		changed.foreach(text_=)
	}
	
	
	// NESTED CLASSES	----------------------
	
	private object EnterListener extends ActionListener
	{
		// When enter is pressed, filters field value and informs listeners
		override def actionPerformed(e: ActionEvent) =
		{
			filter()
			if (enterListeners.nonEmpty || resultListeners.nonEmpty)
			{
				val result = value
				enterListeners.foreach { _(result) }
				resultListeners.foreach { _(result) }
			}
		}
	}
	
	private object ValueChangeListener extends ChangeListener[Option[String]]
	{
		override def onChangeEvent(event: ChangeEvent[Option[String]]) =
		{
			if (!isUpdatingText)
				text = event.newValue getOrElse ""
		}
	}
	
	private object InputListener extends DocumentListener
	{
		override def insertUpdate(e: DocumentEvent) = handleInputChange()
		
		override def removeUpdate(e: DocumentEvent) = handleInputChange()
		
		override def changedUpdate(e: DocumentEvent) = handleInputChange()
		
		private def handleInputChange() =
		{
			if (!isUpdatingText)
			{
				isUpdatingText = true
				val raw = text.trim
				value = resultFilter.map { _.findFirstFrom(raw) }.getOrElse { if (raw.isEmpty) None else Some(raw) }
				isUpdatingText = false
			}
			
			// Updates prompt display status
			val newPromptStatus = text.isEmpty && prompt.isDefined
			if (isDisplayingPrompt != newPromptStatus)
			{
				isDisplayingPrompt = newPromptStatus
				repaint()
			}
		}
	}
	
	private class PromptDrawer extends TextDrawer
	{
		private val _insets = StackInsets.symmetric(insideMargins * 2)
		
		override def drawContext =
		{
			prompt match
			{
				case Some(p) => TextDrawContext(p.font, textColor.timesAlpha(0.66), insets = _insets)
				case None => TextDrawContext(font, textColor)
			}
		}
		
		override def text =
		{
			if (isDisplayingPrompt)
				prompt.map { _.text }.getOrElse(LocalizedString.empty)
			else
				LocalizedString.empty
		}
		
		override val drawLevel = Normal
		
		/*
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			if (isDisplayingPrompt)
			{
				prompt.foreach { p =>
					drawer.clippedTo(bounds).withEdgeColor(textColor.timesAlpha(0.66)).drawText(p.text.string,
						p.font.toAwt, bounds.topLeft + insideMargins.optimal)
				}
			}
		}*/
	}
	
	object FocusResultHandler extends FocusListener
	{
		override def focusGained(e: FocusEvent) = ()
		
		override def focusLost(e: FocusEvent) =
		{
			filter()
			if (resultListeners.nonEmpty)
			{
				val result = value
				resultListeners.foreach { _(result) }
			}
		}
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
	
	override def drawBounds = Bounds(Point.origin, Size.of(getSize()))
	
	override def paintComponent(g: Graphics) = customPaintComponent(g, super.paintComponent)
	
	override def paintChildren(g: Graphics) = customPaintChildren(g, super.paintChildren)
	
	override def isPaintingOrigin = shouldPaintOrigin()
}
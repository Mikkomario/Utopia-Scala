package utopia.reflection.component.reach.input

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, DataFlavor, StringSelection, Transferable}
import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeListener, Changing}
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, KeyStatus, KeyTypedEvent, MouseButtonStateEvent, MouseEvent, MouseMoveEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{Actor, KeyStateListener, KeyTypedHandlerType, KeyTypedListener, MouseButtonStateListener, MouseMoveHandlerType, MouseMoveListener}
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.{Direction2D, Point}
import utopia.genesis.view.{GlobalKeyboardEventHandler, GlobalMouseEventHandler}
import utopia.inception.handling.HandlerType
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.view.SelectableTextViewDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{CursorDefining, MutableCustomDrawReachComponent, MutableFocusable}
import utopia.reflection.component.template.text.MutableTextComponent
import utopia.reflection.cursor.Cursor
import utopia.reflection.cursor.CursorType.{Default, Text}
import utopia.reflection.event.{FocusChangeEvent, FocusChangeListener, FocusListener}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.text.{FontMetricsContext, MeasuredText, Regex}
import utopia.reflection.localization.LocalString._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}

object EditableTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike, EditableTextLabelFactory,
	ContextualEditableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new EditableTextLabelFactory(hierarchy)
}

class EditableTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualEditableTextLabelFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualEditableTextLabelFactory(this, context)
	
	/**
	  * Creates a new editable text label
	  * @param actorHandler Actor handler that will deliver required action events
	  * @param stylePointer A pointer to this label's styling
	  * @param selectedTextColorPointer A pointer to this label's selected text's color (default = always standard black)
	  * @param selectionBackgroundColorPointer A pointer to this label's selected text's background color, if any
	  *                                        (default = always None)
	  * @param caretColor Color used when drawing the caret (default = standard black)
	  * @param caretWidth Width of the drawn caret (default = 1 pixels)
	  * @param caretBlinkFrequency Frequency how often caret visibility is changed (default = every 0.5 seconds)
	  * @param textPointer A pointer to this label's text (default = new empty pointer)
	  * @param inputFilter A filter regex applied to typed and pasted input, if any (default = None)
	  * @param maxLength Maximum text length, if any (default = None)
	  * @param enabledPointer A pointer to this label's enabled state (default = always enabled)
	  * @param allowSelectionWhileDisabled Whether this label's text should be selectable even while it is disabled
	  *                                    (default = true)
	  * @param allowLineBreaks Whether line breaks should be allowed inside this label (default = true)
	  * @param allowTextShrink Whether the drawn text should be shrank to conserve space when required (default = false)
	  * @return A new label
	  */
	def apply(actorHandler: ActorHandler, stylePointer: PointerWithEvents[TextDrawContext],
			  selectedTextColorPointer: Changing[Color] = Changing.wrap(Color.textBlack),
			  selectionBackgroundColorPointer: Changing[Option[Color]] = Changing.wrap(None),
			  caretColor: Color = Color.textBlack, caretWidth: Double = 1.0,
			  caretBlinkFrequency: Duration = 0.5.seconds,
			  textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
			  inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
			  enabledPointer: Changing[Boolean] = Changing.wrap(true),
			  allowSelectionWhileDisabled: Boolean = true, allowLineBreaks: Boolean = true,
			  allowTextShrink: Boolean = false) =
		new EditableTextLabel(parentHierarchy, actorHandler, stylePointer, selectedTextColorPointer,
			selectionBackgroundColorPointer, caretColor, caretWidth, caretBlinkFrequency, textPointer,
			inputFilter, maxLength, enabledPointer, allowSelectionWhileDisabled, allowLineBreaks, allowTextShrink)
}

case class ContextualEditableTextLabelFactory[+N <: TextContextLike](factory: EditableTextLabelFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualEditableTextLabelFactory]
{
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new editable label
	  * @param textPointer A pointer to this label's text (default = new empty pointer)
	  * @param inputFilter A filter regex applied to typed and pasted input, if any (default = None)
	  * @param maxLength Maximum text length, if any (default = None)
	  * @param enabledPointer A pointer to this label's enabled state (default = always enabled)
	  * @param caretBlinkFrequency Frequency how often caret visibility is changed (default = every 0.5 seconds)
	  * @param allowSelectionWhileDisabled Whether this label's text should be selectable even while it is disabled
	  *                                    (default = true)
	  * @return a new label
	  */
	def apply(textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
			  inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
			  enabledPointer: Changing[Boolean] = Changing.wrap(true), caretBlinkFrequency: Duration = 0.5.seconds,
			  allowSelectionWhileDisabled: Boolean = true) =
	{
		val selectionBackground = context.color(Secondary, Light)
		val caretColor = context.colorScheme.secondary.bestAgainst(
			Vector(context.containerBackground, selectionBackground))
		factory(context.actorHandler, new PointerWithEvents(TextDrawContext.contextual(context)),
			Changing.wrap(selectionBackground.defaultTextColor), Changing.wrap(Some(selectionBackground)),
			caretColor, (context.margins.verySmall / 2) max 1.0, caretBlinkFrequency, textPointer,
			inputFilter, maxLength, enabledPointer, allowSelectionWhileDisabled, context.allowLineBreaks,
			context.allowTextShrink)
	}
}

/**
  * Used for requesting user input in text format
  * @author Mikko Hilpinen
  * @since 30.10.2020, v2
  */
class EditableTextLabel(override val parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler,
						val baseStylePointer: PointerWithEvents[TextDrawContext],
						selectedTextColorPointer: Changing[Color] = Changing.wrap(Color.textBlack),
						selectionBackgroundColorPointer: Changing[Option[Color]] = Changing.wrap(None),
						caretColor: Color = Color.textBlack, caretWidth: Double = 1.0,
						caretBlinkFrequency: Duration = 0.5.seconds,
						val textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
						inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
						enabledPointer: Changing[Boolean] = Changing.wrap(true),
						allowSelectionWhileDisabled: Boolean = true, allowLineBreaks: Boolean = true,
						override val allowTextShrink: Boolean = false)
	extends MutableCustomDrawReachComponent with MutableTextComponent with MutableFocusable with CursorDefining
{
	// ATTRIBUTES	-------------------------------
	
	private var focusLeaveConditions = Vector[String => (String, Boolean)]()
	private var draggingMouse = false
	
	override var focusListeners: Seq[FocusListener] = Vector(FocusHandler)
	
	/**
	  * A pointer to this label's actual text styling, which takes into account the enabled state
	  */
	val effectiveStylePointer = baseStylePointer.mergeWith(enabledPointer) { (style, enabled) =>
		if (enabled)
			style
		else
			style.mapColor { _.timesAlpha(0.66) }
	}
	/**
	  * A pointer to this label's measured text information
	  */
	val measuredTextPointer = textPointer.mergeWith(effectiveStylePointer) { (text, style) =>
		MeasuredText(text.noLanguageLocalizationSkipped, FontMetricsContext(fontMetrics(style.font),
			style.betweenLinesMargin), style.alignment, allowLineBreaks)
	}
	private val caretIndexPointer = new PointerWithEvents(measuredText.maxCaretIndex)
	private val caretVisibilityPointer = new PointerWithEvents(false)
	private val drawnCaretPointer = caretIndexPointer.mergeWith(caretVisibilityPointer) { (index, isVisible) =>
		if (isVisible && selectable) Some(index) else None }
	// Selected range is in caret indices
	private val selectedRangePointer = new PointerWithEvents[Option[(Int, Int)]](None)
	
	private val drawer = SelectableTextViewDrawer(measuredTextPointer, effectiveStylePointer,
		selectedRangePointer.map { _.map { case (start, end) => if (start < end) start to end else end to start } },
		drawnCaretPointer, selectedTextColorPointer, selectionBackgroundColorPointer, caretColor, caretWidth)
	private val repaintListener: ChangeListener[Any] = _ => repaint()
	private val showCaretListener: ChangeListener[Any] = _ => CaretBlinker.show()
	
	
	// INITIAL CODE	-------------------------------
	
	// Registers some listeners only while attached to top hierarchy
	addHierarchyListener { isAttached =>
		if (isAttached)
		{
			enableFocusHandling()
			GlobalKeyboardEventHandler.register(KeyListener)
			GlobalMouseEventHandler.register(GlobalMouseReleaseListener)
			actorHandler += CaretBlinker
			parentCanvas.cursorManager.foreach { _ += this }
		}
		else
		{
			disableFocusHandling()
			GlobalKeyboardEventHandler.unregister(KeyListener)
			GlobalMouseEventHandler.unregister(GlobalMouseReleaseListener)
			actorHandler -= CaretBlinker
			parentCanvas.cursorManager.foreach { _ -= this }
		}
	}
	addMouseButtonListener(MouseListener)
	addMouseMoveListener(MouseListener)
	addCustomDrawer(drawer)
	
	// Repaints (and possibly revalidates) this component when content or styling changes
	measuredTextPointer.addListener { event =>
		if (event.compareBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	effectiveStylePointer.addListener(repaintListener)
	drawnCaretPointer.addListener(repaintListener)
	selectedRangePointer.addListener(repaintListener)
	selectedTextColorPointer.addListener(repaintListener)
	selectionBackgroundColorPointer.addListener(repaintListener)
	
	// Whenever text changes or caret position is updated, shows the caret
	caretIndexPointer.addListener(showCaretListener)
	textPointer.addListener(showCaretListener)
	
	// Clears or limits selected range whenever the text is updated
	textPointer.addListener { event =>
		selectedRangePointer.value.foreach { case (start, end) =>
			val maxIndex = event.newValue.length
			if (start >= maxIndex)
				clearSelection()
			else if (end > maxIndex)
				selectedRangePointer.value = Some(start -> maxIndex)
		}
	}
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Whether this label is currently the focused component
	  */
	def hasFocus = FocusHandler.hasFocus
	
	/**
	  * @return Whether this label is currently enabled
	  */
	def enabled = enabledPointer.value
	
	/**
	  * @return Whether text in this label can currently be selected
	  */
	def selectable = allowSelectionWhileDisabled || enabled
	
	/**
	  * @return Current index of the text caret
	  */
	def caretIndex = caretIndexPointer.value
	private def caretIndex_=(newIndex: Int) =
	{
		if (measuredText.isValidCaretIndex(newIndex))
			caretIndexPointer.value = newIndex
	}
	
	/**
	  * @return Currently selected range of text (in caret indices). None if no range is selected
	  */
	def selectedRange = selectedRangePointer.value.map { case (start, end) =>
		if (start < end) start to end else end to start }
	
	/**
	  * @return Currently selected text inside this label. None if no text is selected.
	  */
	def selectedText = selectedRange
		.map { r => measuredText.caretIndexToCharacterIndex(r.start) until r.end }
		.map { _text.slice(_) }.filterNot { _.isEmpty }
	
	def text_=(newText: String) = textPointer.value = newText
	
	private def _text = textPointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
	override def text_=(newText: LocalizedString) = textPointer.value = newText.string
	
	override def updateLayout() = ()
	
	override def drawContext_=(newContext: TextDrawContext) = baseStylePointer.value = newContext
	
	override def measuredText = measuredTextPointer.value
	
	override def drawContext = baseStylePointer.value
	
	override def allowsFocusEnter = selectable
	
	override def allowsFocusLeave =
	{
		// Checks with focus leave conditions
		val (textAfter, allowed) = focusLeaveConditions.foldLeft((_text, true)) { (prev, condition) =>
			if (prev._2)
				condition(prev._1)
			else
				prev
		}
		textPointer.value = textAfter
		allowed
	}
	
	override def cursorType = if (selectable) Text else Default
	
	override def cursorBounds = boundsInsideTop
	
	override def cursorToImage(cursor: Cursor, position: Point) =
	{
		// If hovering over a selected area, bases cursor color on that
		// Otherwise proposes standard text color
		selectionBackgroundColorPointer.value match
		{
			case Some(selectedAreaBackground) =>
				val highlightAreas = drawer.drawTargets._2
				lazy val positionInTextBounds = ((this.position + position) - drawer.lastDrawPosition) * drawer.lastDrawScaling
				if (highlightAreas.nonEmpty && highlightAreas.exists { _._2.contains(positionInTextBounds) })
					cursor.over(selectedAreaBackground)
				else
					cursor.proposing(textColor)
			case None => cursor.proposing(textColor)
		}
	}
	
	
	// OTHER	----------------------------------
	
	/**
	  * Moves caret to the end of this label's current text
	  */
	def moveCaretToEnd() = caretIndexPointer.value = measuredText.maxCaretIndex
	
	/**
	  * Adds a new condition to be tested before allowing focus leave from this component
	  * @param condition A new condition to test (accepts current text, returns possibly modified text and a boolean
	  *                  indicating whether focus move is allowed)
	  */
	def addFocusLeaveCondition(condition: String => (String, Boolean)) = focusLeaveConditions :+= condition
	
	/**
	  * Clears all text in this label
	  */
	def clear() = text = ""
	
	/**
	  * Clears the current selection area
	  */
	def clearSelection() = selectedRangePointer.value = None
	
	private def insertToCaret(text: String) =
	{
		// If a range of characters is selected, overwrites those, then removes the selection
		selectedRange match
		{
			case Some(selection) =>
				val replaceText = maxLength match
				{
					case Some(maxLength) =>
						val availableSpace = maxLength - _text.length + selection.length
						limitedLengthString(availableSpace, text)
					case None => text
				}
				val replaceStartCharIndex = measuredText.caretIndexToCharacterIndex(selection.start)
				val replaceEndCharIndex = measuredText.caretIndexToCharacterIndex(selection.end)
				clearSelection()
				textPointer.update { old => old.take(replaceStartCharIndex) + replaceText + old.drop(replaceEndCharIndex) }
				caretIndex = selection.start + replaceText.length
			case None =>
				// May insert only part of the text, due to length limits
				val textToInsert = maxLength match
				{
					case Some(maxLength) => limitedLengthString(maxLength - _text.length, text)
					case None => text
				}
				if (textToInsert.nonEmpty)
				{
					val (before, after) = textPointer.value.splitAt(caretIndex)
					
					textPointer.value = before + text + after
					// Moves the caret to the end of the inserted string
					caretIndexPointer.update { _ + text.length }
				}
		}
	}
	
	private def limitedLengthString(maxLength: Int, original: String) =
	{
		if (maxLength > 0)
		{
			if (maxLength > original.length) original else original.take(maxLength)
		}
		else
			""
	}
	
	private def removeAt(index: Int) =
	{
		if (index >= 0)
			textPointer.update { old =>
				if (index < old.length)
					old.take(index) + old.drop(index + 1)
				else
					old
			}
	}
	
	private def removeSelectedText() =
	{
		selectedRange.foreach { selection =>
			caretIndex = selection.start
			val replaceStartCharIndex = measuredText.caretIndexToCharacterIndex(selection.start)
			val replaceEndCharIndex = selection.end min _text.length
			clearSelection()
			textPointer.update { old => old.take(replaceStartCharIndex) + old.drop(replaceEndCharIndex) }
		}
	}
	
	private def moveCaret(direction: Direction2D, selecting: Boolean = false, jumpWord: Boolean = false) =
	{
		if (selectedRange.nonEmpty && !selecting)
			clearSelection()
		else
		{
			val previousIndex = caretIndex
			val nextIndex =
			{
				// Word jump feature is only supported on the horizontal direction
				if (jumpWord)
				{
					if (direction.isHorizontal)
						skipWord(previousIndex, direction.sign)
					else
						None
				}
				else
					measuredText.caretIndexNextTo(previousIndex, direction)
			}
			nextIndex.foreach { newIndex =>
				// Moves the caret
				caretIndex = newIndex
				// Updates text selection, if specified
				if (selecting)
					selectedRangePointer.update
					{
						case Some((start, _)) =>
							// Extends the selected range to the new index
							Some(start -> newIndex).filterNot { case (start, end) => start == end }
						// Creates a new selected range
						case None => Some(previousIndex -> newIndex)
					}
			}
		}
	}
	
	// Returns new index after skip
	private def skipWord(originIndex: Int, direction: Direction1D) =
	{
		// Checks whether there is space to move
		val isSpaceAvailable = direction match
		{
			case Positive => originIndex <= _text.length - 1
			case Negative => originIndex > 0
		}
		if (isSpaceAvailable)
		{
			// Checks the first skipped character
			val firstCharIndex = direction match
			{
				case Positive => originIndex
				case Negative => originIndex -1
			}
			val nextChar = _text(firstCharIndex)
			// Skips either specific special characters or letter-digit combinations
			val takeCondition: Char => Boolean = if (nextChar.isLetterOrDigit) c => c.isLetterOrDigit else c => c == nextChar
			// Checks how many characters can be skipped
			val charsToSkip = (direction match
			{
				case Positive => _text.drop(originIndex + 1).iterator
				case Negative => _text.take(originIndex - 1).reverseIterator
			}).takeWhile(takeCondition).size + 1
			
			Some(originIndex + (charsToSkip * direction.modifier))
		}
		else
			None
	}
	
	
	// NESTED	----------------------------------
	
	private object FocusHandler extends FocusChangeListener
	{
		// ATTRIBUTES	--------------------------
		
		var hasFocus = false
		
		
		// IMPLEMENTED	--------------------------
		
		override def onFocusChangeEvent(event: FocusChangeEvent) =
		{
			// Tracks focus state
			hasFocus = event.hasFocus
			// Alters caret / selection on focus changes
			if (hasFocus)
			{
				moveCaretToEnd()
				CaretBlinker.show()
			}
			else
			{
				CaretBlinker.hide()
				clearSelection()
			}
		}
	}
	
	// Switches between visible and invisible caret
	private object CaretBlinker extends Actor
	{
		// ATTRIBUTES	--------------------------
		
		private var passedDuration = Duration.Zero
		
		
		// IMPLEMENTED	--------------------------
		
		override def act(duration: FiniteDuration) =
		{
			passedDuration += duration
			if (passedDuration >= caretBlinkFrequency)
			{
				resetCounter()
				caretVisibilityPointer.update { !_ }
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus && selectable
		
		
		// OTHER	------------------------------
		
		def resetCounter() = passedDuration = Duration.Zero
		
		// Makes caret visible for the full duration / refreshes duration if already visible
		def show() =
		{
			caretVisibilityPointer.value = true
			resetCounter()
		}
		
		def hide() = caretVisibilityPointer.value = false
	}
	
	private object KeyListener extends KeyTypedListener with KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES	--------------------------
		
		var keyStatus = KeyStatus.empty
		private val ignoredOnType = Set(KeyEvent.VK_ENTER, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE, KeyEvent.VK_TAB)
		
		
		// COMPUTED	------------------------------
		
		// NB: May throw
		private def clipBoard = Toolkit.getDefaultToolkit.getSystemClipboard
		
		
		// IMPLEMENTED	--------------------------
		
		override def onKeyTyped(event: KeyTypedEvent) =
		{
			// Skips cases handled by key state listening
			if (!ignoredOnType.contains(event.index) && font.toAwt.canDisplay(event.typedChar))
			{
				// Inserts the typed character into the string (if accepted by the content filter)
				if (inputFilter.forall { _(event.typedChar.toString) })
					insertToCaret(event.typedChar.toString)
			}
		}
		
		override def onKeyState(event: KeyStateEvent) =
		{
			keyStatus = event.keyStatus
			if (event.isDown)
			{
				event.arrow match
				{
					// On arrow keys, changes caret position (and possibly selected area)
					case Some(arrowDirection) => moveCaret(arrowDirection, event.keyStatus.shift, event.keyStatus.control)
					case None =>
						// Inserts a line-break on enter (if enabled)
						if (allowLineBreaks && event.index == KeyEvent.VK_ENTER)
							insertToCaret("\n")
						// Removes a character on backspace / delete
						else if (event.index == KeyEvent.VK_BACK_SPACE)
						{
							if (selectedRange.nonEmpty)
								removeSelectedText()
							else if (caretIndex > 0)
							{
								removeAt(caretIndex - 1)
								caretIndex -= 1
							}
						}
						else if (event.index == KeyEvent.VK_DELETE)
						{
							if (selectedRange.nonEmpty)
								removeSelectedText()
							else
								removeAt(caretIndex)
						}
						// Listens to shortcut keys (ctrl + C, V or X)
						else if (event.keyStatus.control)
						{
							if (event.index == KeyEvent.VK_C)
								selectedText.filterNot { _.isEmpty }.foreach { selected =>
									Try { clipBoard.setContents(new StringSelection(selected), this) }
								}
							else if (event.index == KeyEvent.VK_V && enabled)
								Try {
									// Retrieves the clipboard contents and pastes them on the string
									Option(clipBoard.getContents(null)).foreach { pasteContent =>
										if (pasteContent.isDataFlavorSupported(DataFlavor.stringFlavor)) {
											val rawPasteText = pasteContent.getTransferData(DataFlavor.stringFlavor).toString
											val actualPasteText = inputFilter match {
												case Some(filter) => filter.filter(rawPasteText)
												case None => rawPasteText
											}
											if (actualPasteText.nonEmpty)
												insertToCaret(actualPasteText)
										}
									}
								}
							else if (event.index == KeyEvent.VK_X && enabled)
								selectedText.foreach { textToCopy =>
									// Copies the cut content to the clip board. Will not remove the text if copy failed.
									Try { clipBoard.setContents(new StringSelection(textToCopy), this) } match
									{
										case Success(_) => removeSelectedText()
										case Failure(_) => ()
									}
								}
							else if (event.index == KeyEvent.VK_A)
								selectedRangePointer.value = Some(0 -> measuredText.maxCaretIndex)
						}
				}
			}
		}
		
		// Only listens to key events while focused
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus && (handlerType match
		{
			case KeyTypedHandlerType => enabled
			case _ => selectable
		})
		
		// Called when clipboard contents are lost. Ignores this event
		override def lostOwnership(clipboard: Clipboard, contents: Transferable) = ()
	}
	
	private object MouseListener extends MouseButtonStateListener with MouseMoveListener
	{
		// ATTRIBUTES	---------------------------------
		
		// Is only interested in left mouse button presses inside this component's area
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			MouseEvent.isOverAreaFilter(bounds)
		
		
		// IMPLEMENTED	---------------------------------
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			draggingMouse = true
			updateCaret(event.mousePosition, KeyListener.keyStatus.shift)
			Some(ConsumeEvent("EditableTextLabel clicked"))
		}
		
		override def onMouseMove(event: MouseMoveEvent) = updateCaret(event.mousePosition, selectArea = true)
		
		override def allowsHandlingFrom(handlerType: HandlerType) = handlerType match
		{
			case MouseMoveHandlerType => draggingMouse
			case _ => true
		}
		
		
		// OTHER	----------------------------------
		
		private def updateCaret(mousePosition: Point, selectArea: => Boolean) =
		{
			// Calculates and updates the new caret position
			val previousCaretIndex = caretIndex
			val (newLineIndex, newIndexOnLine) = measuredText.caretIndexClosestTo(
				(mousePosition - drawer.lastDrawPosition) * drawer.lastDrawScaling)
			val newCaretIndex = measuredText.mapCaretIndex(newLineIndex, newIndexOnLine)
			caretIndex = newCaretIndex
			
			// If this component wasn't on focus, requests focus now, otherwise checks if selection should be updated
			if (hasFocus)
			{
				if (_text.nonEmpty)
				{
					// Shift + click selects an area of text
					if (selectArea)
					{
						if (selectedRangePointer.value.forall { _._2 != newCaretIndex })
							selectedRangePointer.update
							{
								case Some((start, _)) => Some(start -> newCaretIndex)
								case None => Some(previousCaretIndex -> newCaretIndex)
							}
					}
					// If same caret position is clicked twice, expands the selection to the word around
					else if (previousCaretIndex == newCaretIndex)
					{
						val newSelectionStart = skipWord(previousCaretIndex, Negative).getOrElse(previousCaretIndex)
						val newSelectionEnd = skipWord(previousCaretIndex, Positive).getOrElse(previousCaretIndex)
						if (newSelectionStart != newSelectionEnd)
							selectedRangePointer.value = Some(newSelectionStart -> newSelectionEnd)
					}
					// Otherwise may cancel a selection
					else
						clearSelection()
				}
			}
			else
				requestFocus()
		}
	}
	
	private object GlobalMouseReleaseListener extends MouseButtonStateListener
	{
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.wasReleasedFilter
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			draggingMouse = false
			None
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = draggingMouse
	}
}

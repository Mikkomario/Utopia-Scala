package utopia.reflection.component.reach.input

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, DataFlavor, StringSelection, Transferable}
import java.awt.event.KeyEvent

import utopia.flow.datastructure.immutable.View
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.datastructure.template.Viewable
import utopia.flow.event.Changing
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, KeyStatus, KeyTypedEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.{Actor, KeyStateListener, KeyTypedHandlerType, KeyTypedListener, MouseButtonStateListener}
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.Direction2D
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.view.SelectableTextViewDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{MutableCustomDrawReachComponent, MutableFocusable}
import utopia.reflection.component.template.text.MutableTextComponent
import utopia.reflection.event.{FocusChangeEvent, FocusChangeListener, FocusListener}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.text.{FontMetricsContext, MeasuredText, Regex}
import utopia.reflection.localization.LocalString._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}

/**
  * Used for requesting user input in text format
  * @author Mikko Hilpinen
  * @since 30.10.2020, v2
  */
class EditableTextLabel(override val parentHierarchy: ComponentHierarchy,
						val baseStylePointer: PointerWithEvents[TextDrawContext],
						selectedTextColorPointer: Viewable[Color] = View(Color.textBlack),
						selectionBackgroundColorPointer: Viewable[Option[Color]] = View(None), caretColor: Color,
						caretWidth: Double = 1.0, caretBlinkFrequency: Duration = 0.5.seconds,
						val textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
						inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
						enabledPointer: Changing[Boolean] = Changing.wrap(true),
						allowSelectionWhileDisabled: Boolean = true, allowLineBreaks: Boolean = true,
						override val allowTextShrink: Boolean = false)
	extends MutableCustomDrawReachComponent with MutableTextComponent with MutableFocusable
{
	// ATTRIBUTES	-------------------------------
	
	private var focusLeaveConditions = Vector[String => (String, Boolean)]()
	
	override var focusListeners: Seq[FocusListener] = Vector(FocusHandler)
	
	val effectiveStylePointer = baseStylePointer.mergeWith(enabledPointer) { (style, enabled) =>
		if (enabled)
			style
		else
			style.mapColor { _.timesAlpha(0.66) }
	}
	val measuredTextPointer = textPointer.mergeWith(effectiveStylePointer) { (text, style) =>
		MeasuredText(text.noLanguageLocalizationSkipped, FontMetricsContext(fontMetrics(style.font),
			style.betweenLinesMargin), style.alignment, allowLineBreaks)
	}
	private val caretIndexPointer = new PointerWithEvents(textPointer.value.length - 1)
	private val caretVisibilityPointer = new PointerWithEvents(false)
	private val selectedRangePointer = new PointerWithEvents[Option[(Int, Int)]](None)
	
	private val drawer = SelectableTextViewDrawer(measuredTextPointer, effectiveStylePointer,
		selectedRangePointer.map { _.map { case (start, end) => if (start < end) start to end else end to start } },
		caretIndexPointer.mergeWith(caretVisibilityPointer) { (index, isVisible) =>
			if (isVisible && selectable) Some(index) else None },
		selectedTextColorPointer, selectionBackgroundColorPointer, caretColor, caretWidth)
	
	
	// INITIAL CODE	-------------------------------
	
	enableFocusHandlingWhileLinked()
	// TODO: Register listeners and drawers and such
	
	
	// COMPUTED	-----------------------------------
	
	def hasFocus = FocusHandler.hasFocus
	
	def enabled = enabledPointer.value
	
	def selectable = allowSelectionWhileDisabled || enabled
	
	def caretIndex = caretIndexPointer.value
	private def caretIndex_=(newIndex: Int) = caretIndexPointer.value = newIndex
	
	def selectedRange = selectedRangePointer.value.map { case (start, end) =>
		if (start < end) start to end else end to start }
	
	def selectedText = selectedRange.map { _text.slice(_) }.filterNot { _.isEmpty }
	
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
	
	
	// OTHER	----------------------------------
	
	def moveCaretToEnd() = caretIndexPointer.value = textPointer.value.length - 1
	
	/**
	  * Adds a new condition to be tested before allowing focus leave from this component
	  * @param condition A new condition to test (accepts current text, returns possibly modified text and a boolean
	  *                  indicating whether focus move is allowed)
	  */
	def addFocusLeaveCondition(condition: String => (String, Boolean)) = focusLeaveConditions :+= condition
	
	private def clearSelection() = selectedRangePointer.value = None
	
	private def insertToCaret(text: String) =
	{
		// May insert only part of the text, due to length limits
		val textToInsert = maxLength match
		{
			case Some(maxLength) =>
				val remainingSpace = maxLength - _text.length
				if (remainingSpace > 0)
				{
					if (remainingSpace > text.length) text else text.take(remainingSpace)
				}
				else
					""
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
	
	private def moveCaret(direction: Direction2D, selecting: Boolean = false, jumpWord: Boolean = false) =
	{
		if (selectedRange.nonEmpty && !selecting)
			clearSelection()
		else
		{
			val previousIndex = caretIndex
			(direction.axis match
			{
				case X =>
					if (jumpWord)
						skipWord(previousIndex, direction.sign)
					else
						direction.sign match
						{
							case Positive => if (previousIndex < _text.length) Some(previousIndex + 1) else None
							case Negative => if (previousIndex > 0) Some(previousIndex - 1) else None
						}
				case Y =>
					direction.sign match
					{
						case Positive => measuredText.caretIndexBelow(previousIndex)
						case Negative => measuredText.caretIndexAbove(previousIndex)
					}
			}).foreach { newIndex =>
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
			hasFocus = event.hasFocus
			if (hasFocus)
			{
				moveCaretToEnd()
				CaretBlinker.resetCounter()
				clearSelection()
			}
		}
	}
	
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
	}
	
	private object KeyListener extends KeyTypedListener with KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES	--------------------------
		
		var keyStatus = KeyStatus.empty
		
		
		// COMPUTED	------------------------------
		
		// NB: May throw
		private def clipBoard = Toolkit.getDefaultToolkit.getSystemClipboard
		
		
		// IMPLEMENTED	--------------------------
		
		override def onKeyTyped(event: KeyTypedEvent) =
		{
			// Inserts the typed character into the string (if accepted by the content filter)
			if (inputFilter.forall { _(event.typedChar.toString) })
				insertToCaret(event.typedChar.toString)
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
						// Listens to shortcut keys (ctrl + C, V or X)
						else if (event.keyStatus.control)
						{
							if (event.index == KeyEvent.VK_C)
								selectedText.filterNot {_.isEmpty}.foreach { selected =>
									Try {
										clipBoard.setContents(
											new StringSelection(selected), this)
									}
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
								selectedRange.foreach { range =>
									val textBefore = textPointer.value
									val (cut, remain) = textBefore.cut(range)
									if (cut.nonEmpty) {
										// Copies the cut content to the clip board. Will not remove the text if copy failed.
										Try {clipBoard.setContents(new StringSelection(cut), this)} match {
											case Success(_) => textPointer.value = remain
											case Failure(_) => ()
										}
									}
								}
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
		
		override def lostOwnership(clipboard: Clipboard, contents: Transferable) = ()
	}
	
	private object MouseListener extends MouseButtonStateListener with Handleable
	{
		// ATTRIBUTES	---------------------------------
		
		// Is only interested in left mouse button presses inside this component's area
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			MouseEvent.isOverAreaFilter(bounds)
		
		
		// IMPLEMENTED	---------------------------------
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			// Calculates and updates the new caret position
			val previousCaretIndex = caretIndex
			val (newLineIndex, newIndexOnLine) = measuredText.caretIndexClosestTo(
				(event.mousePosition - drawer.lastDrawPosition) * drawer.lastDrawScaling)
			val newCaretIndex = measuredText.mapIndex(newLineIndex, newIndexOnLine)
			caretIndex = newCaretIndex
			
			// If this component wasn't on focus, requests focus now, otherwise checks if selection should be updated
			if (hasFocus)
			{
				// If same caret position is clicked twice, expands the selection to the word around
				if (_text.nonEmpty)
				{
					if (previousCaretIndex == newCaretIndex)
					{
						val newSelectionStart = skipWord(previousCaretIndex, Negative).getOrElse(previousCaretIndex)
						val newSelectionEnd = skipWord(previousCaretIndex, Positive).getOrElse(previousCaretIndex)
						if (newSelectionStart != newSelectionEnd)
							selectedRangePointer.value = Some(newSelectionStart -> newSelectionEnd)
					}
					else if (KeyListener.keyStatus.shift)
						selectedRangePointer.update
						{
							case Some((start, _)) => Some(start -> newCaretIndex)
							case None => Some(previousCaretIndex -> newCaretIndex)
						}
				}
			}
			else
				requestFocus()
			
			Some(ConsumeEvent("EditableTextLabel clicked"))
		}
	}
}

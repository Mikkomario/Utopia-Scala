package utopia.reach.component.label.text.selectable

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.model.TextDrawContext
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color
import utopia.genesis.event._
import utopia.genesis.handling._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.paradigm.shape.shape2d.Point
import utopia.genesis.view.{GlobalKeyboardEventHandler, GlobalMouseEventHandler}
import utopia.inception.handling.HandlerType
import utopia.paradigm.enumeration.Direction2D
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.template.{CursorDefining, CustomDrawReachComponent}
import utopia.reach.cursor.Cursor
import utopia.reach.cursor.CursorType.{Default, Text}
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener}
import utopia.reach.drawing.Priority.VeryHigh
import utopia.firmament.drawing.view.SelectableTextViewDrawer
import utopia.firmament.component.text.TextComponent
import utopia.firmament.localization.LocalizedString

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, StringSelection, Transferable}
import java.awt.event.KeyEvent
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

/**
  * Displays text in a label while also allowing focus and text selection. Doesn't provide edit features.
  * The subclasses should register FocusHandler (inner object) as a focus listener.
  * @author Mikko Hilpinen
  * @since 30.10.2020, v0.3
  */
// TODO: Create a password mode where text is not displayed nor copyable
abstract class AbstractSelectableTextLabel
(override val parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler,
 textPointer: Changing[LocalizedString], stylePointer: Changing[TextDrawContext],
 selectedTextColorPointer: Changing[Color] = Fixed(Color.textBlack),
 selectionBackgroundColorPointer: Changing[Option[Color]] = Fixed(None),
 caretColorPointer: Changing[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
 caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
 override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with TextComponent with Focusable with CursorDefining
{
	// ATTRIBUTES	-------------------------------
	
	private var draggingMouse = false
	
	override val focusId = hashCode()
	/**
	  * A pointer to this label's measured text information
	  */
	val measuredTextPointer = textPointer.mergeWith(stylePointer)(measure)
	/**
	  * Pointer that contains the current caret index within text
	  */
	protected val caretIndexPointer = new PointerWithEvents(measuredText.maxCaretIndex)
	private val caretVisibilityPointer = new PointerWithEvents(false)
	private val drawnCaretPointer = caretIndexPointer.mergeWith(caretVisibilityPointer) { (index, isVisible) =>
		if (isVisible && selectable) Some(index) else None }
	// Selected range is in caret indices
	private val selectedRangePointer = new PointerWithEvents[Option[(Int, Int)]](None)
	
	/**
	  * The drawer that paints the contents of this component.
	  * Needs to be registered as a custom drawer to this component.
	  */
	protected val mainDrawer = SelectableTextViewDrawer(measuredTextPointer, stylePointer,
		selectedRangePointer.map { _.map { case (start, end) => if (start < end) start to end else end to start } },
		drawnCaretPointer, selectedTextColorPointer, selectionBackgroundColorPointer, caretColorPointer, caretWidth)
	private val repaintListener = ChangeListener.onAnyChange { repaint() }
	private val showCaretListener = ChangeListener.onAnyChange { if (hasFocus) CaretBlinker.show() }
	
	
	// ABSTRACT -----------------------------------
	
	/**
	  * @return Whether text in this label can currently be selected
	  */
	def selectable: Boolean
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return A pointer to this label's focus state
	  */
	def focusPointer = FocusHandler.focusPointer
	
	/**
	  * @return Whether this label is currently the focused component
	  */
	def hasFocus = FocusHandler.focus
	
	/**
	  * @return Current index of the text caret
	  */
	def caretIndex = caretIndexPointer.value
	protected def caretIndex_=(newIndex: Int) = {
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
	
	private def _text = textPointer.value.string
	
	
	// IMPLEMENTED	-------------------------------
	
	override def updateLayout() = ()
	
	override def measuredText = measuredTextPointer.value
	
	override def textDrawContext = stylePointer.value
	
	override def allowsFocusEnter = selectable
	
	override def cursorType = if (selectable) Text else Default
	
	override def cursorBounds = boundsInsideTop
	
	override def cursorToImage(cursor: Cursor, position: Point) = {
		// If hovering over a selected area, bases cursor color on that
		// Otherwise proposes standard text color
		selectionBackgroundColorPointer.value match {
			case Some(selectedAreaBackground) =>
				val highlightAreas = mainDrawer.drawTargets._2
				lazy val positionInTextBounds = ((this.position + position) -
					mainDrawer.lastDrawPosition) * mainDrawer.lastDrawScaling
				if (highlightAreas.nonEmpty && highlightAreas.exists { _._3.contains(positionInTextBounds) })
					cursor.over(selectedAreaBackground)
				else
					cursor.proposing(textColor)
			case None => cursor.proposing(textColor)
		}
	}
	
	
	// OTHER	----------------------------------
	
	/**
	  * Sets up this component's automated functions.
	  * Should be called when the sub-class has first been initialized.
	  */
	protected def setup() = {
		// Registers some listeners only while attached to top hierarchy
		addHierarchyListener { isAttached =>
			if (isAttached) {
				enableFocusHandling()
				GlobalKeyboardEventHandler.register(KeyListener)
				GlobalMouseEventHandler.register(GlobalMouseReleaseListener)
				actorHandler += CaretBlinker
				parentCanvas.cursorManager.foreach { _ += this }
			}
			else {
				disableFocusHandling()
				GlobalKeyboardEventHandler.unregister(KeyListener)
				GlobalMouseEventHandler.unregister(GlobalMouseReleaseListener)
				actorHandler -= CaretBlinker
				parentCanvas.cursorManager.foreach { _ -= this }
			}
		}
		addMouseButtonListener(MouseListener)
		addMouseMoveListener(MouseListener)
		
		// Repaints (and possibly revalidates) this component when content or styling changes
		measuredTextPointer.addListener { event =>
			if (event.equalsBy { _.size })
				repaint(VeryHigh)
			else
				revalidate()
		}
		stylePointer.addListener { change =>
			if (change.merge { _ hasSameDimensionsAs _ })
				repaint()
			else
				revalidate()
		}
		drawnCaretPointer.addListener(repaintListener)
		caretColorPointer.addListener(repaintListener)
		selectedRangePointer.addListener(repaintListener)
		selectedTextColorPointer.addListener(repaintListener)
		selectionBackgroundColorPointer.addListener(repaintListener)
		
		// Whenever text changes or caret position is updated, shows the caret
		caretIndexPointer.addListener(showCaretListener)
		textPointer.addListener(showCaretListener)
		
		// Clears or limits selected range whenever the text is updated
		textPointer.addListener { event =>
			selectedRangePointer.value.foreach { case (start, end) =>
				val maxIndex = event.newValue.string.length
				if (start >= maxIndex)
					clearSelection()
				else if (end > maxIndex)
					selectedRangePointer.value = Some(start -> maxIndex)
			}
		}
	}
	
	/**
	  * Moves caret to the end of this label's current text
	  */
	def moveCaretToEnd() = caretIndexPointer.value = measuredText.maxCaretIndex
	
	/**
	  * Clears the current selection area
	  */
	def clearSelection() = selectedRangePointer.value = None
	
	private def moveCaret(direction: Direction2D, selecting: Boolean = false, jumpWord: Boolean = false) = {
		if (selectedRange.nonEmpty && !selecting)
			clearSelection()
		else {
			val previousIndex = caretIndex
			val nextIndex = {
				// Word jump feature is only supported on the horizontal direction
				if (jumpWord) {
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
					selectedRangePointer.update {
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
	private def skipWord(originIndex: Int, direction: Sign) = {
		// Checks whether there is space to move
		val isSpaceAvailable = direction match {
			case Positive => originIndex <= _text.length - 1
			case Negative => originIndex > 0
		}
		if (isSpaceAvailable) {
			// Checks the first skipped character
			val firstCharIndex = direction match {
				case Positive => originIndex
				case Negative => originIndex -1
			}
			val nextChar = _text(firstCharIndex)
			// Skips either specific special characters or letter-digit combinations
			val takeCondition: Char => Boolean =
				if (nextChar.isLetterOrDigit) c => c.isLetterOrDigit else c => c == nextChar
			// Checks how many characters can be skipped
			val charsToSkip = (direction match {
				case Positive => _text.drop(originIndex + 1).iterator
				case Negative => _text.take(originIndex - 1).reverseIterator
			}).takeWhile(takeCondition).size + 1
			
			Some(originIndex + (charsToSkip * direction.modifier))
		}
		else
			None
	}
	
	
	// NESTED	----------------------------------
	
	/**
	  * The main focus handler of this label. Needs to be registered as a focus listener
	  * in order to work properly
	  */
	protected object FocusHandler extends FocusChangeListener
	{
		// ATTRIBUTES	--------------------------
		
		private val _focusPointer = new PointerWithEvents(false)
		
		
		// COMPUTED	------------------------------
		
		def focusPointer = _focusPointer.view
		
		def focus = _focusPointer.value
		private def focus_=(newState: Boolean) = _focusPointer.value = newState
		
		
		// IMPLEMENTED	--------------------------
		
		override def onFocusChangeEvent(event: FocusChangeEvent) = {
			// Tracks focus state
			focus = event.hasFocus
			// Alters caret / selection on focus changes
			if (event.hasFocus) {
				moveCaretToEnd()
				CaretBlinker.show()
			}
			else {
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
		
		override def act(duration: FiniteDuration) = {
			passedDuration += duration
			if (passedDuration >= caretBlinkFrequency) {
				resetCounter()
				caretVisibilityPointer.update { !_ }
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus && selectable
		
		
		// OTHER	------------------------------
		
		def resetCounter() = passedDuration = Duration.Zero
		
		// Makes caret visible for the full duration / refreshes duration if already visible
		def show() = {
			caretVisibilityPointer.value = true
			resetCounter()
		}
		
		def hide() = caretVisibilityPointer.value = false
	}
	
	private object KeyListener extends KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES	--------------------------
		
		var keyStatus = KeyStatus.empty
		
		
		// COMPUTED	------------------------------
		
		// NB: May throw
		private def clipBoard = Toolkit.getDefaultToolkit.getSystemClipboard
		
		
		// IMPLEMENTED	--------------------------
		
		override def onKeyState(event: KeyStateEvent) = {
			keyStatus = event.keyStatus
			if (event.isDown) {
				event.arrow match {
					// On arrow keys, changes caret position (and possibly selected area)
					case Some(arrowDirection) => moveCaret(arrowDirection, event.keyStatus.shift, event.keyStatus.control)
					case None =>
						// Listens to shortcut keys (ctrl + C, V or X)
						if (event.keyStatus.control) {
							if (event.index == KeyEvent.VK_C)
								selectedText.filterNot { _.isEmpty }.foreach { selected =>
									Try { clipBoard.setContents(new StringSelection(selected), this) }
								}
							else if (event.index == KeyEvent.VK_A)
								selectedRangePointer.value = Some(0 -> measuredText.maxCaretIndex)
						}
				}
			}
		}
		
		// Only listens to key events while focused
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus && selectable
		
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
		
		override def onMouseButtonState(event: MouseButtonStateEvent) = {
			draggingMouse = true
			updateCaret(event.mousePosition, KeyListener.keyStatus.shift)
			Some(ConsumeEvent("EditableTextLabel clicked"))
		}
		
		override def onMouseMove(event: MouseMoveEvent) = updateCaret(event.mousePosition, selectArea = true)
		
		override def allowsHandlingFrom(handlerType: HandlerType) = handlerType match {
			case MouseMoveHandlerType => draggingMouse
			case _ => true
		}
		
		
		// OTHER	----------------------------------
		
		private def updateCaret(mousePosition: Point, selectArea: => Boolean) = {
			// Calculates and updates the new caret position
			val previousCaretIndex = caretIndex
			val (newLineIndex, newIndexOnLine) = measuredText.caretIndexClosestTo(
				(mousePosition - mainDrawer.lastDrawPosition) * mainDrawer.lastDrawScaling)
			val newCaretIndex = measuredText.mapCaretIndex(newLineIndex, newIndexOnLine)
			caretIndex = newCaretIndex
			
			// If this component wasn't on focus, requests focus now, otherwise checks if selection should be updated
			if (hasFocus) {
				if (_text.nonEmpty) {
					// Shift + click selects an area of text
					if (selectArea) {
						if (selectedRangePointer.value.forall { _._2 != newCaretIndex })
							selectedRangePointer.update {
								case Some((start, _)) => Some(start -> newCaretIndex)
								case None => Some(previousCaretIndex -> newCaretIndex)
							}
					}
					// If same caret position is clicked twice, expands the selection to the word around
					else if (previousCaretIndex == newCaretIndex) {
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
		
		override def onMouseButtonState(event: MouseButtonStateEvent) = {
			draggingMouse = false
			None
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = draggingMouse
	}
}

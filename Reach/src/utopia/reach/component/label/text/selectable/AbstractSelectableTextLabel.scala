package utopia.reach.component.label.text.selectable

import utopia.firmament.component.text.TextComponent
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.SelectableTextViewDrawer
import utopia.firmament.localization.LocalizedString
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.Priority.VeryHigh
import utopia.genesis.handling.action.Actor
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.{Control, Shift}
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents, KeyboardState}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent, MouseButtonStateListener, MouseEvent, MouseMoveEvent, MouseMoveListener}
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{CursorDefining, CustomDrawReachComponent}
import utopia.reach.cursor.Cursor
import utopia.reach.cursor.CursorType.{Default, Text}
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener, FocusListener}

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, StringSelection, Transferable}
import java.awt.event.KeyEvent
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

/**
  * Displays text in a label while also allowing focus and text selection. Doesn't provide edit features.
  * @author Mikko Hilpinen
  * @since 30.10.2020, v0.3
  */
// TODO: Create a password mode where text is not displayed nor copyable
abstract class AbstractSelectableTextLabel(override val parentHierarchy: ComponentHierarchy,
                                           context: VariableTextContext, textPointer: Changing[LocalizedString],
                                           val selectableFlag: Flag,
                                           settings: SelectableTextLabelSettings = SelectableTextLabelSettings.default,
                                           enabledFlag: Flag = AlwaysTrue)
	extends CustomDrawReachComponent with TextComponent with FocusableWithState with CursorDefining
{
	// ATTRIBUTES	-------------------------------
	
	private val disabledFlag = !enabledFlag
	private val draggingFlag = ResettableFlag()
	
	override val focusId = hashCode()
	private val stylePointer = context.textDrawContextPointerFor(disabledFlag)
	/**
	  * A pointer to this label's measured text information
	  */
	val measuredTextPointer = textPointer.mergeWithWhile(stylePointer, linkedFlag)(measure)
	private val caretWidth = (context.margins.verySmall * 0.66) max 1.0
	/**
	  * Pointer that contains the current caret index within text
	  */
	protected val caretIndexPointer = EventfulPointer(measuredText.maxCaretIndex)
	private val caretVisibilityFlag = ResettableFlag()
	private val drawnCaretPointer = caretIndexPointer.mergeWith(caretVisibilityFlag) { (index, isVisible) =>
		if (isVisible && selectable) Some(index) else None }
	// Selected range is in caret indices
	private val selectedRangePointer = EventfulPointer[Option[(Int, Int)]](None)
	
	private val (selectionBgPointer, selectedTextColorPointer, caretColorPointer) = {
		// Case: Draws text selection background => Other colors are also affected
		if (settings.drawsSelectionBackground) {
			// Highlights using the specified highlight color
			val selectionBgPointer = context.colorPointer.light.forRole(settings.highlightColorPointer)
			// Picks the selected text color based on the selection background
			val selectedTextColorPointer = selectionBgPointer.map { _.shade.defaultTextColor }
			// Determines the caret color based on selection background
			val caretColorPointer = context.colorPointer.differentFromVariable(
				rolePointer = settings.customCaretColorPointer.getOrElse(settings.highlightColorPointer),
				competingColorPointer = selectionBgPointer)
			(Some(selectionBgPointer), selectedTextColorPointer, caretColorPointer)
		}
		// Case: Doesn't draw selection background => Highlights with colored text
		else {
			val selectionColorPointer = context.colorPointer.forRole(settings.highlightColorPointer)
			val caretColorPointer = settings.customCaretColorPointer match {
				case Some(custom) => context.colorPointer.forRole(custom)
				case None => selectionColorPointer
			}
			(None, selectionColorPointer, caretColorPointer)
		}
	}
	/**
	  * The drawer that paints the contents of this component.
	  */
	private val mainDrawer = SelectableTextViewDrawer(measuredTextPointer, stylePointer,
		selectedRangePointer.map { _.map { case (start, end) => if (start < end) start to end else end to start } },
		drawnCaretPointer, selectedTextColorPointer, View { selectionBgPointer.map { _.value } }, caretColorPointer,
		View { caretWidth })
	private val repaintListener = ChangeListener.onAnyChange { repaint() }
	private val showCaretListener = ChangeListener.onAnyChange { if (hasFocus) CaretBlinker.show() }
	
	override val focusListeners: Seq[FocusListener] = FocusHandler +: settings.focusListeners
	override val customDrawers: Seq[CustomDrawer] = mainDrawer +: settings.customDrawers
	
	/**
	  * A pointer that contains true while this label allows text selection (being in focus)
	  */
	protected lazy val interactiveFlag = FocusHandler.focusPointer && selectableFlag
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Whether text in this label can currently be selected
	  */
	def selectable: Boolean = selectableFlag.value
	
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
	
	override def focusPointer = FocusHandler.focusPointer
	
	override def measuredText = measuredTextPointer.value
	override def textDrawContext = stylePointer.value
	
	override def allowsFocusEnter = selectable
	override def allowTextShrink: Boolean = context.allowTextShrink
	
	override def cursorType = if (selectable) Text else Default
	override def cursorBounds = boundsInsideTop
	
	override def updateLayout() = ()
	
	override def cursorToImage(cursor: Cursor, position: Point) = {
		// If hovering over a selected area, bases cursor color on that
		// Otherwise proposes standard text color
		selectionBgPointer match {
			case Some(bgPointer) =>
				val highlightAreas = mainDrawer.drawTargets._2
				lazy val positionInTextBounds = ((this.position + position) -
					mainDrawer.lastDrawPosition) * mainDrawer.lastDrawScaling
				if (highlightAreas.nonEmpty && highlightAreas.exists { _._3.contains(positionInTextBounds) })
					cursor.over(bgPointer.value)
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
		val actorHandler = context.actorHandler
		addHierarchyListener { isAttached =>
			if (isAttached) {
				enableFocusHandling()
				KeyboardEvents += KeyListener
				CommonMouseEvents += CommonMouseReleaseListener
				actorHandler += CaretBlinker
				parentCanvas.cursorManager.foreach { _ += this }
			}
			else {
				disableFocusHandling()
				CommonMouseEvents -= CommonMouseReleaseListener
				KeyboardEvents -= KeyListener
				actorHandler -= CaretBlinker
				parentCanvas.cursorManager.foreach { _ -= this }
			}
		}
		this += MouseListener
		
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
		selectionBgPointer.foreach { _.addListener(repaintListener) }
		
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
		
		private val _focusPointer = EventfulPointer(false)
		/**
		  * A pointer that contains true while this component is in focus
		  */
		val focusPointer: Flag = _focusPointer.readOnly
		
		
		// COMPUTED	------------------------------
		
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
		
		override def handleCondition: Flag = interactiveFlag
		
		override def act(duration: FiniteDuration) = {
			passedDuration += duration
			if (passedDuration >= settings.caretBlinkFrequency) {
				resetCounter()
				caretVisibilityFlag.switch()
			}
		}
		
		
		// OTHER	------------------------------
		
		def resetCounter() = passedDuration = Duration.Zero
		
		// Makes caret visible for the full duration / refreshes duration if already visible
		def show() = {
			caretVisibilityFlag.set()
			resetCounter()
		}
		
		def hide() = caretVisibilityFlag.reset()
	}
	
	private object KeyListener extends KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES	--------------------------
		
		var keyStatus = KeyboardState.default
		
		override val handleCondition: Flag = focusPointer && selectableFlag
		
		
		// COMPUTED	------------------------------
		
		// NB: May throw
		private def clipBoard = Toolkit.getDefaultToolkit.getSystemClipboard
		
		
		// IMPLEMENTED	--------------------------
		
		override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
		
		override def onKeyState(event: KeyStateEvent) = {
			keyStatus = event.keyboardState
			if (event.pressed) {
				event.arrow match {
					// On arrow keys, changes caret position (and possibly selected area)
					case Some(arrowDirection) =>
						moveCaret(arrowDirection, event.keyboardState(Shift), event.keyboardState(Control))
					case None =>
						// Listens to shortcut keys (ctrl + C, V or X)
						if (event.keyboardState(Control)) {
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
		
		// Called when clipboard contents are lost. Ignores this event
		override def lostOwnership(clipboard: Clipboard, contents: Transferable) = ()
	}
	
	private object MouseListener extends MouseButtonStateListener with MouseMoveListener
	{
		// ATTRIBUTES	---------------------------------
		
		private val doubleClickThreshold = 0.5.seconds
		private var lastClickTime = Now.toInstant
		
		// Is only interested in left mouse button presses inside this component's area
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(bounds)
		
		
		// IMPLEMENTED	---------------------------------
		
		override def handleCondition: Flag = AlwaysTrue
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			draggingFlag.set()
			updateCaret(event.position, KeyListener.keyStatus(Shift))
			lastClickTime = Now
			Consume("EditableTextLabel clicked")
		}
		
		override def onMouseMove(event: MouseMoveEvent) = {
			if (draggingFlag.isSet)
				updateCaret(event.position, selectArea = true)
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
					// If same caret position is clicked twice within a short period of time,
					// expands the selection to the word around
					else if (previousCaretIndex == newCaretIndex && Now - lastClickTime < doubleClickThreshold) {
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
	
	private object CommonMouseReleaseListener extends MouseButtonStateListener
	{
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.released
		
		override def handleCondition: Flag = draggingFlag
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			draggingFlag.reset()
			Preserve
		}
	}
}

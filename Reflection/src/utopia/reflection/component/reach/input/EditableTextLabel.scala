package utopia.reflection.component.reach.input

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, DataFlavor, StringSelection, Transferable}
import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.color.Color
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.{Actor, KeyStateListener, KeyTypedListener}
import utopia.inception.handling.HandlerType
import utopia.reflection.component.drawing.immutable.TextDrawContext
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
						val stylePointer: PointerWithEvents[TextDrawContext],
						caretColor: Color, selectionBackgroundColor: Color, selectedTextColor: Color = Color.textBlack,
						caretWidth: Double = 1.0, caretBlinkFrequency: Duration = 0.5.seconds,
						val textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
						inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
						allowLineBreaks: Boolean = true)
	extends MutableCustomDrawReachComponent with MutableTextComponent with MutableFocusable
{
	// ATTRIBUTES	-------------------------------
	
	override var focusListeners: Seq[FocusListener] = Vector(FocusHandler)
	
	val measuredTextPointer = textPointer.mergeWith(stylePointer) { (text, style) =>
		MeasuredText(text.noLanguageLocalizationSkipped, FontMetricsContext(fontMetrics(style.font),
			style.betweenLinesMargin), style.alignment, allowLineBreaks)
	}
	private val caretIndexPointer = new PointerWithEvents(textPointer.value.length - 1)
	private val caretVisibilityPointer = new PointerWithEvents(false)
	private val selectedRangePointer = new PointerWithEvents[Option[Range]](None)
	
	
	// INITIAL CODE	-------------------------------
	
	enableFocusHandlingWhileLinked()
	
	
	// COMPUTED	-----------------------------------
	
	def hasFocus = FocusHandler.hasFocus
	
	def caretIndex = caretIndexPointer.value
	
	def selectedRange = selectedRangePointer.value
	
	def selectedText = selectedRange.map { textPointer.value(_) }.filterNot { _.isEmpty }
	
	def text_=(newText: String) = textPointer.value = newText
	
	
	// IMPLEMENTED	-------------------------------
	
	override def text_=(newText: LocalizedString) = textPointer.value = newText.string
	
	override def updateLayout() = ()
	
	override def drawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def measuredText = measuredTextPointer.value
	
	override def drawContext = stylePointer.value
	
	override def allowTextShrink = false
	
	override def allowsFocusEnter = ???
	
	override def allowsFocusLeave = ???
	
	
	// OTHER	----------------------------------
	
	def moveCaretToEnd() = caretIndexPointer.value = textPointer.value.length - 1
	
	private def insertToCaret(text: String) =
	{
		val (before, after) = textPointer.value.splitAt(caretIndex)
		textPointer.value = before + text + after
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
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus
		
		
		// OTHER	------------------------------
		
		def resetCounter() = passedDuration = Duration.Zero
	}
	
	private object KeyListener extends KeyTypedListener with KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES	--------------------------
		
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter
		
		
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
		
		// TODO: Handle arrow key press events
		override def onKeyState(event: KeyStateEvent) =
		{
			// Inserts a line-break on enter (if enabled)
			if (allowLineBreaks && event.index == KeyEvent.VK_ENTER)
				insertToCaret("\n")
			// Listens to shortcut keys (ctrl + C, V or X)
			else if (event.keyStatus.control)
			{
				if (event.index == KeyEvent.VK_C)
					selectedText.filterNot { _.isEmpty }.foreach { selected => Try { clipBoard.setContents(
						new StringSelection(selected), this) }
					}
				else if (event.index == KeyEvent.VK_V)
					Try {
						Option(clipBoard.getContents(null)).foreach { pasteContent =>
							if (pasteContent.isDataFlavorSupported(DataFlavor.stringFlavor))
							{
								val rawPasteText = pasteContent.getTransferData(DataFlavor.stringFlavor).toString
								inputFilter match
								{
									case Some(filter) => insertToCaret(filter.filter(rawPasteText))
									case None => insertToCaret(rawPasteText)
								}
							}
						}
					}
				else if (event.index == KeyEvent.VK_X)
					selectedRange.foreach { range =>
						val textBefore = textPointer.value
						val (cut, remain) = textBefore.cut(range)
						if (cut.nonEmpty)
						{
							// Copies the cut content to the clip board. Will not remove the text if copy failed.
							Try { clipBoard.setContents(new StringSelection(cut), this) } match
							{
								case Success(_) => textPointer.value = remain
								case Failure(_) => ()
							}
						}
					}
			}
		}
		
		// Only listens to key events while focused
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus
		
		override def lostOwnership(clipboard: Clipboard, contents: Transferable) = ()
	}
}

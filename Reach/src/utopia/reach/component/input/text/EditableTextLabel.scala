package utopia.reach.component.input.text

import utopia.flow.parse.string.Regex
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{KeyStateListener, KeyTypedHandlerType, KeyTypedListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.selectable.AbstractSelectableTextLabel
import utopia.reach.component.template.focus.MutableFocusable
import utopia.reach.focus.FocusListener
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.MutableCustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.localization.LocalString._
import utopia.reflection.util.ComponentCreationDefaults

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, DataFlavor, StringSelection, Transferable}
import java.awt.event.KeyEvent
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object EditableTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike, EditableTextLabelFactory,
	ContextualEditableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new EditableTextLabelFactory(hierarchy)
}

class EditableTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualEditableTextLabelFactory]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[N <: TextContextLike](context: N) =
		ContextualEditableTextLabelFactory(this, context)
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Creates a new editable text label
	  * @param actorHandler Actor handler that will deliver required action events
	  * @param stylePointer A pointer to this label's styling
	  * @param selectedTextColorPointer A pointer to this label's selected text's color (default = always standard black)
	  * @param selectionBackgroundColorPointer A pointer to this label's selected text's background color, if any
	  *                                        (default = always None)
	  * @param caretColorPointer A pointer to the color used when drawing the caret (default = always standard black)
	  * @param caretWidth Width of the drawn caret (default = 1 pixels)
	  * @param caretBlinkFrequency Frequency how often caret visibility is changed (default = every 0.5 seconds)
	  * @param textPointer A pointer to this label's text (default = new empty pointer)
	  * @param inputFilter A filter regex applied to typed and pasted input, if any (default = None)
	  * @param maxLength Maximum text length, if any (default = None)
	  * @param enabledPointer A pointer to this label's enabled state (default = always enabled)
	  * @param allowSelectionWhileDisabled Whether this label's text should be selectable even while it is disabled
	  *                                    (default = true)
	  * @param allowTextShrink Whether the drawn text should be shrank to conserve space when required (default = false)
	  * @return A new label
	  */
	def apply(actorHandler: ActorHandler, stylePointer: Changing[TextDrawContext],
	          selectedTextColorPointer: Changing[Color] = Fixed(Color.textBlack),
	          selectionBackgroundColorPointer: Changing[Option[Color]] = Fixed(None),
	          caretColorPointer: Changing[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
	          caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
	          textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
	          inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
	          enabledPointer: Changing[Boolean] = AlwaysTrue,
	          allowSelectionWhileDisabled: Boolean = true, allowTextShrink: Boolean = false) =
		new EditableTextLabel(parentHierarchy, actorHandler, stylePointer, selectedTextColorPointer,
			selectionBackgroundColorPointer, caretColorPointer, caretWidth, caretBlinkFrequency, textPointer,
			inputFilter, maxLength, enabledPointer, allowSelectionWhileDisabled, allowTextShrink)
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
	          enabledPointer: Changing[Boolean] = AlwaysTrue,
	          caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
	          allowSelectionWhileDisabled: Boolean = true) =
	{
		val selectionBackground = context.color(Secondary, Light)
		val caretColor = context.colorScheme.secondary.bestAgainst(
			Vector(context.containerBackground, selectionBackground))
		factory(context.actorHandler, Fixed(TextDrawContext.contextual(context)),
			Fixed(selectionBackground.defaultTextColor), Fixed(Some(selectionBackground)),
			Fixed(caretColor), (context.margins.verySmall * 0.66) max 1.0, caretBlinkFrequency, textPointer,
			inputFilter, maxLength, enabledPointer, allowSelectionWhileDisabled, context.allowTextShrink)
	}
}

/**
  * Used for requesting user input in text format
  * @author Mikko Hilpinen
  * @since 30.10.2020, v0.1
  */
// TODO: Create a password mode where text is not displayed nor copyable
// TODO: Should also support input modification (e.g. upper-casing)
class EditableTextLabel(parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler,
                        baseStylePointer: Changing[TextDrawContext],
                        selectedTextColorPointer: Changing[Color] = Fixed(Color.textBlack),
                        selectionBackgroundColorPointer: Changing[Option[Color]] = Fixed(None),
                        caretColorPointer: Changing[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
                        caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
                        val textPointer: PointerWithEvents[String] = new PointerWithEvents(""),
                        inputFilter: Option[Regex] = None, maxLength: Option[Int] = None,
                        enabledPointer: Changing[Boolean] = AlwaysTrue,
                        allowSelectionWhileDisabled: Boolean = true, allowTextShrink: Boolean = false)
	extends AbstractSelectableTextLabel(parentHierarchy, actorHandler,
		textPointer.map { _.noLanguageLocalizationSkipped },
		baseStylePointer.mergeWith(enabledPointer) { (style, enabled) =>
			if (enabled) style else style.mapColor { _.timesAlpha(0.66) } }, selectedTextColorPointer,
		selectionBackgroundColorPointer, caretColorPointer, caretWidth, caretBlinkFrequency, allowTextShrink)
		with MutableCustomDrawable with MutableFocusable
{
	// ATTRIBUTES	-------------------------------
	
	private var focusLeaveConditions = Vector[String => (String, Boolean)]()
	
	override var customDrawers = Vector[CustomDrawer](mainDrawer)
	override var focusListeners: Seq[FocusListener] = Vector(FocusHandler)
	
	
	// INITIAL CODE	-------------------------------
	
	setup()
	addHierarchyListener { isAttached =>
		if (isAttached)
			GlobalKeyboardEventHandler += KeyListener
		else
			GlobalKeyboardEventHandler -= KeyListener
	}
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Whether this label is currently enabled
	  */
	def enabled = enabledPointer.value
	
	def text = textPointer.value
	def text_=(newText: String) = textPointer.value = newText
	
	private def _text = textPointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
	/**
	  * @return Whether text in this label can currently be selected
	  */
	def selectable = allowSelectionWhileDisabled || enabled
	
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
					
					textPointer.value = before + textToInsert + after
					// Moves the caret to the end of the inserted string
					caretIndexPointer.update { _ + textToInsert.length }
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
	
	
	// NESTED	----------------------------------
	
	private object KeyListener extends KeyTypedListener with KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES	--------------------------
		
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
			if (event.isDown)
			{
				// Inserts a line-break on enter (if enabled)
				if (drawContext.allowLineBreaks && event.index == KeyEvent.VK_ENTER)
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
					if (event.index == KeyEvent.VK_V && enabled)
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
}

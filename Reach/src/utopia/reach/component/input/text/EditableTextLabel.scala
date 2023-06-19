package utopia.reach.component.input.text

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalString._
import utopia.flow.parse.string.Regex
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{KeyStateEvent, KeyTypedEvent}
import utopia.genesis.handling.{KeyStateListener, KeyTypedHandlerType, KeyTypedListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.paradigm.color.ColorRole
import utopia.reach.component.factory.FromContextComponentFactoryFactory
import utopia.reach.component.factory.contextual.{VariableBackgroundRoleAssignableFactory, VariableContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.selectable.{AbstractSelectableTextLabel, SelectableTextLabelSettings, SelectableTextLabelSettingsLike}
import utopia.reach.focus.FocusListener

import java.awt.Toolkit
import java.awt.datatransfer.{Clipboard, ClipboardOwner, DataFlavor, StringSelection, Transferable}
import java.awt.event.KeyEvent
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
  * Common trait for editable text label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait EditableTextLabelSettingsLike[+Repr] extends SelectableTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped general selectable text label settings
	  */
	def labelSettings: SelectableTextLabelSettings
	
	/**
	  * Pointer that determines whether this label is interactive or not
	  */
	def enabledPointer: Changing[Boolean]
	/**
	  * Filter that determines what input strings are recognized.
	  * This filter is used to test individual characters.
	  */
	def inputFilter: Option[Regex]
	/**
	  * Longest allowed input length. None if no maximum is defined.
	  */
	def maxLength: Option[Int]
	/**
	  * Whether text selection should be allowed while the editing features are disabled
	  */
	def allowsSelectionWhileDisabled: Boolean
	
	/**
	  * Whether text selection should be allowed while the editing features are disabled
	  * @param allow New allows selection while disabled to use.
	  *              Whether text selection should be allowed while the editing features are disabled
	  * @return Copy of this factory with the specified allows selection while disabled
	  */
	def withAllowsSelectionWhileDisabled(allow: Boolean): Repr
	/**
	  * Pointer that determines whether this label is interactive or not
	  * @param p New enabled pointer to use.
	  *          Pointer that determines whether this label is interactive or not
	  * @return Copy of this factory with the specified enabled pointer
	  */
	def withEnabledPointer(p: Changing[Boolean]): Repr
	/**
	  * Filter that determines what input strings are recognized.
	  * This filter is used to test individual characters.
	  * @param filter New input filter to use.
	  *               Filter that determines what input strings are recognized.
	  *               This filter is used to test individual characters.
	  * @return Copy of this factory with the specified input filter
	  */
	def withInputFilter(filter: Option[Regex]): Repr
	/**
	  * Wrapped general selectable text label settings
	  * @param settings New label settings to use.
	  *                 Wrapped general selectable text label settings
	  * @return Copy of this factory with the specified label settings
	  */
	def withLabelSettings(settings: SelectableTextLabelSettings): Repr
	/**
	  * Longest allowed input length. None if no maximum is defined.
	  * @param max New max length to use.
	  *            Longest allowed input length. None if no maximum is defined.
	  * @return Copy of this factory with the specified max length
	  */
	def withMaxLength(max: Option[Int]): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this factory that only accepts digits as input
	  */
	def onlyPositiveIntegers = withInputFilter(Regex.digit)
	/**
	  * @return Copy of this factory that only accepts positive or negative integers as input
	  */
	def onlyIntegers = withInputFilter(Regex.numericParts)
	/**
	  * @return Copy of this factory that only accepts positive decimal and integer numbers as input
	  */
	def onlyPositiveNumbers = withInputFilter(Regex.decimalPositiveParts)
	/**
	  * @return Copy of this factory that only accepts decimal and integer numbers as input
	  */
	def onlyNumbers = withInputFilter(Regex.decimalParts)
	/**
	  * @return Copy of this factory that only accepts letters as input
	  */
	def onlyLetters = withInputFilter(Regex.alpha)
	/**
	  * @return Copy of this factory that only accepts letters and digits as input
	  */
	def onlyAlphaNumeric = withInputFilter(Regex.alphaNumeric)
	
	
	// IMPLEMENTED	--------------------
	
	override def caretBlinkFrequency = labelSettings.caretBlinkFrequency
	override def customCaretColorPointer = labelSettings.customCaretColorPointer
	override def customDrawers = labelSettings.customDrawers
	override def drawsSelectionBackground = labelSettings.drawsSelectionBackground
	override def highlightColorPointer = labelSettings.highlightColorPointer
	override def focusListeners: Vector[FocusListener] = labelSettings.focusListeners
	
	override def withFocusListeners(listeners: Vector[FocusListener]): Repr =
		mapLabelSettings { _.withFocusListeners(listeners) }
	override def withCaretBlinkFrequency(frequency: Duration) =
		withLabelSettings(labelSettings.withCaretBlinkFrequency(frequency))
	override def withCustomCaretColorPointer(p: Option[Changing[ColorRole]]) =
		withLabelSettings(labelSettings.withCustomCaretColorPointer(p))
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		withLabelSettings(labelSettings.withCustomDrawers(drawers))
	override def withDrawSelectionBackground(drawBackground: Boolean) =
		withLabelSettings(labelSettings.withDrawSelectionBackground(drawBackground))
	override def withHighlightColorPointer(p: Changing[ColorRole]) =
		withLabelSettings(labelSettings.withHighlightColorPointer(p))
	
	
	// OTHER	--------------------
	
	def mapEnabledPointer(f: Changing[Boolean] => Changing[Boolean]) = withEnabledPointer(f(enabledPointer))
	def mapLabelSettings(f: SelectableTextLabelSettings => SelectableTextLabelSettings) =
		withLabelSettings(f(labelSettings))
	
	def mapInputFilter(f: Option[Regex] => Option[Regex]) = withInputFilter(f(inputFilter))
	
	/**
	  * @param filter New input filter that is applied to every typed character
	  * @return Copy of this factory with the specified input filter
	  */
	def withInputFilter(filter: Regex): Repr = withInputFilter(Some(filter))
	/**
	  * @param maxLength Maximum input length
	  * @return Copy of this factory with the specified maximum input length
	  */
	def withMaxLength(maxLength: Int): Repr = withMaxLength(Some(maxLength))
}

object EditableTextLabelSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing editable text labels
  * @param labelSettings                Wrapped general selectable text label settings
  * @param enabledPointer               Pointer that determines whether this label is interactive or not
  * @param inputFilter                  Filter that determines what input strings are recognized.
  *                                     This filter is used to test individual characters.
  * @param maxLength                    Longest allowed input length. None if no maximum is defined.
  * @param allowsSelectionWhileDisabled Whether text selection should be allowed while the editing
  *                                     features are disabled
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class EditableTextLabelSettings(labelSettings: SelectableTextLabelSettings = SelectableTextLabelSettings.default,
                                     enabledPointer: Changing[Boolean] = AlwaysTrue, inputFilter: Option[Regex] = None,
                                     maxLength: Option[Int] = None,
                                     allowsSelectionWhileDisabled: Boolean = true)
	extends EditableTextLabelSettingsLike[EditableTextLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withAllowsSelectionWhileDisabled(allow: Boolean) =
		copy(allowsSelectionWhileDisabled = allow)
	override def withEnabledPointer(p: Changing[Boolean]) = copy(enabledPointer = p)
	override def withInputFilter(filter: Option[Regex]) = copy(inputFilter = filter)
	override def withLabelSettings(settings: SelectableTextLabelSettings) = copy(labelSettings = settings)
	override def withMaxLength(max: Option[Int]) = copy(maxLength = max)
}

/**
  * Common trait for factories that wrap a editable text label settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait EditableTextLabelSettingsWrapper[+Repr] extends EditableTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: EditableTextLabelSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: EditableTextLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def allowsSelectionWhileDisabled = settings.allowsSelectionWhileDisabled
	override def enabledPointer = settings.enabledPointer
	override def inputFilter = settings.inputFilter
	override def labelSettings = settings.labelSettings
	override def maxLength = settings.maxLength
	
	override def withAllowsSelectionWhileDisabled(allow: Boolean) =
		mapSettings { _.withAllowsSelectionWhileDisabled(allow) }
	override def withEnabledPointer(p: Changing[Boolean]) = mapSettings { _.withEnabledPointer(p) }
	override def withInputFilter(filter: Option[Regex]) = mapSettings { _.withInputFilter(filter) }
	override def withLabelSettings(settings: SelectableTextLabelSettings) =
		mapSettings { _.withLabelSettings(settings) }
	override def withMaxLength(max: Option[Int]) = mapSettings { _.withMaxLength(max) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: EditableTextLabelSettings => EditableTextLabelSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing editable text labels using contextual component creation information
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ContextualEditableTextLabelFactory(parentHierarchy: ComponentHierarchy,
                                              contextPointer: Changing[TextContext],
                                              settings: EditableTextLabelSettings = EditableTextLabelSettings.default,
                                              drawsBackground: Boolean = false)
	extends EditableTextLabelSettingsWrapper[ContextualEditableTextLabelFactory]
		with VariableContextualFactory[TextContext, ContextualEditableTextLabelFactory]
		with VariableBackgroundRoleAssignableFactory[TextContext, ContextualEditableTextLabelFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: EditableTextLabelSettings) =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContextPointer: Changing[TextContext],
	                                                     backgroundDrawer: CustomDrawer): ContextualEditableTextLabelFactory =
		copy(contextPointer = newContextPointer, settings = settings.withCustomBackgroundDrawer(backgroundDrawer),
			drawsBackground = true)
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new editable label
	  * @param textPointer A pointer to this label's text (default = new empty pointer)
	  * @return a new label
	  */
	def apply(textPointer: PointerWithEvents[String] = new PointerWithEvents("")) =
		new EditableTextLabel(parentHierarchy, contextPointer, settings, textPointer)
}

/**
  * Used for defining editable text label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class EditableTextLabelSetup(settings: EditableTextLabelSettings = EditableTextLabelSettings.default)
	extends EditableTextLabelSettingsWrapper[EditableTextLabelSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualEditableTextLabelFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualEditableTextLabelFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: EditableTextLabelSettings) = copy(settings = settings)
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new editable text label factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualEditableTextLabelFactory(hierarchy, context, settings)
}

object EditableTextLabel extends EditableTextLabelSetup()
{
	// OTHER	--------------------
	
	def apply(settings: EditableTextLabelSettings) = withSettings(settings)
}

/**
  * Used for requesting user input in text format
  * @author Mikko Hilpinen
  * @since 30.10.2020, v0.1
  */
// TODO: Create a password mode where text is not displayed nor copyable
// TODO: Should also support input modification (e.g. upper-casing)
class EditableTextLabel(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                        settings: EditableTextLabelSettings = EditableTextLabelSettings.default,
                        val textPointer: PointerWithEvents[String] = new PointerWithEvents(""))
	extends AbstractSelectableTextLabel(parentHierarchy, contextPointer,
		textPointer.map { _.noLanguageLocalizationSkipped }, settings.labelSettings, settings.enabledPointer)
{
	// ATTRIBUTES	-------------------------------
	
	private var focusLeaveConditions = Vector[String => (String, Boolean)]()
	
	
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
	def enabled = settings.enabledPointer.value
	
	def text = textPointer.value
	def text_=(newText: String) = textPointer.value = newText
	
	private def _text = textPointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
	/**
	  * @return Whether text in this label can currently be selected
	  */
	def selectable = settings.allowsSelectionWhileDisabled || enabled
	
	override def allowsFocusLeave = {
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
	
	private def insertToCaret(text: String) = {
		// If a range of characters is selected, overwrites those, then removes the selection
		selectedRange match {
			case Some(selection) =>
				val replaceText = settings.maxLength match {
					case Some(maxLength) =>
						val availableSpace = maxLength - _text.length + selection.length
						limitedLengthString(availableSpace, text)
					case None => text
				}
				val replaceStartCharIndex = measuredText.caretIndexToCharacterIndex(selection.start)
				val replaceEndCharIndex = measuredText.caretIndexToCharacterIndex(selection.end)
				clearSelection()
				textPointer.update { old => s"${ old.take(replaceStartCharIndex) }$replaceText${ old.drop(replaceEndCharIndex) }" }
				caretIndex = selection.start + replaceText.length
			case None =>
				// May insert only part of the text, due to length limits
				val textToInsert = settings.maxLength match {
					case Some(maxLength) => limitedLengthString(maxLength - _text.length, text)
					case None => text
				}
				if (textToInsert.nonEmpty) {
					val (before, after) = textPointer.value.splitAt(caretIndex)
					
					textPointer.value = s"$before$textToInsert$after"
					// Moves the caret to the end of the inserted string
					caretIndexPointer.update { _ + textToInsert.length }
				}
		}
	}
	
	private def limitedLengthString(maxLength: Int, original: String) = {
		if (maxLength > 0) {
			if (maxLength > original.length) original else original.take(maxLength)
		}
		else
			""
	}
	
	private def removeAt(index: Int) = {
		if (index >= 0)
			textPointer.update { old =>
				if (index < old.length)
					s"${ old.take(index) }${ old.drop(index + 1) }"
				else
					old
			}
	}
	
	private def removeSelectedText() = {
		selectedRange.foreach { selection =>
			caretIndex = selection.start
			val replaceStartCharIndex = measuredText.caretIndexToCharacterIndex(selection.start)
			val replaceEndCharIndex = selection.end min _text.length
			clearSelection()
			textPointer.update { old => s"${ old.take(replaceStartCharIndex) }${ old.drop(replaceEndCharIndex) }" }
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
		
		override def onKeyTyped(event: KeyTypedEvent) ={
			// Skips cases handled by key state listening
			if (!ignoredOnType.contains(event.index) && font.toAwt.canDisplay(event.typedChar)) {
				// Inserts the typed character into the string (if accepted by the content filter)
				if (settings.inputFilter.forall { _(event.typedChar.toString) })
					insertToCaret(event.typedChar.toString)
			}
		}
		
		override def onKeyState(event: KeyStateEvent) =
		{
			if (event.isDown) {
				// Inserts a line-break on enter (if enabled)
				if (textDrawContext.allowLineBreaks && event.index == KeyEvent.VK_ENTER)
					insertToCaret("\n")
				// Removes a character on backspace / delete
				else if (event.index == KeyEvent.VK_BACK_SPACE) {
					if (selectedRange.nonEmpty)
						removeSelectedText()
					else if (caretIndex > 0) {
						removeAt(caretIndex - 1)
						caretIndex -= 1
					}
				}
				else if (event.index == KeyEvent.VK_DELETE) {
					if (selectedRange.nonEmpty)
						removeSelectedText()
					else
						removeAt(caretIndex)
				}
				// Listens to shortcut keys (ctrl + C, V or X)
				else if (event.keyStatus.control) {
					if (event.index == KeyEvent.VK_V && enabled)
						Try {
							// Retrieves the clipboard contents and pastes them on the string
							Option(clipBoard.getContents(null)).foreach { pasteContent =>
								if (pasteContent.isDataFlavorSupported(DataFlavor.stringFlavor)) {
									val rawPasteText = pasteContent.getTransferData(DataFlavor.stringFlavor).toString
									val actualPasteText = settings.inputFilter match {
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
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus && (handlerType match {
			case KeyTypedHandlerType => enabled
			case _ => selectable
		})
		
		// Called when clipboard contents are lost. Ignores this event
		override def lostOwnership(clipboard: Clipboard, contents: Transferable) = ()
	}
}

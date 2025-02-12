package utopia.reach.component.input.text

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalString._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.operator.filter.Filter
import utopia.flow.parse.string.Regex
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.keyboard.Key.{BackSpace, Control, Delete, Tab}
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard._
import utopia.paradigm.color.ColorRole
import utopia.reach.component.factory.FromContextComponentFactoryFactory
import utopia.reach.component.factory.contextual.{ContextualFactory, VariableBackgroundRoleAssignableFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.selectable.{AbstractSelectableTextLabel, SelectableTextLabelSettings, SelectableTextLabelSettingsLike}
import utopia.reach.focus.FocusListener

import java.awt.Toolkit
import java.awt.datatransfer._
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
	def onlyIntegers = withInputFilter(Regex.integerPart)
	/**
	  * @return Copy of this factory that only accepts positive decimal and integer numbers as input
	  */
	def onlyPositiveNumbers = withInputFilter(Regex.positiveNumberPart)
	/**
	  * @return Copy of this factory that only accepts decimal and integer numbers as input
	  */
	def onlyNumbers = withInputFilter(Regex.numberPart)
	/**
	  * @return Copy of this factory that only accepts letters as input
	  */
	def onlyLetters = withInputFilter(Regex.letter)
	/**
	  * @return Copy of this factory that only accepts letters and digits as input
	  */
	def onlyAlphaNumeric = withInputFilter(Regex.letterOrDigit)
	
	
	// IMPLEMENTED	--------------------
	
	override def caretBlinkFrequency = labelSettings.caretBlinkFrequency
	override def customCaretColorPointer = labelSettings.customCaretColorPointer
	override def customDrawers = labelSettings.customDrawers
	override def drawsSelectionBackground = labelSettings.drawsSelectionBackground
	override def highlightColorPointer = labelSettings.highlightColorPointer
	override def focusListeners: Seq[FocusListener] = labelSettings.focusListeners
	
	override def withFocusListeners(listeners: Seq[FocusListener]): Repr =
		mapLabelSettings { _.withFocusListeners(listeners) }
	override def withCaretBlinkFrequency(frequency: Duration) =
		withLabelSettings(labelSettings.withCaretBlinkFrequency(frequency))
	override def withCustomCaretColorPointer(p: Option[Changing[ColorRole]]) =
		withLabelSettings(labelSettings.withCustomCaretColorPointer(p))
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
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
                                              context: VariableTextContext,
                                              settings: EditableTextLabelSettings = EditableTextLabelSettings.default,
                                              drawsBackground: Boolean = false)
	extends EditableTextLabelSettingsWrapper[ContextualEditableTextLabelFactory]
		with ContextualFactory[VariableTextContext, ContextualEditableTextLabelFactory]
		with VariableBackgroundRoleAssignableFactory[VariableTextContext, ContextualEditableTextLabelFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def withContext(context: VariableTextContext) = copy(context = context)
	override def withSettings(settings: EditableTextLabelSettings) =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContext: VariableTextContext,
	                                                     backgroundDrawer: CustomDrawer): ContextualEditableTextLabelFactory =
		copy(context = newContext, settings = settings.withCustomBackgroundDrawer(backgroundDrawer),
			drawsBackground = true)
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new editable label
	  * @param textPointer A pointer to this label's text (default = new empty pointer)
	  * @return a new label
	  */
	def apply(textPointer: EventfulPointer[String] = EventfulPointer("")) =
		new EditableTextLabel(parentHierarchy, context, settings, textPointer)
}

/**
  * Used for defining editable text label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class EditableTextLabelSetup(settings: EditableTextLabelSettings = EditableTextLabelSettings.default)
	extends EditableTextLabelSettingsWrapper[EditableTextLabelSetup]
		with FromContextComponentFactoryFactory[VariableTextContext, ContextualEditableTextLabelFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext) =
		ContextualEditableTextLabelFactory(hierarchy, context, settings)
	
	override def withSettings(settings: EditableTextLabelSettings) = copy(settings = settings)
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
class EditableTextLabel(parentHierarchy: ComponentHierarchy, context: VariableTextContext,
                        settings: EditableTextLabelSettings = EditableTextLabelSettings.default,
                        val textPointer: EventfulPointer[String] = EventfulPointer("")(ComponentCreationDefaults.componentLogger))
	extends AbstractSelectableTextLabel(parentHierarchy, context,
		textPointer.strongMap { _.noLanguageLocalizationSkipped },
		if (settings.allowsSelectionWhileDisabled) AlwaysTrue else settings.enabledPointer, settings.labelSettings,
		settings.enabledPointer)
{
	// ATTRIBUTES	-------------------------------
	
	private var focusLeaveConditions: Seq[String => (String, Boolean)] = Empty
	
	/**
	  * A flag that contains true while this label is receiving text input (having focus)
	  */
	val editingFlag = if (settings.allowsSelectionWhileDisabled) focusPointer && enabledPointer else interactiveFlag
	
	
	// INITIAL CODE	-------------------------------
	
	setup()
	addHierarchyListener { isAttached =>
		if (isAttached)
			KeyboardEvents ++= Pair(_KeyTypedListener, KeyPressListener)
		else
			KeyboardEvents --= Pair(_KeyTypedListener, KeyPressListener)
	}
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Pointer that contains true while this lable is enabled
	  */
	def enabledPointer = settings.enabledPointer
	/**
	  * @return Whether this label is currently enabled
	  */
	def enabled = enabledPointer.value
	
	def text = textPointer.value
	def text_=(newText: String) = textPointer.value = newText
	
	private def _text = textPointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
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
	
	private object _KeyTypedListener extends KeyTypedListener
	{
		// ATTRIBUTES   ----------------------
		
		override val keyTypedEventFilter: Filter[KeyTypedEvent] = {
			// Ignores certain characters
			val canDisplayFilter = KeyTypedEvent.filter { e => font.toAwt.canDisplay(e.typedChar) }
			val notTabFilter = !KeyEvent.filter(Tab)
			val deleteFilter = KeyEvent.filter(BackSpace, Delete)
			
			// Only accepts characters accepted by the content filter
			val exclusiveFilter = settings.inputFilter match {
				case Some(filter) =>
					(canDisplayFilter && notTabFilter) && KeyTypedEvent.filter { e => filter(e.typedChar.toString) }
				case None => canDisplayFilter && notTabFilter
			}
			deleteFilter || exclusiveFilter
		}
		
		
		// IMPLEMENTED  ---------------------------
		
		override def handleCondition: Flag = editingFlag
		
		override def onKeyTyped(event: KeyTypedEvent): Unit = {
			// Removes a character on backspace / delete
			if (event.index == BackSpace.index) {
				if (selectedRange.nonEmpty)
					removeSelectedText()
				else if (caretIndex > 0) {
					removeAt(caretIndex - 1)
					caretIndex -= 1
				}
			}
			else if (event.index == Delete.index) {
				if (selectedRange.nonEmpty)
					removeSelectedText()
				else
					removeAt(caretIndex)
			}
			else
				insertToCaret(event.typedChar.toString)
		}
	}
	
	private object KeyPressListener extends KeyStateListener with ClipboardOwner
	{
		// ATTRIBUTES   --------------------------
		
		override val keyStateEventFilter: KeyStateEventFilter = KeyStateEvent.filter.pressed
		
		
		// COMPUTED	------------------------------
		
		// NB: May throw
		private def clipBoard = Toolkit.getDefaultToolkit.getSystemClipboard
		
		
		// IMPLEMENTED	--------------------------
		
		override def handleCondition: Flag = editingFlag
		
		override def onKeyState(event: KeyStateEvent) = {
			if (event.keyboardState(Control)) {
				if (event.concernsChar('V'))
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
				else if (event.concernsChar('X'))
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
		
		// Called when clipboard contents are lost. Ignores this event
		override def lostOwnership(clipboard: Clipboard, contents: Transferable) = ()
	}
}

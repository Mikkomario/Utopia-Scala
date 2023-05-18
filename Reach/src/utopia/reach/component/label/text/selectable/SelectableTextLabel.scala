package utopia.reach.component.label.text.selectable

import utopia.firmament.context.{ComponentCreationDefaults, TextContext}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.{FromContextFactory, TextContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.focus.FocusListener

import scala.concurrent.duration.Duration

object SelectableTextLabel extends Cff[SelectableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new SelectableTextLabelFactory(hierarchy)
}

class SelectableTextLabelFactory(hierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualSelectableTextLabelFactory]
{
	override def withContext(context: TextContext) = ContextualSelectableTextLabelFactory(this, context)
	
	/**
	  * Creates a new text label with text selection enabled
	  * @param actorHandler Actor handler that is used for blinking the caret
	  * @param textPointer A pointer to the displayed text
	  * @param stylePointer A pointer to the style the text is displayed with
	  * @param selectedTextColorPointer A pointer to the color to use on selected text (default = always black)
	  * @param selectionBackgroundColorPointer A pointer that contains the
	  *                                        current selection area background color to use (if present,
	  *                                        default = always none)
	  * @param caretColorPointer A pointer that determines caret color (default = always black)
	  * @param caretWidth Caret width in pixels (default = 1 px)
	  * @param caretBlinkFrequency How often caret visibility changes (default = global default)
	  * @param customDrawers Custom drawers to assign to this label (default = empty)
	  * @param focusListeners Focus listeners to assign to this label (default = empty)
	  * @param allowTextShrink Whether text should be allowed to be shrunk
	  *                        to conserve space when necessary (default = false)
	  * @return A new selectable text label
	  */
	def apply(actorHandler: ActorHandler, textPointer: Changing[LocalizedString],
	          stylePointer: Changing[TextDrawContext],
	          selectedTextColorPointer: Changing[Color] = Fixed(Color.textBlack),
	          selectionBackgroundColorPointer: Changing[Option[Color]] = Fixed(None),
	          caretColorPointer: Changing[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
	          caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
	          customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector(),
	          allowTextShrink: Boolean = false) =
		new SelectableTextLabel(hierarchy, actorHandler, textPointer, stylePointer, selectedTextColorPointer,
			selectionBackgroundColorPointer, caretColorPointer, caretWidth, caretBlinkFrequency, customDrawers,
			focusListeners, allowTextShrink)
	
	/**
	  * Creates a new selectable text label with fixed style settings
	  * @param actorHandler Actor handler that is used for blinking the caret
	  * @param textPointer A pointer to the displayed text
	  * @param font Font used when drawing text
	  * @param textColor Color used when drawing non-selected text (default = black)
	  * @param selectedTextColor Color used when drawing selected text (default = black)
	  * @param selectionBackgroundColor Selected area background color (optional)
	  * @param caretColor Color used when drawing the caret (default = black)
	  * @param caretWidth Width of the caret in pixels (default = 1 px)
	  * @param insets Insets placed around the text (default = any, preferring 0)
	  * @param betweenLinesMargin Margin placed between text when there are multiple lines (default = 0 px)
	  * @param alignment Text alignment to use (default = Left)
	  * @param caretBlinkFrequency How often caret visibility changes (default = global default)
	  * @param customDrawers Custom drawers to assign to this label (default = empty)
	  * @param focusListeners Focus listeners to assign to this label (default = empty)
	  * @param allowLineBreaks Whether line breaks should be allowed within the text (default = true)
	  * @param allowTextShrink Whether text should be allowed to be shrunk
	  *                        to conserve space when necessary (default = false)
	  * @return A new selectable text label
	  */
	def withStaticStyle(actorHandler: ActorHandler, textPointer: Changing[LocalizedString], font: Font,
	                    textColor: Color = Color.textBlack, selectedTextColor: Color = Color.textBlack,
	                    selectionBackgroundColor: Option[Color] = None, caretColor: Color = Color.textBlack,
	                    caretWidth: Double = 1.0, insets: StackInsets = StackInsets.any,
	                    betweenLinesMargin: Double = 0.0, alignment: Alignment = Alignment.Left,
	                    caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
	                    customDrawers: Vector[CustomDrawer], focusListeners: Seq[FocusListener],
	                    allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		apply(actorHandler, textPointer,
			Fixed(TextDrawContext(font, textColor, alignment, insets, betweenLinesMargin, allowLineBreaks)),
			Fixed(selectedTextColor), Fixed(selectionBackgroundColor), Fixed(caretColor), caretWidth,
			caretBlinkFrequency, customDrawers, focusListeners, allowTextShrink)
	
	/**
	  * Creates a new selectable text label with fixed content and style
	  * @param actorHandler Actor handler that is used for blinking the caret
	  * @param text Text to display on this label
	  * @param font Font used when drawing text
	  * @param textColor Color used when drawing non-selected text (default = black)
	  * @param selectedTextColor Color used when drawing selected text (default = black)
	  * @param selectionBackgroundColor Selected area background color (optional)
	  * @param caretColor Color used when drawing the caret (default = black)
	  * @param caretWidth Width of the caret in pixels (default = 1 px)
	  * @param insets Insets placed around the text (default = any, preferring 0)
	  * @param betweenLinesMargin Margin placed between text when there are multiple lines (default = 0 px)
	  * @param alignment Text alignment to use (default = Left)
	  * @param caretBlinkFrequency How often caret visibility changes (default = global default)
	  * @param customDrawers Custom drawers to assign to this label (default = empty)
	  * @param focusListeners Focus listeners to assign to this label (default = empty)
	  * @param allowLineBreaks Whether line breaks should be allowed within the text (default = true)
	  * @param allowTextShrink Whether text should be allowed to be shrunk
	  *                        to conserve space when necessary (default = false)
	  * @return A new selectable text label
	  */
	def static(actorHandler: ActorHandler, text: LocalizedString, font: Font, textColor: Color = Color.textBlack,
	           selectedTextColor: Color = Color.textBlack, selectionBackgroundColor: Option[Color] = None,
	           caretColor: Color = Color.textBlack, caretWidth: Double = 1.0, insets: StackInsets = StackInsets.any,
	           betweenLinesMargin: Double = 0.0, alignment: Alignment = Alignment.Left,
	           caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
	           customDrawers: Vector[CustomDrawer], focusListeners: Seq[FocusListener],
	           allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		withStaticStyle(actorHandler, Fixed(text), font, textColor, selectedTextColor, selectionBackgroundColor,
			caretColor, caretWidth, insets, betweenLinesMargin, alignment, caretBlinkFrequency, customDrawers,
			focusListeners, allowLineBreaks, allowTextShrink)
}

case class ContextualSelectableTextLabelFactory(factory: SelectableTextLabelFactory, context: TextContext,
                                                highlightColorPointer: Changing[ColorRole] = Fixed(Secondary),
                                                customDrawers: Vector[CustomDrawer] = Vector(),
                                                focusListeners: Vector[FocusListener] = Vector(),
                                                caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
                                                customStylePointer: Option[Changing[TextDrawContext]] = None,
                                                customSelectedTextColorPointer: Option[Changing[Color]] = None,
                                                customSelectionBackgroundColorPointer: Option[Changing[Option[Color]]] = None,
                                                customCaretColorPointer: Option[Changing[Color]] = None)
	extends TextContextualFactory[ContextualSelectableTextLabelFactory]
{
	// COMPUTED -------------------------------
	
	private def stylePointer =
		customStylePointer.getOrElse { Fixed(TextDrawContext.contextual(context)) }
	
	/**
	  * @return Copy of this factory that doesn't draw a background for selected text
	  */
	def withoutSelectionBackground = withSelectionBackgroundPointer(Fixed(None))
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: ContextualSelectableTextLabelFactory = this
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param p A pointer that contains the highlight color role to use
	  * @return Copy of this factory that uses the specified pointer
	  */
	def withHighlightColorPointer(p: Changing[ColorRole]) =
		copy(highlightColorPointer = p)
	/**
	  * @param c Highlighting color to use for selection
	  * @return Copy of this factory with that highlighting color in place
	  */
	def withHighlightColor(c: ColorRole) = withHighlightColorPointer(Fixed(c))
	
	/**
	  * @param listeners Focus listeners to use in this component
	  * @return Copy of this factory that assigns the specified focus listeners (only)
	  */
	def withFocusListeners(listeners: Vector[FocusListener]) =
		copy(focusListeners = listeners)
	/**
	  * @param listener A focus listener to assign to created components
	  * @return Copy of this factory with the specified focus listener appended
	  */
	def withFocusListener(listener: FocusListener) =
		withFocusListeners(focusListeners :+ listener)
	
	/**
	  * @param interval Interval between caret visibility changes
	  * @return Copy of this factory with specified blink frequency
	  */
	def withCaretBlinkFrequency(interval: Duration) = copy(caretBlinkFrequency = interval)
	
	/**
	  * @param p A pointer that overrides normal text draw style
	  * @return Copy of this factory that uses the specified text style pointer
	  */
	def withStylePointer(p: Changing[TextDrawContext]) =
		copy(customStylePointer = Some(p))
	def withStyle(s: TextDrawContext) = withStylePointer(Fixed(s))
	def mapStyle(f: TextDrawContext => TextDrawContext) =
		withStylePointer(stylePointer.map(f))
	
	/**
	  * @param p Pointer that determines (overrides) the color of selected text
	  * @return Copy of this factory that uses the specified pointer
	  */
	def withSelectedTextColorPointer(p: Changing[Color]) =
		copy(customSelectedTextColorPointer = Some(p))
	/**
	  * @param c Color to use for selected text
	  * @return Copy of this factory with the specified selected text color
	  */
	def withSelectedTextColor(c: Color) = withSelectedTextColorPointer(Fixed(c))
	
	/**
	  * @param p A pointer that determines the (text) selection background color,
	  *          containing None while there shall be no background drawn.
	  * @return A copy of this factory that uses the specified pointer
	  */
	def withSelectionBackgroundPointer(p: Changing[Option[Color]]) =
		copy(customSelectionBackgroundColorPointer = Some(p))
	/**
	  * @param c Background color for selected text
	  * @return Copy of this factory that uses the specified background color
	  */
	def withSelectionBackground(c: Color) =
		withSelectionBackgroundPointer(Fixed(Some(c)))
	
	/**
	  * @param p Pointer that determines the caret color to use
	  * @return Copy of this factory that uses the specified pointer
	  */
	def withCaretColorPointer(p: Changing[Color]) =
		copy(customCaretColorPointer = Some(p))
	/**
	  * @param c Color for the caret
	  * @return Copy of this factory with the specified caret color
	  */
	def withCaretColor(c: Color) = withCaretColorPointer(Fixed(c))
	
	/**
	  * Creates a new selectable text label
	  * @param textPointer Pointer to the displayed text content
	  * @return A new selectable text label
	  */
	def apply(textPointer: Changing[LocalizedString]) = {
		// Uses custom selection background, if specified
		val selectionBgPointer = customSelectionBackgroundColorPointer.getOrElse {
			// By default, highlights using the specified highlight color
			highlightColorPointer.map { c => Some(context.color.light(c)) }
		}
		// Picks the selected text color based on the selection background
		val selectedTextColorPointer = selectionBgPointer.mergeWith(highlightColorPointer)  { (bg, c) =>
			bg match {
				// Case: Selection background is colored => Uses black or white text
				case Some(bg) => bg.shade.defaultTextColor
				// Case: No selection background is used => Uses colored text
				case None => context.color(c)
			}
		}
		// Determines the caret color based on selection background
		val caretColorPointer = customCaretColorPointer.getOrElse {
			selectionBgPointer.mergeWith(highlightColorPointer) { (bg, c) =>
				bg match {
					// Case: Colored selection background => Uses a color that may be distinguished from the background
					case Some(bg) => context.color.differentFrom(c, bg)
					case None => context.color(c)
				}
			}
		}
		factory(context.actorHandler, textPointer, stylePointer, selectedTextColorPointer, selectionBgPointer,
			caretColorPointer, (context.margins.verySmall * 0.66) max 1.0, caretBlinkFrequency, customDrawers,
			focusListeners, context.allowTextShrink)
	}
}

/**
  * A text label which allows the user to select text from it with keyboard or mouse
  * @author Mikko Hilpinen
  * @since 14.5.2021, v0.3
  */
class SelectableTextLabel(parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler,
                          val textPointer: Changing[LocalizedString], stylePointer: Changing[TextDrawContext],
                          selectedTextColorPointer: Changing[Color] = Fixed(Color.textBlack),
                          selectionBackgroundColorPointer: Changing[Option[Color]] = Fixed(None),
                          caretColorPointer: Changing[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
                          caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
                          additionalCustomDrawers: Vector[CustomDrawer], additionalFocusListeners: Seq[FocusListener],
                          allowTextShrink: Boolean = false)
	extends AbstractSelectableTextLabel(parentHierarchy, actorHandler, textPointer,
		stylePointer, selectedTextColorPointer, selectionBackgroundColorPointer, caretColorPointer, caretWidth,
		caretBlinkFrequency, allowTextShrink)
{
	// ATTRIBUTES   --------------------------------
	
	override val customDrawers = mainDrawer +: additionalCustomDrawers
	override val focusListeners = FocusHandler +: additionalFocusListeners
	
	
	// COMPUTED ------------------------------
	
	def text = textPointer.value
	
	
	// INITIAL CODE --------------------------------
	
	setup()
	
	
	// IMPLEMENTED  --------------------------------
	
	override def selectable = text.nonEmpty
	
	override def allowsFocusLeave = true
}

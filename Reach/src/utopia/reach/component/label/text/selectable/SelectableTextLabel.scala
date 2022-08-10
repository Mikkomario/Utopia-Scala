package utopia.reach.component.label.text.selectable

import utopia.flow.event.{ChangingLike, Fixed}
import utopia.paradigm.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.focus.FocusListener
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.ColorShade.Light
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.localization.LocalizedString
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.Duration

object SelectableTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike,
	SelectableTextLabelFactory, ContextualSelectableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new SelectableTextLabelFactory(hierarchy)
}

class SelectableTextLabelFactory(hierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualSelectableTextLabelFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualSelectableTextLabelFactory(this, context)
	
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
	def apply(actorHandler: ActorHandler, textPointer: ChangingLike[LocalizedString],
	          stylePointer: ChangingLike[TextDrawContext],
	          selectedTextColorPointer: ChangingLike[Color] = Fixed(Color.textBlack),
	          selectionBackgroundColorPointer: ChangingLike[Option[Color]] = Fixed(None),
	          caretColorPointer: ChangingLike[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
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
	def withStaticStyle(actorHandler: ActorHandler, textPointer: ChangingLike[LocalizedString], font: Font,
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

case class ContextualSelectableTextLabelFactory[+N <: TextContextLike](factory: SelectableTextLabelFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualSelectableTextLabelFactory]
{
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new selectable text label
	  * @param textPointer Pointer to the displayed text content
	  * @param caretBlinkFrequency How often caret visibility changes (default = global default)
	  * @param customDrawers Custom drawers to assign to this label (default = empty)
	  * @param focusListeners Focus listeners to assign to this label (default = empty)
	  * @return A new selectable text label
	  */
	def apply(textPointer: ChangingLike[LocalizedString],
	          caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
	          customDrawers: Vector[CustomDrawer], focusListeners: Seq[FocusListener]) =
	{
		val selectionBackground = context.color(Secondary, Light)
		val caretColor = context.colorScheme.secondary.bestAgainst(
			Vector(context.containerBackground, selectionBackground))
		factory(context.actorHandler, textPointer, Fixed(TextDrawContext.contextual(context)),
			Fixed(selectionBackground.defaultTextColor), Fixed(Some(selectionBackground)), Fixed(caretColor),
			(context.margins.verySmall * 0.66) max 1.0, caretBlinkFrequency, customDrawers, focusListeners,
			context.allowTextShrink)
	}
}

/**
  * A text label which allows the user to select text from it with keyboard or mouse
  * @author Mikko Hilpinen
  * @since 14.5.2021, v0.3
  */
class SelectableTextLabel(parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler,
                          val textPointer: ChangingLike[LocalizedString], stylePointer: ChangingLike[TextDrawContext],
                          selectedTextColorPointer: ChangingLike[Color] = Fixed(Color.textBlack),
                          selectionBackgroundColorPointer: ChangingLike[Option[Color]] = Fixed(None),
                          caretColorPointer: ChangingLike[Color] = Fixed(Color.textBlack), caretWidth: Double = 1.0,
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

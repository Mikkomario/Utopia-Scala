package utopia.reach.component.button.text

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.{GuiElementStatus, TextDrawContext}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.button.{ButtonSettings, ButtonSettingsWrapper}
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor

/**
  * Common trait for factories that are used for constructing text buttons
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait TextButtonFactoryLike[+Repr] extends ButtonSettingsWrapper[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The component hierarchy, to which created text buttons will be attached
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new text button
	  * @param text               Text displayed on this button
	  * @param font               Font used when drawing the text
	  * @param color              Button background color
	  * @param textColor          Button text color (default = standard black)
	  * @param alignment          Text alignment (default = Center)
	  * @param textInsets         Insets placed around the text (default = any, preferring 0)
	  * @param borderWidth        Width of the border on this button (default = 0 = no border)
	  * @param betweenLinesMargin Margin placed between horizontal text lines in case there are multiple (default = 0.0)
	  * @param allowLineBreaks    Whether line breaks in the drawn text should be respected and applied (default = true)
	  * @param allowTextShrink    Whether text size should be allowed to decrease to conserve space (default = false)
	  * @param action             Action performed each time this button is triggered (call by name)
	  * @return A new text button
	  */
	protected def _apply(text: LocalizedString, font: Font, color: Color, textColor: Color, alignment: Alignment,
	                     textInsets: StackInsets, borderWidth: Double, betweenLinesMargin: Double,
	                     allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false)
	                    (action: => Unit) =
		new TextButton(parentHierarchy, text, TextDrawContext(font, textColor, alignment, textInsets + borderWidth,
			betweenLinesMargin, allowLineBreaks), color, settings, borderWidth, allowTextShrink)(action)
}

/**
  * Factory class used for constructing text buttons using contextual component creation information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ContextualTextButtonFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                       settings: ButtonSettings = ButtonSettings.default)
	extends TextButtonFactoryLike[ContextualTextButtonFactory]
		with TextContextualFactory[ContextualTextButtonFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def self: ContextualTextButtonFactory = this
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
	override def withSettings(settings: ButtonSettings) = copy(settings = settings)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new text button
	  * @param text           The text displayed on this button
	  * @param action         Action performed each time this button is triggered (call by name)
	  * @return A new text button
	  */
	def apply(text: LocalizedString)(action: => Unit) =
		_apply(text, context.font, context.background, context.textColor, context.textAlignment,
			context.textInsets, context.buttonBorderWidth, context.betweenLinesMargin.optimal,
			context.allowLineBreaks, context.allowTextShrink)(action)
}

/**
  * Factory class that is used for constructing text buttons without using contextual information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class TextButtonFactory(parentHierarchy: ComponentHierarchy,
                             settings: ButtonSettings = ButtonSettings.default)
	extends TextButtonFactoryLike[TextButtonFactory]
		with FromContextFactory[TextContext, ContextualTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(context: TextContext) =
		ContextualTextButtonFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: ButtonSettings) = copy(settings = settings)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new text button
	  * @param text Text displayed on this button
	  * @param font Font used when drawing the text
	  * @param color Button background color
	  * @param textColor Button text color (default = standard black)
	  * @param alignment Text alignment (default = Center)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param borderWidth Width of the border on this button (default = 0 = no border)
	  * @param betweenLinesMargin Margin placed between horizontal text lines in case there are multiple (default = 0.0)
	  * @param allowLineBreaks Whether line breaks in the drawn text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text size should be allowed to decrease to conserve space (default = false)
	  * @param action Action performed each time this button is triggered (call by name)
	  * @return A new text button
	  */
	def apply(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
	          alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
	          borderWidth: Double = 0.0, betweenLinesMargin: Double = 0.0,
	          allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false)
	         (action: => Unit) =
		_apply(text, font, color, textColor, alignment, textInsets, borderWidth,
			betweenLinesMargin, allowLineBreaks, allowTextShrink)(action)
}

/**
  * Used for defining text button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class TextButtonSetup(settings: ButtonSettings = ButtonSettings.default)
	extends ButtonSettingsWrapper[TextButtonSetup] with ComponentFactoryFactory[TextButtonFactory]
		with FromContextComponentFactoryFactory[TextContext, ContextualTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = TextButtonFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualTextButtonFactory(hierarchy, context, settings)
	
	override def withSettings(settings: ButtonSettings) = copy(settings = settings)
}

object TextButton extends TextButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ButtonSettings) = withSettings(settings)
}

/**
  * An immutable button that only draws text
  * @author Mikko Hilpinen
  * @since 24.10.2020, v0.1
  */
class TextButton(parentHierarchy: ComponentHierarchy, text: LocalizedString, textDrawContext: TextDrawContext,
                 color: Color, settings: ButtonSettings = ButtonSettings.default, borderWidth: Double = 0.0,
                 allowTextShrink: Boolean = false)(action: => Unit)
	extends ButtonLike with ReachComponentWrapper
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	override val statePointer = baseStatePointer
		.mergeWith(settings.enabledPointer) { (base, enabled) => base + (Disabled -> !enabled) }
	
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
	override val focusId = hashCode()
	override protected val wrapped = new TextLabel(parentHierarchy, text, textDrawContext,
		ButtonBackgroundViewDrawer(Fixed(color), statePointer, Fixed(borderWidth)) +: settings.customDrawers, allowTextShrink)
	
	
	// INITIAL CODE	-----------------------------
	
	setup(baseStatePointer, settings.hotKeys)
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}

package utopia.reach.component.button.text

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsWrapper}
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.cursor.Cursor

/**
  * Common trait for factories that are used for constructing text buttons
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait TextButtonFactoryLike[+Repr]
	extends ButtonSettingsWrapper[Repr] with CustomDrawableFactory[Repr] with PartOfComponentHierarchy
{
	// OTHER	--------------------
	
	protected def _apply(text: LocalizedString, font: Font, color: Color, textColor: Color, alignment: Alignment,
	                     textInsets: StackInsets, borderWidth: Double, lineSplitThreshold: Option[Double],
	                     betweenLinesMargin: Double, allowLineBreaks: Boolean, allowTextShrink: Boolean)
	                    (action: => Unit) =
		new TextButton(parentHierarchy, text, TextDrawContext(font, textColor, alignment, textInsets + borderWidth,
			lineSplitThreshold, betweenLinesMargin, allowLineBreaks), color, settings, borderWidth, customDrawers,
			allowTextShrink)(action)
}

/**
  * Factory class used for constructing text buttons using contextual component creation information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ContextualTextButtonFactory(parentHierarchy: ComponentHierarchy, context: StaticTextContext,
                                       settings: ButtonSettings = ButtonSettings.default,
                                       customDrawers: Seq[CustomDrawer] = Empty)
	extends TextButtonFactoryLike[ContextualTextButtonFactory]
		with TextContextualFactory[ContextualTextButtonFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def self: ContextualTextButtonFactory = this
	
	override def withContext(newContext: StaticTextContext) = copy(context = newContext)
	override def withSettings(settings: ButtonSettings) = copy(settings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualTextButtonFactory =
		copy(customDrawers = drawers)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new text button
	  * @param text           The text displayed on this button
	  * @param action         Action performed each time this button is triggered (call by name)
	  * @return A new text button
	  */
	def apply(text: LocalizedString)(action: => Unit) =
		_apply(text, context.font, context.background, context.textColor, context.textAlignment,
			context.textInsets, context.buttonBorderWidth, context.lineSplitThreshold,
			context.betweenLinesMargin.optimal, context.allowLineBreaks, context.allowTextShrink)(action)
}

/**
  * Factory class that is used for constructing text buttons without using contextual information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class TextButtonFactory(parentHierarchy: ComponentHierarchy,
                             settings: ButtonSettings = ButtonSettings.default,
                             customDrawers: Seq[CustomDrawer] = Empty)
	extends TextButtonFactoryLike[TextButtonFactory]
		with FromContextFactory[StaticTextContext, ContextualTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(context: StaticTextContext) =
		ContextualTextButtonFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: ButtonSettings) = copy(settings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): TextButtonFactory = copy(customDrawers = drawers)
	
	
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
		_apply(text, font, color, textColor, alignment, textInsets, borderWidth, None,
			betweenLinesMargin, allowLineBreaks, allowTextShrink)(action)
}

/**
  * Used for defining text button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class TextButtonSetup(settings: ButtonSettings = ButtonSettings.default)
	extends ButtonSettingsWrapper[TextButtonSetup] with ComponentFactoryFactory[TextButtonFactory]
		with FromContextComponentFactoryFactory[StaticTextContext, ContextualTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = TextButtonFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: StaticTextContext) =
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
                 customDrawers: Seq[CustomDrawer] = Empty,
                 allowTextShrink: Boolean = false)(action: => Unit)
	extends AbstractButton(settings) with ReachComponentWrapper
{
	// ATTRIBUTES	-----------------------------
	
	override protected val wrapped = new TextLabel(parentHierarchy, text, textDrawContext,
		ButtonBackgroundViewDrawer(Fixed(color), statePointer, Fixed(borderWidth)) +: customDrawers, allowTextShrink)
	
	
	// INITIAL CODE	-----------------------------
	
	setup()
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}

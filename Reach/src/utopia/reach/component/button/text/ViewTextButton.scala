package utopia.reach.component.button.text

import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsWrapper}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.cursor.Cursor

object ViewTextButton extends Cff[ViewTextButtonFactory]
	with FromContextComponentFactoryFactory[VariableTextContext, ContextualViewTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewTextButtonFactory(hierarchy)
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext): ContextualViewTextButtonFactory =
		ContextualViewTextButtonFactory(hierarchy, context)
}

case class ViewTextButtonFactory(hierarchy: ComponentHierarchy)
	extends FromContextFactory[VariableTextContext, ContextualViewTextButtonFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED	-----------------------------
	
	override def withContext(c: VariableTextContext): ContextualViewTextButtonFactory =
		ContextualViewTextButtonFactory(hierarchy, c)
}

case class ContextualViewTextButtonFactory(hierarchy: ComponentHierarchy, context: VariableTextContext,
                                           settings: ButtonSettings = ButtonSettings.default,
                                           customDrawers: Seq[CustomDrawer] = Empty)
	extends ContextualFactory[VariableTextContext, ContextualViewTextButtonFactory]
		with ButtonSettingsWrapper[ContextualViewTextButtonFactory]
		with CustomDrawableFactory[ContextualViewTextButtonFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED	------------------------------
	
	override def withContext(context: VariableTextContext): ContextualViewTextButtonFactory = copy(context = context)
	override def withSettings(settings: ButtonSettings): ContextualViewTextButtonFactory = copy(settings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualViewTextButtonFactory =
		copy(customDrawers = drawers)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new button
	  * @param contentPointer           Pointer that contains the displayed button content
	  * @param displayFunction          A function for converting the displayed value to a localized string (default = toString)
	  * @param action                   The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def apply[A](contentPointer: Changing[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	            (action: A => Unit) =
		new ViewTextButton[A](hierarchy, context, contentPointer, settings, displayFunction,
			customDrawers)(action)
	
	/**
	  * @param textPointer Pointer that contains the text to display on this button
	  * @param action Action run when this button is triggered
	  * @return A new button
	  */
	def text(textPointer: Changing[LocalizedString])(action: => Unit) =
		apply(textPointer, DisplayFunction.identity) { _ => action }
	/**
	  * @param text Text to display on this button
	  * @param action Action to perform when this button is triggered
	  * @return A new button
	  */
	def staticText(text: LocalizedString)(action: => Unit) =
		this.text(Fixed(text))(action)
	
	/**
	  * Creates a new button
	  * @param text                     Text displayed on this string
	  * @param action                   The action performed when this button is pressed
	  * @return A new button
	  */
	@deprecated("Please use .staticText(LocalizedString) instead", "v1.3")
	def withStaticText(text: LocalizedString)(action: => Unit) =
		staticText(text)(action)
}

/**
  * A button that matches the states of various pointers (not offering any mutable interface itself)
  * @author Mikko Hilpinen
  * @since 26.10.2020, v0.1
  */
class ViewTextButton[A](override val hierarchy: ComponentHierarchy, context: VariableTextContext,
                        contentPointer: Changing[A], settings: ButtonSettings = ButtonSettings.default,
                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                        additionalDrawers: Seq[CustomDrawer] = Empty)
                       (action: A => Unit)
	extends AbstractButton(settings) with ReachComponentWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val borderWidth = context.buttonBorderWidth.toDouble
	private val appliedContext = {
		// Adds space for the button borders to the text insets
		if (borderWidth > 0)
			context.mapTextInsets { _ + borderWidth }
		else
			context
	}
	
	override protected val wrapped = ViewTextLabel.withContext(hierarchy, appliedContext)
		.withCustomDrawers(
			ButtonBackgroundViewDrawer(colorPointer, statePointer, Fixed(borderWidth)) +: additionalDrawers)
		.apply[A](contentPointer, displayFunction)
	
	
	// INITIAL CODE	---------------------------------
	
	setup()
	colorPointer.addListenerWhile(linkedFlag) { _ => repaint() }
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return This button's current background color
	  */
	def color = colorPointer.value
	
	private def colorPointer = context.backgroundPointer
	
	
	// IMPLEMENTED	---------------------------------
	
	override protected def trigger() = action(contentPointer.value)
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}

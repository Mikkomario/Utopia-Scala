package utopia.reach.component.button.text

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsWrapper}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.factory.{FromVariableContextComponentFactoryFactory, FromVariableContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.cursor.Cursor

object ViewTextButton extends Cff[ViewTextButtonFactory]
	with FromVariableContextComponentFactoryFactory[TextContext, ContextualViewTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewTextButtonFactory(hierarchy)
	
	override def withContextPointer(hierarchy: ComponentHierarchy, context: Changing[TextContext]): ContextualViewTextButtonFactory =
		ContextualViewTextButtonFactory(hierarchy, context)
}

class ViewTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends FromVariableContextFactory[TextContext, ContextualViewTextButtonFactory]
{
	// IMPLEMENTED	-----------------------------
	
	override def withContextPointer(p: Changing[TextContext]): ContextualViewTextButtonFactory =
		ContextualViewTextButtonFactory(parentHierarchy, p)
}

case class ContextualViewTextButtonFactory(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                                           settings: ButtonSettings = ButtonSettings.default,
                                           customDrawers: Vector[CustomDrawer] = Vector.empty)
	extends VariableContextualFactory[TextContext, ContextualViewTextButtonFactory]
		with ButtonSettingsWrapper[ContextualViewTextButtonFactory]
		with CustomDrawableFactory[ContextualViewTextButtonFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED	------------------------------
	
	override def withContextPointer(p: Changing[TextContext]): ContextualViewTextButtonFactory =
		copy(contextPointer = p)
	override def withSettings(settings: ButtonSettings): ContextualViewTextButtonFactory = copy(settings = settings)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualViewTextButtonFactory =
		copy(customDrawers = drawers)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new button that changes its color based on a pointer value
	  * @param contentPointer Pointer that contains the displayed button content
	  * @param colorPointer Pointer that contains this button's background color
	  * @param displayFunction A function for converting the displayed value to a localized string (default = toString)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	@deprecated("Please use other color functions together with .apply(...) instead", "v1.3")
	def withChangingColor[A](contentPointer: Changing[A], colorPointer: Changing[Color],
	                         displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	                        (action: A => Unit) =
		withContextPointer(contextPointer.mergeWith(colorPointer) { _.withBackground(_) })
			.apply(contentPointer, displayFunction)(action)
	
	/**
	  * Creates a new button that changes its color based on a pointer value
	  * @param contentPointer Pointer that contains the displayed button content
	  * @param rolePointer A pointer that contains this button's role
	  * @param displayFunction A function for converting the displayed value to a localized string (default = toString)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	@deprecated("Please use other role-related functions together with .apply(...) instead", "v1.3")
	def withChangingRole[A](contentPointer: Changing[A], rolePointer: Changing[ColorRole],
	                        displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	                       (action: A => Unit) =
		withContextPointer(contextPointer.mergeWith(rolePointer) { _.withBackground(_) })
			.apply(contentPointer, displayFunction)(action)
	
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
		new ViewTextButton[A](parentHierarchy, contextPointer, contentPointer, settings, displayFunction,
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
class ViewTextButton[A](parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                        contentPointer: Changing[A], settings: ButtonSettings = ButtonSettings.default,
                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                        additionalDrawers: Vector[CustomDrawer] = Vector.empty)
                       (action: A => Unit)
	extends AbstractButton(settings) with ReachComponentWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val colorPointer = contextPointer.mapWhile(parentHierarchy.linkPointer) { _.background }
	private val borderWidthPointer = contextPointer.mapWhile(parentHierarchy.linkPointer) { _.buttonBorderWidth.toDouble }
	private val appliedContextPointer = contextPointer.mapWhile(parentHierarchy.linkPointer) { c =>
		if (c.buttonBorderWidth > 0) c.mapTextInsets { _ + c.buttonBorderWidth } else c
	}
	
	override protected val wrapped = ViewTextLabel.withContextPointer(parentHierarchy, appliedContextPointer)
		.withCustomDrawers(ButtonBackgroundViewDrawer(colorPointer, statePointer, borderWidthPointer) +: additionalDrawers)
		.apply[A](contentPointer, displayFunction)
	
	
	// INITIAL CODE	---------------------------------
	
	setup()
	colorPointer.addContinuousListener { _ => repaint() }
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return This button's current background color
	  */
	def color = colorPointer.value
	
	
	// IMPLEMENTED	---------------------------------
	
	override protected def trigger() = action(contentPointer.value)
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}

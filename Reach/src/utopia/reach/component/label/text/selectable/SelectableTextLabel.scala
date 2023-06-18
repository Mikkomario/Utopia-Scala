package utopia.reach.component.label.text.selectable

import utopia.firmament.context.{ComponentCreationDefaults, TextContext}
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorRole
import utopia.reach.component.factory.contextual.{VariableBackgroundRoleAssignableFactory, VariableContextualFactory}
import utopia.reach.component.factory.{FocusListenableFactory, FromContextComponentFactoryFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.focus.FocusListener

import scala.concurrent.duration.Duration

/**
  * Common trait for selectable text label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait SelectableTextLabelSettingsLike[+Repr] extends CustomDrawableFactory[Repr] with FocusListenableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * A pointer that determines the color used when highlighting selected text
	  */
	def highlightColorPointer: Changing[ColorRole]
	/**
	  * A pointer that, if defined, determines the color of the caret when drawn
	  */
	def customCaretColorPointer: Option[Changing[ColorRole]]
	/**
	  * Interval between caret visibility changes
	  */
	def caretBlinkFrequency: Duration
	/**
	  * Whether selected area background should be highlighted.
	  * If false, only highlights selected text.
	  */
	def drawsSelectionBackground: Boolean
	
	/**
	  * A pointer that, if defined, determines the color of the caret when drawn
	  * @param p New custom caret color pointer to use.
	  *          A pointer that, if defined, determines the color of the caret when drawn
	  * @return Copy of this factory with the specified custom caret color pointer
	  */
	def withCustomCaretColorPointer(p: Option[Changing[ColorRole]]): Repr
	/**
	  * Interval between caret visibility changes
	  * @param frequency New caret blink frequency to use.
	  *                  Interval between caret visibility changes
	  * @return Copy of this factory with the specified caret blink frequency
	  */
	def withCaretBlinkFrequency(frequency: Duration): Repr
	/**
	  * Whether selected area background should be highlighted.
	  * If false, only highlights selected text.
	  * @param drawBackground New draws selection background to use.
	  *                       Whether selected area background should be highlighted.
	  *                       If false, only highlights selected text.
	  * @return Copy of this factory with the specified draws selection background
	  */
	def withDrawSelectionBackground(drawBackground: Boolean): Repr
	/**
	  * A pointer that determines the color used when highlighting selected text
	  * @param p New highlight color pointer to use.
	  *          A pointer that determines the color used when highlighting selected text
	  * @return Copy of this factory with the specified highlight color pointer
	  */
	def withHighlightColorPointer(p: Changing[ColorRole]): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Copy of this factory that doesn't draw a background for selected text
	  */
	def withoutSelectionBackground = withDrawSelectionBackground(false)
	
	
	// OTHER	--------------------
	
	def mapCaretBlinkFrequency(f: Duration => Duration) = withCaretBlinkFrequency(f(caretBlinkFrequency))
	def mapHighlightColorPointer(f: Changing[ColorRole] => Changing[ColorRole]) =
		withHighlightColorPointer(f(highlightColorPointer))
	
	/**
	  * @param c Highlighting color to use for selection
	  * @return Copy of this factory with that highlighting color in place
	  */
	def withHighlightColor(c: ColorRole) = withHighlightColorPointer(Fixed(c))
	
	/**
	  * @param p Pointer that determines the caret color to use
	  * @return Copy of this factory that uses the specified pointer
	  */
	def withCaretColorPointer(p: Changing[ColorRole]) =
		withCustomCaretColorPointer(Some(p))
	/**
	  * @param c Color for the caret
	  * @return Copy of this factory with the specified caret color
	  */
	def withCaretColor(c: ColorRole) = withCaretColorPointer(Fixed(c))
}

object SelectableTextLabelSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing selectable text labels
  * @param customDrawers            Custom drawers to assign to created components
  * @param focusListeners           Focus listeners to assign to created components
  * @param highlightColorPointer    A pointer that determines the color used when highlighting selected text
  * @param customCaretColorPointer  A pointer that, if defined, determines the color of the caret when drawn
  * @param caretBlinkFrequency      Interval between caret visibility changes
  * @param drawsSelectionBackground Whether selected area background should be highlighted.
  *                                 If false, only highlights selected text.
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class SelectableTextLabelSettings(customDrawers: Vector[CustomDrawer] = Vector.empty,
                                       focusListeners: Vector[FocusListener] = Vector.empty,
                                       highlightColorPointer: Changing[ColorRole] = Fixed(ColorRole.Secondary),
                                       customCaretColorPointer: Option[Changing[ColorRole]] = None,
                                       caretBlinkFrequency: Duration = ComponentCreationDefaults.caretBlinkFrequency,
                                       drawsSelectionBackground: Boolean = true)
	extends SelectableTextLabelSettingsLike[SelectableTextLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withFocusListeners(listeners: Vector[FocusListener]): SelectableTextLabelSettings =
		copy(focusListeners = listeners)
	override def withCaretBlinkFrequency(frequency: Duration) =
		copy(caretBlinkFrequency = frequency)
	override def withCustomCaretColorPointer(p: Option[Changing[ColorRole]]) =
		copy(customCaretColorPointer = p)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		copy(customDrawers = drawers)
	override def withDrawSelectionBackground(drawBackground: Boolean) =
		copy(drawsSelectionBackground = drawBackground)
	override def withHighlightColorPointer(p: Changing[ColorRole]) =
		copy(highlightColorPointer = p)
}

/**
  * Common trait for factories that wrap a selectable text label settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait SelectableTextLabelSettingsWrapper[+Repr] extends SelectableTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: SelectableTextLabelSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: SelectableTextLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def caretBlinkFrequency = settings.caretBlinkFrequency
	override def customCaretColorPointer = settings.customCaretColorPointer
	override def customDrawers = settings.customDrawers
	override def drawsSelectionBackground = settings.drawsSelectionBackground
	override def highlightColorPointer = settings.highlightColorPointer
	override protected def focusListeners: Vector[FocusListener] = settings.focusListeners
	
	override def withFocusListeners(listeners: Vector[FocusListener]): Repr =
		mapSettings { _.withFocusListeners(listeners) }
	override def withCaretBlinkFrequency(frequency: Duration) =
		mapSettings { _.withCaretBlinkFrequency(frequency) }
	override def withCustomCaretColorPointer(p: Option[Changing[ColorRole]]) =
		mapSettings { _.withCustomCaretColorPointer(p) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withDrawSelectionBackground(drawBackground: Boolean) =
		mapSettings { _.withDrawSelectionBackground(drawBackground) }
	override def withHighlightColorPointer(p: Changing[ColorRole]) =
		mapSettings { _.withHighlightColorPointer(p) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: SelectableTextLabelSettings => SelectableTextLabelSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing selectable text labels using contextual component creation information
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ContextualSelectableTextLabelFactory(parentHierarchy: ComponentHierarchy,
                                                contextPointer: Changing[TextContext],
                                                settings: SelectableTextLabelSettings = SelectableTextLabelSettings.default,
                                                drawsBackground: Boolean = false)
	extends SelectableTextLabelSettingsWrapper[ContextualSelectableTextLabelFactory]
		with VariableContextualFactory[TextContext, ContextualSelectableTextLabelFactory]
		with VariableBackgroundRoleAssignableFactory[TextContext, ContextualSelectableTextLabelFactory]
{
	// IMPLEMENTED  ---------------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: SelectableTextLabelSettings) =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContextPointer: Changing[TextContext],
	                                                     backgroundDrawer: CustomDrawer): ContextualSelectableTextLabelFactory =
		copy(contextPointer = newContextPointer, settings = settings.withCustomBackgroundDrawer(backgroundDrawer),
			drawsBackground = true)
	
	
	// OTHER    -------------------------------
	
	/**
	  * Creates a new selectable text label
	  * @param textPointer Pointer to the displayed text content
	  * @return A new selectable text label
	  */
	def apply(textPointer: Changing[LocalizedString]) = {
		val label = new SelectableTextLabel(parentHierarchy, contextPointer, textPointer, settings)
		contextPointer.addContinuousListener { e =>
			if (e.toPair.isAsymmetricBy { _.background })
				label.repaint()
		}
		label
	}
}

/**
  * Used for defining selectable text label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class SelectableTextLabelSetup(settings: SelectableTextLabelSettings = SelectableTextLabelSettings.default)
	extends SelectableTextLabelSettingsWrapper[SelectableTextLabelSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualSelectableTextLabelFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualSelectableTextLabelFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: SelectableTextLabelSettings) = copy(settings = settings)
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new selectable text label factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualSelectableTextLabelFactory(hierarchy, context, settings)
}

object SelectableTextLabel extends SelectableTextLabelSetup()
{
	// OTHER	--------------------
	
	def apply(settings: SelectableTextLabelSettings) = withSettings(settings)
}

/**
  * A text label which allows the user to select text from it with keyboard or mouse
  * @author Mikko Hilpinen
  * @since 14.5.2021, v0.3
  */
class SelectableTextLabel(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                          val textPointer: Changing[LocalizedString], settings: SelectableTextLabelSettings)
	extends AbstractSelectableTextLabel(parentHierarchy, contextPointer, textPointer, settings)
{
	// COMPUTED ------------------------------
	
	def text = textPointer.value
	
	
	// INITIAL CODE --------------------------------
	
	setup()
	
	
	// IMPLEMENTED  --------------------------------
	
	override def selectable = text.nonEmpty
	
	override def allowsFocusLeave = true
}

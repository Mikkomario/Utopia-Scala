package utopia.reach.component.input.check

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.color.VariableColorContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.stack.StackLength
import utopia.firmament.model.{HotKey, StandardSizeAdjustable}
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Priority.High
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.{Color, ColorRole, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

import scala.language.implicitConversions

/**
  * Common trait for radio button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait RadioButtonSettingsLike[+Repr] extends CustomDrawableFactory[Repr] with ButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings that affect the functionality of the created radio buttons
	  */
	def buttonSettings: ButtonSettings
	/**
	  * Color role used when highlighting the selected state in created radio buttons
	  */
	def selectedColorRole: ColorRole
	
	/**
	  * Settings that affect the functionality of the created radio buttons
	  * @param settings New button settings to use.
	  *                 Settings that affect the functionality of the created radio buttons
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	/**
	  * Color role used when highlighting the selected state in created radio buttons
	  * @param role New selected color role to use.
	  *             Color role used when highlighting the selected state in created radio buttons
	  * @return Copy of this factory with the specified selected color role
	  */
	def withSelectedColorRole(role: ColorRole): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def enabledFlag = buttonSettings.enabledFlag
	override def focusListeners = buttonSettings.focusListeners
	override def hotKeys = buttonSettings.hotKeys
	
	override def withEnabledFlag(p: Flag) = withButtonSettings(buttonSettings.withEnabledFlag(p))
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	override def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
}

object RadioButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
	
	
	// IMPLICIT ------------------------
	
	// Implicitly converts button settings to radio button settings
	implicit def wrap(buttonSettings: ButtonSettings): RadioButtonSettings = apply(buttonSettings = buttonSettings)
}
/**
  * Combined settings used when constructing radio buttons
  * @param customDrawers     Custom drawers to assign to created components
  * @param buttonSettings    Settings that affect the functionality of the created radio buttons
  * @param selectedColorRole Color role used when highlighting the selected state in created radio buttons
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class RadioButtonSettings(customDrawers: Seq[CustomDrawer] = Empty,
                               buttonSettings: ButtonSettings = ButtonSettings.default,
                               selectedColorRole: ColorRole = ColorRole.Secondary)
	extends RadioButtonSettingsLike[RadioButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withSelectedColorRole(role: ColorRole) = copy(selectedColorRole = role)
}

/**
  * Common trait for factories that wrap a radio button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait RadioButtonSettingsWrapper[+Repr] extends RadioButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: RadioButtonSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: RadioButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def customDrawers = settings.customDrawers
	override def selectedColorRole = settings.selectedColorRole
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withSelectedColorRole(role: ColorRole) = mapSettings { _.withSelectedColorRole(role) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: RadioButtonSettings => RadioButtonSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing radio buttons
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait RadioButtonFactoryLike[+Repr] extends RadioButtonSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	// OTHER    -------------------
	
	/**
	  * Creates a new radio button
	  * @param selectedValuePointer   A mutable pointer to the currently selected value
	  * @param value                  Value represented by this radio button
	  * @param backgroundColorPointer A pointer to the current background color
	  * @param diameter               The diameter (2 * radius) of this button
	  * @param hoverExtraRadius       Added radius for the hover effect
	  * @param ringWidth              Width of the outer ring in this button
	  * @param colorScheme            Color scheme used (implicit)
	  * @tparam A Type of selected value
	  * @return A new radio button
	  */
	protected def _apply[A](selectedValuePointer: EventfulPointer[A], value: A,
	                        backgroundColorPointer: Changing[Color], diameter: Double,
	                        hoverExtraRadius: Double, ringWidth: Double)
	                       (implicit colorScheme: ColorScheme) =
		new RadioButton[A](hierarchy, selectedValuePointer, value, backgroundColorPointer, diameter,
			hoverExtraRadius, ringWidth, (ringWidth * 1.25).round.toDouble, settings)
}

/**
  * Factory class used for constructing radio buttons using contextual component creation information
  * @param scaling Scaling modifier applied to the size of the created radio buttons
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class ContextualRadioButtonFactory(hierarchy: ComponentHierarchy,
                                        context: VariableColorContext,
                                        settings: RadioButtonSettings = RadioButtonSettings.default,
                                        scaling: Double = 1.0)
	extends RadioButtonFactoryLike[ContextualRadioButtonFactory]
		with ContextualFactory[VariableColorContext, ContextualRadioButtonFactory]
		with StandardSizeAdjustable[ContextualRadioButtonFactory]
{
	// IMPLEMENTED  --------------------------
	
	override def self: ContextualRadioButtonFactory = this
	override protected def relativeToStandardSize: Double = scaling
	
	override def withContext(context: VariableColorContext) = copy(context = context)
	override def withSettings(settings: RadioButtonSettings) = copy(settings = settings)
	
	override def *(mod: Double): ContextualRadioButtonFactory = copy(scaling = scaling * mod)
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new radio button
	  * @param selectedValuePointer   A mutable pointer to the currently selected value
	  * @param value                  Value represented by this radio button
	  * @tparam A Type of selected value
	  * @return A new radio button
	  */
	def apply[A](selectedValuePointer: EventfulPointer[A], value: A) = {
		// Uses a static size after creation
		val sizeMod = ComponentCreationDefaults.radioButtonScalingFactor * scaling
		_apply[A](selectedValuePointer, value, context.backgroundPointer,
			(context.margins.medium * 1.6 * sizeMod).round.toDouble,
			(context.margins.medium * 0.4 * sizeMod).round.toDouble,
			((context.margins.medium * 0.22 * sizeMod) max 1.0).round.toDouble)(context.colors)
	}
}

/**
  * Factory class that is used for constructing radio buttons without using contextual information
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class RadioButtonFactory(hierarchy: ComponentHierarchy,
                              settings: RadioButtonSettings = RadioButtonSettings.default)
	extends RadioButtonFactoryLike[RadioButtonFactory]
		with FromContextFactory[VariableColorContext, ContextualRadioButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(c: VariableColorContext) = ContextualRadioButtonFactory(hierarchy, c, settings)
	override def withSettings(settings: RadioButtonSettings) = copy(settings = settings)
	
	
	// OTHER    ----------------------------------------
	
	/**
	 * Creates a new radio button
	 * @param selectedValuePointer A mutable pointer to the currently selected value
	 * @param value Value represented by this radio button
	 * @param backgroundColorPointer A pointer to the current background color
	 * @param diameter The diameter (2 * radius) of this button
	 * @param hoverExtraRadius Added radius for the hover effect
	 * @param ringWidth Width of the outer ring in this button (default = 1 px)
	 * @param colorScheme Color scheme used (implicit)
	 * @tparam A Type of selected value
	 * @return A new radio button
	 */
	def apply[A](selectedValuePointer: EventfulPointer[A], value: A,
	             backgroundColorPointer: Changing[Color], diameter: Double,
	             hoverExtraRadius: Double, ringWidth: Double = 1.0)
	            (implicit colorScheme: ColorScheme) =
		_apply[A](selectedValuePointer, value, backgroundColorPointer, diameter, hoverExtraRadius, ringWidth)
}

/**
  * Used for defining radio button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class RadioButtonSetup(settings: RadioButtonSettings = RadioButtonSettings.default)
	extends RadioButtonSettingsWrapper[RadioButtonSetup] with ComponentFactoryFactory[RadioButtonFactory]
		with FromContextComponentFactoryFactory[VariableColorContext, ContextualRadioButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = RadioButtonFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, c: VariableColorContext) =
		ContextualRadioButtonFactory(hierarchy, c, settings)
	
	override def withSettings(settings: RadioButtonSettings) = copy(settings = settings)
}

object RadioButton extends RadioButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: RadioButtonSettings) = withSettings(settings)
}

/**
 * Used for selecting a single item from a list of items
 * @author Mikko Hilpinen
 * @since 30.1.2021, v0.1
 */
class RadioButton[A](override val hierarchy: ComponentHierarchy, selectedValuePointer: EventfulPointer[A],
                     representing: A, backgroundColorPointer: Changing[Color],
                     diameter: Double, hoverExtraRadius: Double, ringWidth: Double = 1.0, emptyRingWidth: Double = 1.25,
                     settings: RadioButtonSettings = RadioButtonSettings.default)
                    (implicit colorScheme: ColorScheme)
	extends AbstractButton(settings) with ConcreteCustomDrawReachComponent
{
	// ATTRIBUTES   ---------------------------------
	
	/**
	 * A pointer that contains whether this button is currently selected
	 */
	val selectedPointer = selectedValuePointer.map { _ == representing }
	
	override val calculatedStackSize = {
		// Hover radius is omitted in the minimum size
		val optimalLength = diameter + hoverExtraRadius * 2
		StackLength(diameter, optimalLength, optimalLength).square
	}
	
	private val colorPointer = backgroundColorPointer
		.mergeWith(selectedPointer, settings.enabledFlag) { (background, isSelected, isEnabled) =>
			// While disabled or unselected, uses either black or white, with certain opacity
			// Otherwise uses the selection color
			if (isSelected && isEnabled)
				colorScheme(settings.selectedColorRole)
					.against(background, minimumContrast = Minimum.defaultMinimumContrast)
			else {
				val base = background.shade.defaultTextColor
				if (isEnabled) base.withAlpha(0.72) else base.withAlpha(0.5)
			}
		}
	private val hoverColorPointer = colorPointer
		.lazyMergeWith(statePointer) { (color, state) => color.timesAlpha(state.hoverAlpha) }
	
	override val customDrawers = RadioButtonDrawer +: settings.customDrawers
	
	
	// INITIAL CODE ---------------------------------
	
	setup()
	selectedPointer.addContinuousAnyChangeListener { repaint(High) }
	
	
	// COMPUTED -------------------------------------
	
	def selected = selectedPointer.value
	
	
	// IMPLEMENTED  ---------------------------------
	
	override protected def trigger() = select()
	
	override def updateLayout() = ()
	
	override def cursorToImage(cursor: Cursor, position: Point) =
		if (selected) cursor.over(colorPointer.value) else cursor.over(backgroundColorPointer.value)
	
	
	// OTHER    ---------------------------------------
	
	/**
	 * Selects this radio button
	 */
	def select() = selectedValuePointer.value = representing
	
	
	// NESTED   --------------------------------------
	
	private object RadioButtonDrawer extends CustomDrawer
	{
		// ATTRIBUTES   ------------------------
		
		// Draw settings when drawing (filled) circles
		private val mainDsPointer = colorPointer.lazyMap(DrawSettings.onlyFill)
		private val bgDsPointer = backgroundColorPointer.lazyMap(DrawSettings.onlyFill)
		
		
		// IMPLEMENTED  ------------------------
		
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Calculates dimensions
			val center = bounds.center.round
			val maxRadius = bounds.size.minDimension / 2
			if (maxRadius > 1.0) {
				val buttonRadius = (diameter / 2).round.toDouble min maxRadius
				// Uses anti-aliasing while drawing
				drawer.antialiasing.use { drawer =>
					// Draws the full (outer) circle first
					drawer.draw(Circle(center, buttonRadius))(mainDsPointer.value)
					
					// Draws the background over the drawn circle to create an open circle
					val emptyCircleRadius = {
						val base = buttonRadius - ringWidth
						(if (selected) base max 2.0 else base).round.toDouble
					}
					if (emptyCircleRadius > 0)
						drawer.draw(Circle(center, emptyCircleRadius))(bgDsPointer.value)
					
					// Draws the hover effect, if necessary
					val hoverColor = hoverColorPointer.value
					if (hoverColor.alpha > 0.0)
						drawer.draw(Circle(center, (buttonRadius + hoverExtraRadius) min maxRadius))(
							DrawSettings.onlyFill(hoverColor))
					
					// Finally draws the selected center, if necessary
					if (selected)
						drawer.draw(Circle(center, (emptyCircleRadius - emptyRingWidth) max 1.0))(mainDsPointer.value)
				}
			}
		}
	}
}

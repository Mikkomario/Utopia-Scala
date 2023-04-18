package utopia.reach.component.input.check

import utopia.firmament.context.{ColorContext, ComponentCreationDefaults}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.template.DrawLevel.Normal
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.StackLength
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.color.{Color, ColorRole, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard.Minimum
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Point}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.{ColorContextualFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ButtonLike, CustomDrawReachComponent}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reach.util.Priority.High

object RadioButton extends Cff[RadioButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new RadioButtonFactory(hierarchy)
}

class RadioButtonFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[ColorContext, ContextualRadioButtonFactory]
{
	// IMPLEMENTED  ------------------------------------
	
	override def withContext(context: ColorContext) =
		ContextualRadioButtonFactory(this, context)
	
	
	// OTHER    ----------------------------------------
	
	/**
	 * Creates a new radio button
	 * @param selectedValuePointer A mutable pointer to the currently selected value
	 * @param value Value represented by this radio button
	 * @param backgroundColorPointer A pointer to the current background color
	 * @param diameter The diameter (2 * radius) of this button
	 * @param hoverExtraRadius Added radius for the hover effect
	 * @param ringWidth Width of the outer ring in this button (default = 1 px)
	 * @param selectedColorRole Color role that represents the selected state (default = Secondary)
	 * @param enabledPointer A pointer that contains the enabled status of this button (default = always enabled)
	 * @param customDrawers Custom drawers to assign to this button (default = empty)
	 * @param focusListeners Focus listeners to assign to this button (default = empty)
	 * @param colorScheme Color scheme used (implicit)
	 * @tparam A Type of selected value
	 * @return A new radio button
	 */
	def apply[A](selectedValuePointer: PointerWithEvents[A], value: A,
	             backgroundColorPointer: Changing[Color], diameter: Double,
	             hoverExtraRadius: Double, ringWidth: Double = 1.0, selectedColorRole: ColorRole = ColorRole.Secondary,
	             enabledPointer: Changing[Boolean] = AlwaysTrue,
	             customDrawers: Vector[CustomDrawer] = Vector(),
	             focusListeners: Seq[FocusListener] = Vector())(implicit colorScheme: ColorScheme) =
		new RadioButton[A](parentHierarchy, selectedValuePointer, value, backgroundColorPointer, diameter,
			hoverExtraRadius, ringWidth, (ringWidth * 1.25).round.toDouble, selectedColorRole, enabledPointer, customDrawers,
			focusListeners)
}

case class ContextualRadioButtonFactory(factory: RadioButtonFactory, context: ColorContext)
	extends ColorContextualFactory[ContextualRadioButtonFactory]
{
	// IMPLICIT ------------------------------
	
	private implicit def colorScheme: ColorScheme = context.colors
	
	
	// IMPLEMENTED  --------------------------
	
	override def self: ContextualRadioButtonFactory = this
	
	override def withContext(newContext: ColorContext) = copy(context = newContext)
	
	
	// OTHER    ------------------------------
	
	/**
	 * Creates a new radio button
	 * @param selectedValuePointer A mutable pointer to the currently selected value
	 * @param value Value represented by this radio button
	 * @param selectedColorRole Color role that represents the selected state (default = Secondary)
	 * @param enabledPointer A pointer that contains the enabled status of this button (default = always enabled)
	 * @param backgroundColorPointer A pointer to the current background color (default = determined by context (fixed))
	  * @param sizeModifier A modifier applied to this button's size (default = global default)
	 * @param customDrawers Custom drawers to assign to this button (default = empty)
	 * @param focusListeners Focus listeners to assign to this button (default = empty)
	 * @tparam A Type of selected value
	 * @return A new radio button
	 */
	def apply[A](selectedValuePointer: PointerWithEvents[A], value: A,
	             selectedColorRole: ColorRole = ColorRole.Secondary,
	             enabledPointer: Changing[Boolean] = AlwaysTrue,
	             backgroundColorPointer: Changing[Color] = Fixed(context.background),
	             sizeModifier: Double = ComponentCreationDefaults.radioButtonScalingFactor,
	             customDrawers: Vector[CustomDrawer] = Vector(),
	             focusListeners: Seq[FocusListener] = Vector()) =
	{
		val sizeMod = ComponentCreationDefaults.radioButtonScalingFactor
		factory(selectedValuePointer, value, backgroundColorPointer,
			(context.margins.medium * 1.6 * sizeMod).round.toDouble,
			(context.margins.medium * 0.4 * sizeMod).round.toDouble,
			((context.margins.medium * 0.22 * sizeMod) max 1.0).round.toDouble,
			selectedColorRole, enabledPointer, customDrawers, focusListeners)
	}
}

/**
 * Used for selecting a single item from a list of items
 * @author Mikko Hilpinen
 * @since 30.1.2021, v0.1
 */
class RadioButton[A](override val parentHierarchy: ComponentHierarchy, selectedValuePointer: PointerWithEvents[A],
                     representing: A, backgroundColorPointer: Changing[Color],
                     diameter: Double, hoverExtraRadius: Double, ringWidth: Double = 1.0, emptyRingWidth: Double = 1.25,
                     selectedColorRole: ColorRole = Secondary, enabledPointer: Changing[Boolean] = AlwaysTrue,
                     additionalDrawers: Vector[CustomDrawer] = Vector(),
                     additionalFocusListeners: Seq[FocusListener] = Vector())
                    (implicit colorScheme: ColorScheme)
	extends CustomDrawReachComponent with ButtonLike
{
	// ATTRIBUTES   ---------------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	override val statePointer = {
		if (enabledPointer.isAlwaysTrue)
			baseStatePointer.view
		else
			baseStatePointer.mergeWith(enabledPointer) { (base, enabled) => base + (Disabled -> !enabled) }
	}
	
	/**
	 * A pointer that contains whether this button is currently selected
	 */
	val selectedPointer = selectedValuePointer.map { _ == representing }
	
	override val calculatedStackSize =
	{
		// Hover radius is omitted in the minimum size
		val optimalLength = diameter + hoverExtraRadius * 2
		StackLength(diameter, optimalLength, optimalLength).square
	}
	
	override val focusId = hashCode()
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	
	private val colorPointer = backgroundColorPointer
		.mergeWith(selectedPointer, enabledPointer) { (background, isSelected, isEnabled) =>
			// While disabled or unselected, uses either black or white, with certain opacity
			// Otherwise uses the selection color
			if (isSelected && isEnabled)
				colorScheme(selectedColorRole).against(background, minimumContrast = Minimum.defaultMinimumContrast)
			else {
				val base = background.shade.defaultTextColor
				if (isEnabled) base.withAlpha(0.72) else base.withAlpha(0.5)
			}
		}
	private val hoverColorPointer = colorPointer.lazyMergeWith(baseStatePointer) { (color, state) =>
		color.timesAlpha(state.hoverAlpha)
	}
	
	override val customDrawers = RadioButtonDrawer +: additionalDrawers
	
	
	// INITIAL CODE ---------------------------------
	
	setup(baseStatePointer)
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

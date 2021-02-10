package utopia.reach.component.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{AlwaysTrue, ChangingLike, Fixed}
import utopia.genesis.color.ColorContrastStandard.Minimum
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point}
import utopia.genesis.util.Drawer
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ButtonLike, CustomDrawReachComponent}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reach.util.Priority.High
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.{ColorRole, ColorScheme, ComponentColor}
import utopia.reflection.component.context.ColorContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.event.ButtonState
import utopia.reflection.shape.stack.StackLength

object RadioButton
	extends ContextInsertableComponentFactoryFactory[ColorContextLike, RadioButtonFactory, ContextualRadioButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new RadioButtonFactory(hierarchy)
}

class RadioButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualRadioButtonFactory]
{
	// IMPLEMENTED  ------------------------------------
	
	override def withContext[N <: ColorContextLike](context: N) =
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
	             backgroundColorPointer: ChangingLike[ComponentColor], diameter: Double,
	             hoverExtraRadius: Double, ringWidth: Double = 1.0, selectedColorRole: ColorRole = ColorRole.Secondary,
	             enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
	             customDrawers: Vector[CustomDrawer] = Vector(),
	             focusListeners: Seq[FocusListener] = Vector())(implicit colorScheme: ColorScheme) =
		new RadioButton[A](parentHierarchy, selectedValuePointer, value, backgroundColorPointer, diameter,
			hoverExtraRadius, ringWidth, (ringWidth * 1.25).round.toDouble, selectedColorRole, enabledPointer, customDrawers,
			focusListeners)
}

case class ContextualRadioButtonFactory[+N <: ColorContextLike](factory: RadioButtonFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualRadioButtonFactory]
{
	// IMPLICIT ------------------------------
	
	private implicit def colorScheme: ColorScheme = context.colorScheme
	
	
	// IMPLEMENTED  --------------------------
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER    ------------------------------
	
	/**
	 * Creates a new radio button
	 * @param selectedValuePointer A mutable pointer to the currently selected value
	 * @param value Value represented by this radio button
	 * @param selectedColorRole Color role that represents the selected state (default = Secondary)
	 * @param enabledPointer A pointer that contains the enabled status of this button (default = always enabled)
	 * @param backgroundColorPointer A pointer to the current background color (default = determined by context (fixed))
	 * @param customDrawers Custom drawers to assign to this button (default = empty)
	 * @param focusListeners Focus listeners to assign to this button (default = empty)
	 * @tparam A Type of selected value
	 * @return A new radio button
	 */
	def apply[A](selectedValuePointer: PointerWithEvents[A], value: A,
	             selectedColorRole: ColorRole = ColorRole.Secondary,
	             enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
	             backgroundColorPointer: ChangingLike[ComponentColor] = Fixed(context.containerBackground),
	             customDrawers: Vector[CustomDrawer] = Vector(),
	             focusListeners: Seq[FocusListener] = Vector()) =
	{
		
		factory(selectedValuePointer, value, backgroundColorPointer, (context.margins.medium * 2).round.toDouble,
			(context.margins.medium * 0.8).round.toDouble, ((context.margins.medium * 0.25) max 1.0).round.toDouble,
			selectedColorRole, enabledPointer, customDrawers, focusListeners)
	}
}

/**
 * Used for selecting a single item from a list of items
 * @author Mikko Hilpinen
 * @since 30.1.2021, v1
 */
class RadioButton[A](override val parentHierarchy: ComponentHierarchy, selectedValuePointer: PointerWithEvents[A],
					 representing: A, backgroundColorPointer: ChangingLike[ComponentColor],
					 diameter: Double, hoverExtraRadius: Double, ringWidth: Double = 1.0, emptyRingWidth: Double = 1.25,
					 selectedColorRole: ColorRole = Secondary, enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
					 additionalDrawers: Vector[CustomDrawer] = Vector(),
					 additionalFocusListeners: Seq[FocusListener] = Vector())
                    (implicit colorScheme: ColorScheme)
	extends CustomDrawReachComponent with ButtonLike
{
	// ATTRIBUTES   ---------------------------------
	
	private val baseStatePointer = new PointerWithEvents(ButtonState.default)
	override val statePointer = baseStatePointer.mergeWith(enabledPointer) { (base, enabled) =>
		base.copy(isEnabled = enabled) }
	
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
				colorScheme(selectedColorRole).forBackground(background, Minimum.defaultMinimumContrast).background
			else
			{
				val base = background.defaultTextColor
				if (isEnabled) base.withAlpha(0.72) else base.withAlpha(0.5)
			}
		}
	private val hoverColorPointer = colorPointer.lazyMergeWith(baseStatePointer) { (color, state) =>
		color.timesAlpha(state.hoverAlpha)
	}
	
	override val customDrawers = RadioButtonDrawer +: additionalDrawers
	
	
	// INITIAL CODE ---------------------------------
	
	setup(baseStatePointer)
	selectedPointer.addAnyChangeListener { repaint(High) }
	
	
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
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Calculates dimensions
			val center = bounds.center.round
			val maxRadius = bounds.minDimension / 2
			if (maxRadius > 1.0)
			{
				val buttonRadius = (diameter / 2).round.toDouble min maxRadius
				
				// Draws the full (outer) circle first
				val mainDrawer = drawer.onlyFill(colorPointer.value)
				mainDrawer.draw(Circle(center, buttonRadius))
				
				// Draws the background over the drawn circle to create an open circle
				val emptyCircleRadius =
				{
					val base = buttonRadius - ringWidth
					(if (selected) base max 2.0 else base).round.toDouble
				}
				if (emptyCircleRadius > 0)
					drawer.onlyFill(backgroundColorPointer.value).draw(Circle(center, emptyCircleRadius))
				
				// Draws the hover effect, if necessary
				val hoverColor = hoverColorPointer.value
				if (hoverColor.alpha > 0.0)
					drawer.onlyFill(hoverColor).draw(Circle(center, (buttonRadius + hoverExtraRadius) min maxRadius))
				
				// Finally draws the selected center, if necessary
				if (selected)
					mainDrawer.draw(Circle(center, (emptyCircleRadius - emptyRingWidth) max 1.0))
			}
		}
	}
}

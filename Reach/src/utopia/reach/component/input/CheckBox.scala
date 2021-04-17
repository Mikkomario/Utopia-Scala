package utopia.reach.component.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{AlwaysTrue, ChangingLike}
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point}
import utopia.genesis.util.Drawer
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.ViewImageLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.color.{ColorRole, ColorShadeVariant}
import utopia.reflection.component.context.ColorContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Background
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.event.ButtonState
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.shape.LengthExtensions._

object CheckBox
	extends ContextInsertableComponentFactoryFactory[ColorContextLike, CheckBoxFactory, ContextualCheckBoxFactory]
{
	// IMPLEMENTED	-------------------------
	
	override def apply(hierarchy: ComponentHierarchy) = new CheckBoxFactory(hierarchy)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a version of this factory which uses specific global settings when creating components
	  * @param onIcon Icon that represents a ticked check box state
	  * @param offIcon Icon that represents an unticked check box state
	  * @param selectionColorRole Color role used in the selected state (default = secondary)
	  * @return A new check box factory factory
	  */
	def full(onIcon: SingleColorIcon, offIcon: SingleColorIcon, selectionColorRole: ColorRole = Secondary) =
		new FullCheckBoxFactoryFactory(onIcon, offIcon, selectionColorRole)
}

class CheckBoxFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualCheckBoxFactory]
{
	override def withContext[N <: ColorContextLike](context: N) =
		ContextualCheckBoxFactory(this, context)
	
	/**
	  * Creates a new check box
	  * @param onImage Image displayed when this box is ticked
	  * @param offImage Image displayed when this box is not ticked
	  * @param onHoverColor Hover effect base color when this box is ticked
	  * @param offHoverColor Hover effect base color when this box is not ticked
	  * @param hoverRadius Hover effect radius (pixels) past image borders (default = 0 px)
	  * @param valuePointer Mutable pointer to currently selected value (default = new pointer)
	  * @param enabledPointer Pointer to this component's enabled state (default = always enabled)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param focusListeners Focus listeners assigned to this component (default = empty)
	  * @return A new check box
	  */
	def apply(onImage: Image, offImage: Image, onHoverColor: Color, offHoverColor: Color, hoverRadius: Double = 0.0,
			  valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			  enabledPointer: ChangingLike[Boolean] = AlwaysTrue, customDrawers: Vector[CustomDrawer] = Vector(),
			  focusListeners: Seq[FocusListener] = Vector()) =
		new CheckBox(parentHierarchy, onImage, offImage, onHoverColor, offHoverColor, hoverRadius, valuePointer,
			enabledPointer, customDrawers, focusListeners)
}

case class ContextualCheckBoxFactory[+N <: ColorContextLike](factory: CheckBoxFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualCheckBoxFactory]
{
	private implicit val c: ColorContextLike = context
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new check box
	  * @param onIcon Icon displayed when this box is selected
	  * @param offIcon Icon displayed when this box is not selected
	  * @param valuePointer Mutable pointer to currently selected value (default = new pointer)
	  * @param enabledPointer Pointer to this component's enabled state (default = always enabled)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param focusListeners Focus listeners assigned to this component (default = empty)
	  * @param selectionColorRole Color role that represents the selected state (default = secondary)
	  * @return A new check box
	  */
	def apply(onIcon: SingleColorIcon, offIcon: SingleColorIcon,
			  valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			  enabledPointer: ChangingLike[Boolean] = AlwaysTrue, customDrawers: Vector[CustomDrawer] = Vector(),
			  focusListeners: Seq[FocusListener] = Vector(), selectionColorRole: ColorRole = Secondary) =
	{
		val selectedColor = context.color(selectionColorRole)
		factory(onIcon.asImageWithColor(selectedColor), offIcon.singleColorImage, selectedColor,
			context.containerBackground.defaultTextColor.withAlpha(1.0),
			context.margins.medium.round.toDouble, valuePointer, enabledPointer, customDrawers, focusListeners)
	}
}

class FullCheckBoxFactoryFactory(onIcon: SingleColorIcon, offIcon: SingleColorIcon,
								 selectionColorRole: ColorRole = Secondary)
	extends ContextInsertableComponentFactoryFactory[ColorContextLike, FullCheckBoxFactory, FullContextualCheckBoxFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new FullCheckBoxFactory(new CheckBoxFactory(hierarchy), onIcon,
		offIcon, selectionColorRole)
}

class FullCheckBoxFactory(factory: CheckBoxFactory, onIcon: SingleColorIcon, offIcon: SingleColorIcon,
						  selectionColorRole: ColorRole = Secondary)
	extends ContextInsertableComponentFactory[ColorContextLike, FullContextualCheckBoxFactory]
{
	override def withContext[N <: ColorContextLike](context: N) =
		FullContextualCheckBoxFactory(factory.withContext(context), onIcon, offIcon, selectionColorRole)
}

case class FullContextualCheckBoxFactory[+N <: ColorContextLike](factory: ContextualCheckBoxFactory[N],
																 onIcon: SingleColorIcon, offIcon: SingleColorIcon,
																 selectionColorRole: ColorRole = Secondary)
	extends ContextualComponentFactory[N, ColorContextLike, FullContextualCheckBoxFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def context = factory.context
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(factory = factory.withContext(newContext))
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new check box
	  * @param valuePointer Mutable pointer to currently selected value (default = new pointer)
	  * @param enabledPointer Pointer to this component's enabled state (default = always enabled)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param focusListeners Focus listeners assigned to this component (default = empty)
	  * @return A new check box
	  */
	def apply(valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			  enabledPointer: ChangingLike[Boolean] = AlwaysTrue, customDrawers: Vector[CustomDrawer] = Vector(),
			  focusListeners: Seq[FocusListener] = Vector()) =
	{
		factory(onIcon, offIcon, valuePointer, enabledPointer, customDrawers, focusListeners, selectionColorRole)
	}
}

/**
  * Used for toggling a boolean value. Changes drawn image based on the selected value.
  * @author Mikko Hilpinen
  * @since 25.2.2021, v0.1
  */
class CheckBox(parentHierarchy: ComponentHierarchy, onImage: Image, offImage: Image, onHoverColor: Color,
			   offHoverColor: Color, hoverRadius: Double = 0.0,
			   override val valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			   enabledPointer: ChangingLike[Boolean] = AlwaysTrue, additionalDrawers: Vector[CustomDrawer] = Vector(),
			   additionalFocusListeners: Seq[FocusListener] = Vector())
	extends ReachComponentWrapper with ButtonLike with InteractionWithPointer[Boolean]
{
	// ATTRIBUTES	---------------------------
	
	private val baseStatePointer = new PointerWithEvents(ButtonState.default)
	override val statePointer = baseStatePointer.mergeWith(enabledPointer) { (base, enabled) =>
		base.copy(isEnabled = enabled) }
	
	private val baseImagePointer = valuePointer.map { if (_) onImage else offImage }
	/**
	  * Pointer to the currently displayed image
	  */
	val imagePointer = baseImagePointer.mergeWith(enabledPointer) { (image, enabled) =>
		if (enabled) image else image.timesAlpha(0.66) }
	override protected val wrapped = ViewImageLabel(parentHierarchy)
		.withStaticLayout(imagePointer, hoverRadius.downscaling.toInsets,
			additionalCustomDrawers = additionalDrawers :+ HoverDrawer)
	
	override val focusId = hashCode()
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	
	
	// INITIAL CODE	--------------------------
	
	setup(baseStatePointer)
	
	
	// IMPLEMENTED	--------------------------
	
	override protected def trigger() = valuePointer.update { !_ }
	
	override def cursorToImage(cursor: Cursor, position: Point) =
		cursor(ColorShadeVariant.forLuminosity(baseImagePointer.value.pixels.averageLuminosity).opposite)
	
	
	// NESTED	------------------------------
	
	// Used for drawing the interactive hover effect on focus
	private object HoverDrawer extends CustomDrawer
	{
		override def drawLevel = Background
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Calculates draw alpha (if any)
			val alpha = state.hoverAlpha
			if (alpha > 0)
				drawer.onlyFill(if (value) onHoverColor else offHoverColor).withAlpha(alpha)
					.draw(Circle(bounds.center, bounds.minDimension / 2.0))
		}
	}
}

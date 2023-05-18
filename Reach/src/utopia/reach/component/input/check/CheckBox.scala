package utopia.reach.component.input.check

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.template.DrawLevel.Background
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.color.{Color, ColorRole, ColorShade}
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Point}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.factory.contextual.ColorContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

// TODO: Also add a component for text box + label
object CheckBox extends Cff[CheckBoxFactory]
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
	extends FromContextFactory[ColorContext, ContextualCheckBoxFactory]
{
	override def withContext(context: ColorContext) =
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
	          enabledPointer: Changing[Boolean] = AlwaysTrue, customDrawers: Vector[CustomDrawer] = Vector(),
	          focusListeners: Seq[FocusListener] = Vector()) =
		new CheckBox(parentHierarchy, onImage, offImage, onHoverColor, offHoverColor, hoverRadius, valuePointer,
			enabledPointer, customDrawers, focusListeners)
}

case class ContextualCheckBoxFactory(factory: CheckBoxFactory, context: ColorContext)
	extends ColorContextualFactory[ContextualCheckBoxFactory]
{
	private implicit val c: ColorContext = context
	
	override def self: ContextualCheckBoxFactory = this
	
	override def withContext(newContext: ColorContext) = copy(context = newContext)
	
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
	          enabledPointer: Changing[Boolean] = AlwaysTrue, customDrawers: Vector[CustomDrawer] = Vector(),
	          focusListeners: Seq[FocusListener] = Vector(), selectionColorRole: ColorRole = Secondary) =
	{
		val selectedColor = context.color(selectionColorRole)
		factory(onIcon(selectedColor), offIcon.contextual, selectedColor,
			context.textColor.withAlpha(1.0),
			context.margins.medium.round.toDouble, valuePointer, enabledPointer, customDrawers, focusListeners)
	}
}

class FullCheckBoxFactoryFactory(onIcon: SingleColorIcon, offIcon: SingleColorIcon,
								 selectionColorRole: ColorRole = Secondary)
	extends Ccff[ColorContext, FullContextualCheckBoxFactory]
{
	override def withContext(parentHierarchy: ComponentHierarchy, context: ColorContext) =
		FullContextualCheckBoxFactory(new CheckBoxFactory(parentHierarchy).withContext(context), onIcon,
			offIcon, selectionColorRole)
}

case class FullContextualCheckBoxFactory(factory: ContextualCheckBoxFactory,
                                         onIcon: SingleColorIcon, offIcon: SingleColorIcon,
                                         selectionColorRole: ColorRole = Secondary)
	extends ColorContextualFactory[FullContextualCheckBoxFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def context = factory.context
	
	override def self: FullContextualCheckBoxFactory = this
	
	override def withContext(newContext: ColorContext) =
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
	          enabledPointer: Changing[Boolean] = AlwaysTrue, customDrawers: Vector[CustomDrawer] = Vector(),
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
               enabledPointer: Changing[Boolean] = AlwaysTrue, additionalDrawers: Vector[CustomDrawer] = Vector(),
               additionalFocusListeners: Seq[FocusListener] = Vector())
	extends ReachComponentWrapper with ButtonLike with InteractionWithPointer[Boolean]
{
	// ATTRIBUTES	---------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	override val statePointer = baseStatePointer.mergeWith(enabledPointer) { (base, enabled) =>
		base + (Disabled -> !enabled) }
	
	private val baseImagePointer = valuePointer.map { if (_) onImage else offImage }
	/**
	  * Pointer to the currently displayed image
	  */
	val imagePointer = baseImagePointer.mergeWith(enabledPointer) { (image, enabled) =>
		if (enabled) image else image.timesAlpha(0.66) }
	override protected val wrapped = ViewImageLabel(parentHierarchy)
		.withInsets(hoverRadius.downscaling.toInsets).withCustomDrawers(additionalDrawers :+ HoverDrawer)
		.apply(imagePointer)
	
	override val focusId = hashCode()
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	
	
	// INITIAL CODE	--------------------------
	
	setup(baseStatePointer)
	
	
	// IMPLEMENTED	--------------------------
	
	override protected def trigger() = valuePointer.update { !_ }
	
	override def cursorToImage(cursor: Cursor, position: Point) =
		cursor(ColorShade.forLuminosity(baseImagePointer.value.pixels.averageLuminosity).opposite)
	
	
	// NESTED	------------------------------
	
	// Used for drawing the interactive hover effect on focus
	private object HoverDrawer extends CustomDrawer
	{
		// ATTRIBUTES   -----------------------
		
		// Hover and value affect the drawing
		private val dsPointer = statePointer.lazyMergeWith(valuePointer) { (state, value) =>
			DrawSettings.onlyFill((if (value) onHoverColor else offHoverColor).withAlpha(state.hoverAlpha))
		}
		private lazy val shapePointer = boundsPointer.lazyMap { bounds =>
			Circle(bounds.center, bounds.size.minDimension / 2.0)
		}
		
		
		// IMPLEMENTED  -----------------------
		
		override def drawLevel = Background
		
		override def opaque = false
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			implicit val ds: DrawSettings = dsPointer.value
			if (ds.fillColor.exists { _.alpha > 0 })
				drawer.draw(shapePointer.value)
		}
	}
}

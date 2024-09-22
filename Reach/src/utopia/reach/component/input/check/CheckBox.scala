package utopia.reach.component.input.check

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.context.{ColorContext, ComponentCreationDefaults}
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.HotKey
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.operator.enumeration.End.First
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Background
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.ColorContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

import scala.language.implicitConversions

/**
  * Common trait for check box factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
// TODO: Add scaling support
trait CheckBoxSettingsLike[+Repr] extends CustomDrawableFactory[Repr] with ButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings that affect the button aspects of these check boxes
	  */
	def buttonSettings: ButtonSettings
	/**
	  * Settings that affect the button aspects of these check boxes
	  * @param settings New button settings to use.
	  *                 Settings that affect the button aspects of these check boxes
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def enabledPointer = buttonSettings.enabledPointer
	override def focusListeners = buttonSettings.focusListeners
	override def hotKeys = buttonSettings.hotKeys
	
	override def withEnabledPointer(p: Changing[Boolean]) =
		withButtonSettings(buttonSettings.withEnabledPointer(p))
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	override def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
}

object CheckBoxSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
	
	
	// IMPLICIT ------------------------
	
	// Implicitly converts from button settings
	implicit def wrap(buttonSettings: ButtonSettings): CheckBoxSettings = apply(Empty, buttonSettings)
}
/**
  * Combined settings used when constructing check boxs
  * @param customDrawers  Custom drawers to assign to created components
  * @param buttonSettings Settings that affect the button aspects of these check boxes
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
case class CheckBoxSettings(customDrawers: Seq[CustomDrawer] = Empty,
                            buttonSettings: ButtonSettings = ButtonSettings.default)
	extends CheckBoxSettingsLike[CheckBoxSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
}

/**
  * Common trait for factories that wrap a check box settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
trait CheckBoxSettingsWrapper[+Repr] extends CheckBoxSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: CheckBoxSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: CheckBoxSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def customDrawers = settings.customDrawers
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: CheckBoxSettings => CheckBoxSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing check boxs
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
trait CheckBoxFactoryLike[+Repr] extends CheckBoxSettingsWrapper[Repr] with PartOfComponentHierarchy

/**
  * Factory class used for constructing check boxs using contextual component creation information
  * @param selectedColorRole Color (role) of the hover effects in check boxes while selected
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
case class ContextualCheckBoxFactory(parentHierarchy: ComponentHierarchy, context: ColorContext,
                                     settings: CheckBoxSettings = CheckBoxSettings.default,
                                     selectedColorRole: ColorRole = ColorRole.Secondary)
	extends CheckBoxFactoryLike[ContextualCheckBoxFactory]
		with ColorContextualFactory[ContextualCheckBoxFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def self: ContextualCheckBoxFactory = this
	override def withContext(context: ColorContext) = copy(context = context)
	override def withSettings(settings: CheckBoxSettings) = copy(settings = settings)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param role Color (role) of the hover effects in check boxes while selected
	  * @return Copy of this factory with the specified selected color role
	  */
	def withSelectedColorRole(role: ColorRole) = copy(selectedColorRole = role)
	
	/**
	  * Creates a new check box
	  * @param images Images to display on this checkbox (off & on). Specified as either icons or images.
	  * @param valuePointer       Mutable pointer to currently selected value (default = new pointer)
	  * @return A new check box
	  */
	def iconsOrImages(images: Either[Pair[SingleColorIcon], Pair[Image]],
	                  valuePointer: EventfulPointer[Boolean] = EventfulPointer(false)) =
	{
		// Requires high contrast because of low alpha values
		implicit val c: ColorContext = context.withEnhancedColorContrast
		
		// Converts the icons to images, if specified
		val appliedImages = images.rightOrMap { icons =>
			icons.mapWithSides { (icon, side) =>
				// Case: Not selected => Uses black or white color
				if (side == First)
					icon.contextual
				// Case: Selected => Uses the selection color
				else
					icon(selectedColorRole)
			}
		}
		new CheckBox(parentHierarchy, appliedImages,
			Pair(context.textColor.withAlpha(1.0), context.color(selectedColorRole)),
			context.margins.small.round.toDouble, settings, valuePointer)
	}
	/**
	  * Creates a new check box
	  * @param icons       Icons to display on this checkbox (off & on)
	  * @param valuePointer Mutable pointer to currently selected value (default = new pointer)
	  * @return A new check box
	  */
	def icons(icons: Pair[SingleColorIcon],
	          valuePointer: EventfulPointer[Boolean] = EventfulPointer[Boolean](false)) =
		iconsOrImages(Left(icons), valuePointer)
	/**
	  * Creates a new check box
	  * @param images       Images to display on this checkbox (off & on)
	  * @param valuePointer Mutable pointer to currently selected value (default = new pointer)
	  * @return A new check box
	  */
	def images(images: Pair[Image], valuePointer: EventfulPointer[Boolean] = EventfulPointer[Boolean](false)) =
		iconsOrImages(Right(images), valuePointer)
}

/**
  * Factory class that is used for constructing check boxs without using contextual information
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
case class CheckBoxFactory(parentHierarchy: ComponentHierarchy,
                           settings: CheckBoxSettings = CheckBoxSettings.default)
	extends CheckBoxFactoryLike[CheckBoxFactory]
		with FromContextFactory[ColorContext, ContextualCheckBoxFactory]
{
	import utopia.firmament.context.ComponentCreationDefaults.componentLogger
	
	// IMPLEMENTED	--------------------
	
	override def withContext(context: ColorContext) =
		ContextualCheckBoxFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: CheckBoxSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new check box
	  * @param images Images that represent this check box (off & on)
	  * @param hoverColors Colors to use when drawing hover effects (off & on)
	  * @param hoverRadius Hover effect radius (pixels) past image borders (default = 0 px)
	  * @param valuePointer Mutable pointer to currently selected value (default = new pointer)
	  * @return A new check box
	  */
	def apply(images: Pair[Image], hoverColors: Pair[Color], hoverRadius: Double = 0.0,
	          valuePointer: EventfulPointer[Boolean] = EventfulPointer(false)) =
		new CheckBox(parentHierarchy, images, hoverColors, hoverRadius, settings, valuePointer)
}

case class FullContextualCheckBoxFactory(factory: ContextualCheckBoxFactory,
                                         images: Either[Pair[SingleColorIcon], Pair[Image]])
	extends CheckBoxSettingsWrapper[FullContextualCheckBoxFactory] with
		ColorContextualFactory[FullContextualCheckBoxFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def context = factory.context
	override def self: FullContextualCheckBoxFactory = this
	override protected def settings: CheckBoxSettings = factory.settings
	
	override def withSettings(settings: CheckBoxSettings): FullContextualCheckBoxFactory =
		copy(factory = factory.withSettings(settings))
	override def withContext(newContext: ColorContext) =
		copy(factory = factory.withContext(newContext))
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new check box
	  * @param valuePointer   Mutable pointer to currently selected value (default = new pointer)
	  * @return A new check box
	  */
	def apply(valuePointer: EventfulPointer[Boolean] = EventfulPointer(false)) =
		factory.iconsOrImages(images, valuePointer)
}

case class FullCheckBoxSetup(images: Either[Pair[SingleColorIcon], Pair[Image]],
                             settings: CheckBoxSettings = CheckBoxSettings.default,
                             selectedColorRole: ColorRole = Secondary)
	extends Ccff[ColorContext, FullContextualCheckBoxFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def withContext(parentHierarchy: ComponentHierarchy, context: ColorContext) =
		FullContextualCheckBoxFactory(ContextualCheckBoxFactory(parentHierarchy, context, settings, selectedColorRole),
			images)
	
	
	// OTHER    -------------------------
	
	def withSelectedColorRole(role: ColorRole) = copy(selectedColorRole = role)
}

/**
  * Used for defining check box creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 20.06.2023, v1.1
  */
case class CheckBoxSetup(settings: CheckBoxSettings = CheckBoxSettings.default)
	extends CheckBoxSettingsWrapper[CheckBoxSetup] with ComponentFactoryFactory[CheckBoxFactory]
		with FromContextComponentFactoryFactory[ColorContext, ContextualCheckBoxFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = CheckBoxFactory(hierarchy, settings)
	override def withContext(hierarchy: ComponentHierarchy, context: ColorContext) =
		ContextualCheckBoxFactory(hierarchy, context, settings)
	override def withSettings(settings: CheckBoxSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param images Images to use in the check boxes (off & on).
	  *               May be specified in either icons (left) or images (right).
	  * @return Copy of this factory that applies the specified images
	  */
	def usingIconsOrImages(images: Either[Pair[SingleColorIcon], Pair[Image]]) = FullCheckBoxSetup(images, settings)
	/**
	  * @param icons Icons to use in the check boxes (off & on).
	  * @return Copy of this factory that applies the specified icons
	  */
	def usingIcons(icons: Pair[SingleColorIcon]) = usingIconsOrImages(Left(icons))
	/**
	  * @param images Images to use in the check boxes (off & on)
	  * @return Copy of this factory that applies the specified images
	  */
	def usingImages(images: Pair[Image]) = usingIconsOrImages(Right(images))
}

// TODO: Also add a component for text box + label
object CheckBox extends CheckBoxSetup()
{
	// OTHER	-----------------------------
	
	def apply(settings: CheckBoxSettings) = withSettings(settings)
	
	/**
	  * Creates a version of this factory which uses specific global settings when creating components
	  * @param onIcon             Icon that represents a ticked check box state
	  * @param offIcon            Icon that represents an unticked check box state
	  * @param selectionColorRole Color role used in the selected state (default = secondary)
	  * @return A new check box factory factory
	  */
	@deprecated("Please use .usingIcons(Pair).withSelectedColorRole(ColorRole) instead", "v1.1")
	def full(onIcon: SingleColorIcon, offIcon: SingleColorIcon, selectionColorRole: ColorRole = Secondary) =
		usingIcons(Pair(offIcon, onIcon)).withSelectedColorRole(selectionColorRole)
}

/**
  * Used for toggling a boolean value. Changes drawn image based on the selected value.
  * @author Mikko Hilpinen
  * @since 25.2.2021, v0.1
  */
class CheckBox(parentHierarchy: ComponentHierarchy,
               images: Pair[Image], hoverColors: Pair[Color],
               hoverRadius: Double = 0.0, settings: CheckBoxSettings = CheckBoxSettings.default,
               override val valuePointer: EventfulPointer[Boolean] = EventfulPointer(false)(ComponentCreationDefaults.componentLogger))
	extends AbstractButton(settings) with ReachComponentWrapper with InteractionWithPointer[Boolean]
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * Pointer to the currently displayed image
	  */
	val imagePointer = valuePointer.mergeWith(settings.enabledPointer) { (selected, enabled) =>
		val base = if (selected) images.second else images.first
		if (enabled) base else base.timesAlpha(0.66)
	}
	override protected val wrapped = {
		ViewImageLabel(parentHierarchy)
			.withInsets(hoverRadius.downscaling.toInsets).withCustomDrawers(settings.customDrawers :+ HoverDrawer)
			.apply(imagePointer)
	}
	
	
	// INITIAL CODE	--------------------------
	
	setup()
	
	
	// IMPLEMENTED	--------------------------
	
	override protected def trigger() = valuePointer.update { !_ }
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(imagePointer.value.shade)
	
	
	// NESTED	------------------------------
	
	// Used for drawing the interactive hover effect on focus
	private object HoverDrawer extends CustomDrawer
	{
		// ATTRIBUTES   -----------------------
		
		// Hover and value affect the drawing
		private val dsPointer = statePointer.lazyMergeWith(valuePointer) { (state, value) =>
			DrawSettings.onlyFill((if (value) hoverColors.second else hoverColors.first).withAlpha(state.hoverAlpha))
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
				drawer.antialiasing.draw(shapePointer.value)
		}
	}
}

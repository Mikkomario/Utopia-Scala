package utopia.reach.component.label.image

import utopia.firmament.component.stack.ConstrainableWrapper
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.reach.component.factory.contextual.VariableBackgroundRoleAssignableFactory
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.Stack

/**
  * Common trait for view image and text label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageAndTextLabelSettingsLike[+Repr]
	extends CustomDrawableFactory[Repr] with FromAlignmentFactory[Repr]
{
	// ABSTRACT	--------------------
	
	protected def imageSettings: ViewImageLabelSettings
	/**
	  * Whether the image and the text should be forced to have equal height or width (depending on the
	  * alignment used)
	  */
	protected def forceEqualBreadth: Boolean
	
	/**
	  * Whether the image and the text should be forced to have equal height or width (depending on the
	  * alignment used)
	  * @param force New force equal breadth to use.
	  *              Whether the image and the text should be forced to have equal height or width (depending on the
	  *              alignment used)
	  * @return Copy of this factory with the specified force equal breadth
	  */
	def withForceEqualBreadth(force: Boolean): Repr
	/**
	  * @param settings New image settings to use.
	  * @return Copy of this factory with the specified image settings
	  */
	def withImageSettings(settings: ViewImageLabelSettings): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Copy of this factory that forces the text and the image to have the same breadth
	  *         (i.e. height or width, depending on alignment)
	  */
	def forcingEqualBreadth = withForceEqualBreadth(force = true)
	/**
	  * @return Copy of this factory that uses low-priority constraints for the image size
	  */
	def withLowPriorityImageSize = withImageUsesLowPrioritySize(lowPriority = true)
	
	/**
	  * @return Copy of this factory that doesn't place any insets around the image
	  */
	def withoutImageInsets = withImageInsets(StackInsets.zero)
	
	/**
	  * insets pointer from the wrapped view image label settings
	  */
	protected def imageInsetsPointer = imageSettings.insetsPointer
	/**
	  * alignment pointer from the wrapped view image label settings
	  */
	protected def imageAlignmentPointer = imageSettings.alignmentPointer
	/**
	  * color overlay pointer from the wrapped view image label settings
	  */
	protected def imageColorOverlayPointer = imageSettings.colorOverlayPointer
	/**
	  * image scaling pointer from the wrapped view image label settings
	  */
	protected def imageScalingPointer = imageSettings.imageScalingPointer
	/**
	  * uses low priority size from the wrapped view image label settings
	  */
	protected def imageUsesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	
	// IMPLEMENTED  ----------------
	
	override def apply(alignment: Alignment): Repr = withImageAlignment(alignment.opposite)
	
	
	// OTHER	--------------------
	
	def mapImageAlignmentPointer(f: Changing[Alignment] => Changing[Alignment]) =
		withImageAlignmentPointer(f(imageAlignmentPointer))
	def mapImageAlignment(f: Alignment => Alignment) = mapImageAlignmentPointer { _.map(f) }
	def mapImageColorOverlayPointer(f: Option[Changing[Color]] => Option[Changing[Color]]) =
		withImageColorOverlayPointer(f(imageColorOverlayPointer))
	def mapImageScalingPointer(f: Changing[Double] => Changing[Double]) =
		withImageScalingPointer(f(imageScalingPointer))
	def mapImageScaling(f: Double => Double) = mapImageScalingPointer { _.map(f) }
	def mapImageInsetsPointer(f: Changing[StackInsets] => Changing[StackInsets]) =
		withImageInsetsPointer(f(imageInsetsPointer))
	def mapImageInsets(f: StackInsets => StackInsets) = mapImageInsetsPointer { _.map(f) }
	
	def mapImageSettings(f: ViewImageLabelSettings => ViewImageLabelSettings) =
		withImageSettings(f(imageSettings))
	
	/**
	  * @param p Pointer that determines the image drawing location within this component
	  * @return Copy of this factory with the specified image alignment pointer
	  */
	def withImageAlignmentPointer(p: Changing[Alignment]) =
		withImageSettings(imageSettings.withAlignmentPointer(p))
	/**
	  * @param alignment Alignment to use when drawing the image
	  * @return Copy of this factory with the specified image-drawing alignment
	  */
	def withImageAlignment(alignment: Alignment) = withImageAlignmentPointer(Fixed(alignment))
	/**
	  * @param p Pointer that, when defined, places a color overlay over the drawn image
	  * @return Copy of this factory with the specified image color overlay pointer
	  */
	def withImageColorOverlayPointer(p: Option[Changing[Color]]) =
		withImageSettings(imageSettings.withColorOverlayPointer(p))
	/**
	  * @param p Pointer that places a color overlay over the drawn image
	  * @return Copy of this factory with the specified image color overlay pointer
	  */
	def withImageColorOverlayPointer(p: Changing[Color]): Repr = withImageColorOverlayPointer(Some(p))
	/**
	  * @param color A color overlay to place above the drawn image
	  * @return Copy of this factory that places a color overlay over the drawn images
	  */
	def withImageColorOverlay(color: Color) = withImageColorOverlayPointer(Fixed(color))
	/**
	  * @param p Pointer that determines image scaling, in addition to the original image scaling
	  * @return Copy of this factory with the specified image image scaling pointer
	  */
	def withImageScalingPointer(p: Changing[Double]) =
		withImageSettings(imageSettings.withImageScalingPointer(p))
	/**
	  * @param scaling Scaling to apply to the drawn image
	  * @return Copy of this factory with the specified image scaling
	  */
	def withImageScaling(scaling: Double) = withImageScalingPointer(Fixed(scaling))
	/**
	  * @param p Pointer that determines the insets placed around the image
	  * @return Copy of this factory with the specified image insets pointer
	  */
	def withImageInsetsPointer(p: Changing[StackInsets]) = withImageSettings(imageSettings
		.withInsetsPointer(p))
	/**
	  * @param insets Insets to place around the image
	  * @return copy of this factory with the specified insets used
	  */
	def withImageInsets(insets: StackInsetsConvertible) = withImageInsetsPointer(Fixed(insets.toInsets))
	/**
	  * @param lowPriority Whether this label should use low priority size constraints
	  * @return Copy of this factory with the specified image uses low priority size
	  */
	def withImageUsesLowPrioritySize(lowPriority: Boolean) =
		withImageSettings(imageSettings.withUseLowPrioritySize(lowPriority))
}

object ViewImageAndTextLabelSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply(imageSettings = ViewImageLabelSettings(alignmentPointer = Fixed(Alignment.Right)))
}
/**
  * Combined settings used when constructing view image and text labels
  * @param forceEqualBreadth Whether the image and the text should be forced to have equal height
  *                          or width (depending on the alignment used)
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageAndTextLabelSettings(customDrawers: Vector[CustomDrawer] = Vector.empty,
                                         imageSettings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                         forceEqualBreadth: Boolean = false)
	extends ViewImageAndTextLabelSettingsLike[ViewImageAndTextLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withForceEqualBreadth(force: Boolean) = copy(forceEqualBreadth = force)
	override def withImageSettings(settings: ViewImageLabelSettings) = copy(imageSettings = settings)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ViewImageAndTextLabelSettings =
		copy(customDrawers = customDrawers)
}

/**
  * Common trait for factories that wrap a view image and text label settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageAndTextLabelSettingsWrapper[+Repr] extends ViewImageAndTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ViewImageAndTextLabelSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ViewImageAndTextLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def customDrawers = settings.customDrawers
	override protected def forceEqualBreadth = settings.forceEqualBreadth
	override protected def imageSettings = settings.imageSettings
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withForceEqualBreadth(force: Boolean) = mapSettings { _.withForceEqualBreadth(force) }
	override def withImageSettings(settings: ViewImageLabelSettings) =
		mapSettings { _.withImageSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewImageAndTextLabelSettings => ViewImageAndTextLabelSettings) = withSettings(f(settings))
}

case class ContextualViewImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy,
                                                  contextPointer: Changing[TextContext],
                                                  settings: ViewImageAndTextLabelSettings = ViewImageAndTextLabelSettings.default,
                                                  drawBackground: Boolean = false)
	extends ViewImageAndTextLabelSettingsWrapper[ContextualViewImageAndTextLabelFactory]
		with VariableBackgroundRoleAssignableFactory[TextContext, ContextualViewImageAndTextLabelFactory]
{
	// IMPLEMENTED  ------------------
	
	override def withContextPointer(p: Changing[TextContext]): ContextualViewImageAndTextLabelFactory =
		copy(contextPointer = p)
	override def withSettings(settings: ViewImageAndTextLabelSettings) =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContextPointer: Changing[TextContext],
	                                                     backgroundDrawer: CustomDrawer): ContextualViewImageAndTextLabelFactory =
		copy(contextPointer = newContextPointer,
			settings = settings.withCustomDrawers(backgroundDrawer +: settings.customDrawers), drawBackground = true)
	
	// When (text) alignment is changed, also changes the image alignment
	override def apply(alignment: Alignment) =
		copy(contextPointer = contextPointer.map { _.withTextAlignment(alignment) },
			settings = settings.withImageAlignment(alignment.opposite))
	
	
	// OTHER    ---------------------
	
	/**
	  * @param p A pointer to the applied color overlay
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that places a color overlay, according to the specified pointer
	  */
	def withImageColorRolePointer(p: Changing[ColorRole], preferredShade: ColorLevel = Standard) =
		withImageColorOverlayPointer(contextPointer.mergeWith(p) { _.color.preferring(preferredShade)(_) })
	/**
	  * @param role The color role to use as image overlay color
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that places a color overlay over the drawn images
	  */
	def withImageColorOverlay(role: ColorRole, preferredShade: ColorLevel): ContextualViewImageAndTextLabelFactory =
		withImageColorRolePointer(Fixed(role), preferredShade)
	/**
	  * @param role           The color role to use as image overlay color
	  * @return Copy of this factory that places a color overlay over the drawn images
	  */
	def withImageColorOverlay(role: ColorRole): ContextualViewImageAndTextLabelFactory =
		withImageColorOverlay(role, Standard)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param imagePointer            A pointer to the image displayed on this label.
	  *                                Left if displaying an icon, Right if displaying an image.
	  * @param displayFunction         Display function used when converting the item to text (default = toString)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def iconOrImage[A](itemPointer: Changing[A], imagePointer: Either[Changing[SingleColorIcon], Changing[Image]],
	                          displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
	{
		val label = new ViewImageAndTextLabel[A](parentHierarchy, contextPointer, itemPointer, imagePointer,
			imageSettings, displayFunction, customDrawers, forceEqualBreadth)
		if (drawBackground)
			contextPointer.addContinuousListener { e =>
				if (e.values.isAsymmetricBy { _.background })
					label.repaint()
			}
		label
	}
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer     A pointer to this label's (text-determining) content
	  * @param imagePointer    A pointer to the image displayed on this label.
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def apply[A](itemPointer: Changing[A], imagePointer: Changing[Image],
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		iconOrImage(itemPointer, Right(imagePointer), displayFunction)
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param iconPointer             A pointer to the displayed icon
	  * @param displayFunction         A function for converting the displayed item to text (default = use toString)
	  * @return A new label
	  */
	def icon[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	            displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		iconOrImage[A](itemPointer, Left(iconPointer), displayFunction)
	@deprecated("Renamed to .icon(...)", "v1.1")
	def withIcon[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		icon(itemPointer, iconPointer, displayFunction)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param iconPointer             A pointer to the displayed icon
	  * @param rolePointer             A pointer to the role the icon serves (determines icon color)
	  * @param preferredShade          Preferred color shade to use (default = standard)
	  * @param displayFunction         A function for converting the displayed item to text (default = use toString)
	  * @return A new label
	  */
	@deprecated("Please use .withImageColorRolePointer(Changing).icon(...) instead", "v1.1")
	def withColouredIcon[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                        rolePointer: Changing[ColorRole], preferredShade: ColorLevel = Standard,
	                        displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		withImageColorRolePointer(rolePointer, preferredShade).icon(itemPointer, iconPointer, displayFunction)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param iconPointer             A pointer to the displayed icon
	  * @param rolePointer             A pointer to the color role used in label background
	  * @param preferredShade          Preferred color shade to use (default = standard)
	  * @param displayFunction         Display function used when converting the item to text (default = toString)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	@deprecated("Please use .withBackgroundRolePointer(...).icon(...) instead", "v1.1")
	def withIconAndChangingBackground[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                                     rolePointer: Changing[ColorRole], preferredShade: ColorLevel = Standard,
	                                     displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		withBackgroundRolePointer(rolePointer, preferredShade).icon(itemPointer, iconPointer, displayFunction)
}

/**
  * Used for defining view image and text label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ViewImageAndTextLabelSetup(settings: ViewImageAndTextLabelSettings = ViewImageAndTextLabelSettings.default)
	extends ViewImageAndTextLabelSettingsWrapper[ViewImageAndTextLabelSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualViewImageAndTextLabelFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualViewImageAndTextLabelFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: ViewImageAndTextLabelSettings) =
		copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualViewImageAndTextLabelFactory(hierarchy, context, settings)
}

object ViewImageAndTextLabel extends ViewImageAndTextLabelSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ViewImageAndTextLabelSettings) = withSettings(settings)
}

/**
  * A pointer-based label that displays an image and a piece of text
  * @author Mikko Hilpinen
  * @since 9.11.2020, v0.1
  */
class ViewImageAndTextLabel[A](parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                               val itemPointer: Changing[A],
                               imgPointer: Either[Changing[SingleColorIcon], Changing[Image]],
                               imageSettings: ViewImageLabelSettings,
                               displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                               additionalDrawers: Vector[CustomDrawer] = Vector(),
                               forceEqualBreadth: Boolean = false)
	extends ReachComponentWrapper with ConstrainableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	override protected val wrapped = {
		// TODO: Uses a static context here
		// TODO: Allow margin customization
		Stack.withContext(parentHierarchy, contextPointer.value)
			.withoutMargin.withCustomDrawers(additionalDrawers)
			.buildPair(Mixed, contextPointer.value.textAlignment, forceFitLayout = forceEqualBreadth) { factories =>
				val imageLabel = factories(ViewImageLabel).withContextPointer(contextPointer)
					.withSettings(imageSettings)
					.iconOrImagePointer(imgPointer)
				val textLabel = factories(ViewTextLabel).withContextPointer(contextPointer)
					.apply(itemPointer, displayFunction)
				Pair(imageLabel, textLabel)
			}
	}
}

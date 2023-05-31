package utopia.reach.component.label.image

import utopia.firmament.component.stack.ConstrainableWrapper
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.component.wrapper.Open
import utopia.reach.container.multi.Stack
import utopia.reach.drawing.Priority.Low

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
	
	val default = apply()
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

/**
  * Common trait for factories that are used for constructing view image and text labels
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageAndTextLabelFactoryLike[+Repr] extends ViewImageAndTextLabelSettingsWrapper[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The component hierarchy, to which created view image and text labels will be attached
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	
	// OTHER    -------------------
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param imagePointer            A pointer to the image displayed on this label
	  * @param fontPointer             A pointer to the font used in the text
	  * @param alignment               Alignment used when placing the image next to the text.
	  *                                I.e. Left alignment would place the image on the left side of the text.
	  *                                (default = opposite to image alignment)
	  * @param displayFunction         Display function used when converting the item to text (default = toString)
	  * @param textColorPointer        A pointer to the color used when drawing text (default = always standard black)
	  * @param textInsetsPointer       A pointer to insets placed around the text (default = any, preferring 0)
	  * @param betweenLinesMargin      Vertical margin placed between text lines (default = 0)
	  * @param allowLineBreaks         Whether text should be allowed to use line breaks (default = true)
	  * @param allowImageUpscaling     Whether image should be allowed to scale up to its source resolution (default = true)
	  * @param allowTextShrink         Whether text should be allowed to shrink to conserve space (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	protected def _apply[A](itemPointer: Changing[A], imagePointer: Changing[Image],
	                        fontPointer: Changing[Font], textColorPointer: Changing[Color] = Fixed(Color.textBlack),
	                        textInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	                        alignment: Alignment = imageSettings.alignmentPointer.value.opposite,
	                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                        betweenLinesMargin: Double = 0.0, allowLineBreaks: Boolean = true,
	                        allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
	                        disableColorOverlay: Boolean = false) =
	{
		// Applies image scaling and coloring, if defined
		val trueImagePointer = {
			// Image coloring may be disabled (in which case it is already assumed to have been applied)
			val colored = {
				// Case: Coloring disabled
				if (disableColorOverlay)
					imagePointer
				else
					imageColorOverlayPointer match {
						// Case: Coloring specified
						case Some(colorPointer) => imagePointer.mergeWith(colorPointer) { _ withColorOverlay _ }
						// Case: No coloring specified
						case None => imagePointer
					}
			}
			// Applies image scaling, also
			colored.mergeWith(imageScalingPointer) { _ * _ }
		}
		new ViewImageAndTextLabel[A](parentHierarchy, itemPointer, trueImagePointer, imageSettings, fontPointer,
			textColorPointer, textInsetsPointer, alignment, displayFunction, betweenLinesMargin, customDrawers,
			allowLineBreaks, allowImageUpscaling, allowTextShrink, forceEqualBreadth)
	}
}

case class ContextualViewImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                                  settings: ViewImageAndTextLabelSettings = ViewImageAndTextLabelSettings.default)
	extends ViewImageAndTextLabelFactoryLike[ContextualViewImageAndTextLabelFactory]
		with TextContextualFactory[ContextualViewImageAndTextLabelFactory]
		with ContextualBackgroundAssignableFactory[TextContext, ContextualViewImageAndTextLabelFactory]
{
	// COMPUTED ----------------------
	
	private implicit def c: TextContext = context
	
	/**
	  * @return A copy of this factory that doesn't utilize component creation context
	  */
	def withoutContext = ViewImageAndTextLabelFactory(parentHierarchy, settings)
	
	
	// IMPLEMENTED  ------------------
	
	override def self: ContextualViewImageAndTextLabelFactory = this
	
	override def withContext(newContext: TextContext) =
		copy(context = newContext)
	override def withSettings(settings: ViewImageAndTextLabelSettings) =
		copy(settings = settings)
	
	// When (text) alignment is changed, also changes the image alignment
	override def apply(alignment: Alignment) =
		copy(context = context.withTextAlignment(alignment), settings = settings.withImageAlignment(alignment.opposite))
	override def withTextAlignment(alignment: Alignment) = apply(alignment)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param p A pointer to the applied color overlay
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that places a color overlay, according to the specified pointer
	  */
	def withImageColorRolePointer(p: Changing[ColorRole], preferredShade: ColorLevel = Standard) =
		withImageColorOverlayPointer(p.map { r => context.color.preferring(preferredShade)(r) })
	/**
	  * @param role The color role to use as image overlay color
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that places a color overlay over the drawn images
	  */
	def withImageColorOverlay(role: ColorRole, preferredShade: ColorLevel): ContextualViewImageAndTextLabelFactory = {
		val color = context.color.preferring(preferredShade)(role)
		withImageColorOverlay(color)
	}
	/**
	  * @param role           The color role to use as image overlay color
	  * @return Copy of this factory that places a color overlay over the drawn images
	  */
	def withImageColorOverlay(role: ColorRole): ContextualViewImageAndTextLabelFactory =
		withImageColorOverlay(role, Standard)
	
	/**
	  * Creates a new label which displays both image and text.
	  * Please note that this method won't support automatic adjustments to variable background color
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param imagePointer            A pointer to the image displayed on this label
	  * @param fontPointer             A pointer to the font used in the text (default = determined by context)
	  * @param textColorPointer        A pointer to the color used when drawing text (default = determined by context)
	  * @param textInsetsPointer       A pointer to the insets placed around the text (default = determined by context)
	  * @param displayFunction         Display function used when converting the item to text (default = toString)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def withChangingStyle[A](itemPointer: Changing[A], imagePointer: Changing[Image],
	                         fontPointer: Changing[Font] = Fixed(context.font),
	                         textColorPointer: Changing[Color] = Fixed(context.textColor),
	                         textInsetsPointer: Changing[StackInsets] = Fixed(context.textInsets),
	                         displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		_apply[A](itemPointer, imagePointer, fontPointer, textColorPointer, textInsetsPointer,
			context.textAlignment, displayFunction, context.betweenLinesMargin.optimal, context.allowLineBreaks,
			context.allowImageUpscaling, context.allowTextShrink)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param imagePointer            A pointer to the image displayed on this label
	  * @param displayFunction         Display function used when converting the item to text (default = toString)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def apply[A](itemPointer: Changing[A], imagePointer: Changing[Image],
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		withChangingStyle[A](itemPointer, imagePointer, displayFunction = displayFunction)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer             A pointer to this label's (text-determining) content
	  * @param iconPointer             A pointer to the displayed icon
	  * @param displayFunction         A function for converting the displayed item to text (default = use toString)
	  * @return A new label
	  */
	def icon[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
	{
		val imagePointer = imageColorOverlayPointer match {
			case Some(colorPointer) => iconPointer.mergeWith(colorPointer) { _(_) }
			case None => iconPointer.map { _.contextual }
		}
		_apply[A](itemPointer, imagePointer, Fixed(context.font), Fixed(context.textColor), Fixed(context.textInsets),
			context.textAlignment, displayFunction, context.betweenLinesMargin.optimal, context.allowLineBreaks,
			context.allowImageUpscaling, context.allowTextShrink, disableColorOverlay = true)
	}
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
	  * @param imageInsets             Insets placed around the image (default = any, preferring 0)
	  * @param displayFunction         Display function used when converting the item to text (default = toString)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def withIconAndChangingBackground[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                                     rolePointer: Changing[ColorRole], preferredShade: ColorLevel = Standard,
	                                     imageInsets: StackInsets = StackInsets.any,
	                                     displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
	{
		val backgroundPointer = rolePointer.map { context.color.preferring(preferredShade)(_) }
		val backgroundDrawer = BackgroundViewDrawer(backgroundPointer.map { c => c })
		val imagePointer = iconPointer.mergeWith(backgroundPointer) { _.against(_) }
		val label = withCustomDrawer(backgroundDrawer)._apply(itemPointer, imagePointer, Fixed(context.font),
			backgroundPointer.map { _.shade.defaultTextColor }, Fixed(context.textInsets), context.textAlignment,
			displayFunction, context.betweenLinesMargin.optimal, context.allowLineBreaks, context.allowImageUpscaling,
			context.allowTextShrink, disableColorOverlay = true)
		// Repaints this component whenever background color changes
		backgroundPointer.addContinuousAnyChangeListener { label.repaint(Low) }
		label
	}
}

case class ViewImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy,
                                        settings: ViewImageAndTextLabelSettings = ViewImageAndTextLabelSettings.default)
	extends ViewImageAndTextLabelFactoryLike[ViewImageAndTextLabelFactory]
		with FromContextFactory[TextContext, ContextualViewImageAndTextLabelFactory]
{
	override def withSettings(settings: ViewImageAndTextLabelSettings): ViewImageAndTextLabelFactory =
		copy(settings = settings)
	
	override def withContext(context: TextContext) =
		ContextualViewImageAndTextLabelFactory(parentHierarchy, context, settings)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param imagePointer A pointer to the image displayed on this label
	  * @param fontPointer A pointer to the font used in the text
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param textColorPointer A pointer to the color used when drawing text (default = always standard black)
	  * @param textInsetsPointer A pointer to insets placed around the text (default = any, preferring 0)
	  * @param alignment Alignment used for the <b>text</b> (the image will be placed with the opposite alignment,
	  *                  so that the two form a close pair) (default = Left)
	  * @param betweenLinesMargin Vertical margin placed between text lines (default = 0)
	  * @param allowLineBreaks Whether text should be allowed to use line breaks (default = true)
	  * @param allowImageUpscaling Whether image should be allowed to scale up to its source resolution (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def apply[A](itemPointer: Changing[A], imagePointer: Changing[Image], fontPointer: Changing[Font],
	             textColorPointer: Changing[Color] = Fixed(Color.textBlack),
	             textInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	             alignment: Alignment = Alignment.Left,
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	             betweenLinesMargin: Double = 0.0,
	             allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
	             allowTextShrink: Boolean = false) =
		_apply[A](itemPointer, imagePointer, fontPointer, textColorPointer,
			textInsetsPointer, alignment, displayFunction, betweenLinesMargin, allowLineBreaks, allowImageUpscaling,
			allowTextShrink)
}

object ViewImageAndTextLabel extends Cff[ViewImageAndTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewImageAndTextLabelFactory(hierarchy)
}

/**
  * A pointer-based label that displays an image and a piece of text
  * @author Mikko Hilpinen
  * @since 9.11.2020, v0.1
  */
// TODO: Alignment doesn't adjust to image alignment changes at this time
// FIXME: Don't apply color overlay and scaling here, because they're applied in the underlying image label
// TODO: Accept Image or Icon pointer
class ViewImageAndTextLabel[A](parentHierarchy: ComponentHierarchy, val itemPointer: Changing[A],
                               val imagePointer: Changing[Image], imageSettings: ViewImageLabelSettings,
                               fontPointer: Changing[Font], textColorPointer: Changing[Color] = Fixed(Color.textBlack),
                               textInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                               alignment: Alignment = Alignment.Left,
                               displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                               betweenLinesMargin: Double = 0.0, additionalDrawers: Vector[CustomDrawer] = Vector(),
                               allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
                               allowTextShrink: Boolean = false, forceEqualBreadth: Boolean = false)
	extends ReachComponentWrapper with ConstrainableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	private val stylePointer = new PointerWithEvents[TextDrawContext](updatedStyle)
	private val updateStyleListener: ChangeListener[Any] = _ => stylePointer.value = updatedStyle
	
	override protected val wrapped = {
		// Creates stack content (image and text label)
		val openItems = Open { hierarchy =>
			val imageLabel = ViewImageLabel(imageSettings)(hierarchy)
				.copy(allowsUpscaling = allowImageUpscaling)
				.apply(imagePointer)
			val textLabel = ViewTextLabel(hierarchy).apply(itemPointer, stylePointer, displayFunction,
				allowTextShrink = allowTextShrink)
			Pair(imageLabel, textLabel)
		}(parentHierarchy.top)
		// Wraps the components in a stack
		// TODO: Build instead of use Open (optional)
		Stack(parentHierarchy).withoutMargin.withCustomDrawers(additionalDrawers)
			.forPair(openItems, alignment, forceFitLayout = forceEqualBreadth)
			.parent
	}
	
	
	// INITIAL CODE	--------------------------------
	
	fontPointer.addListener(updateStyleListener)
	textColorPointer.addListener(updateStyleListener)
	textInsetsPointer.addListener(updateStyleListener)
	
	
	// COMPUTED	-------------------------------------
	
	private def updatedStyle = TextDrawContext(fontPointer.value, textColorPointer.value, alignment,
		textInsetsPointer.value, betweenLinesMargin, allowLineBreaks)
}

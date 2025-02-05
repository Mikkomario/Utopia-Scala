package utopia.reach.component.label.image

import utopia.firmament.component.stack.ConstrainableWrapper
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Medium, Small}
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.UnresolvedFramedFactory.{UnresolvedStackInsets, sides}
import utopia.reach.component.factory.contextual.VariableBackgroundRoleAssignableFactory
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageAndTextLabelSettings.defaultImageSettings
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.container.multi.Stack

/**
  * Common trait for view image and text label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageAndTextLabelSettingsLike[+Repr] extends ImageAndTextLabelSettingsLike[ViewImageLabelSettings, Repr]
{
	// ABSTRACT --------------------
	
	/**
	  * @return A pointer that determines whether this label is a hint or a fully visible / standard label
	  */
	def hintFlag: Flag
	/**
	  * @param p A pointer that determines whether this is a hint label (true) or a regular label (false)
	  * @return Copy of this factory that uses the specified hint pointer
	  */
	def withHintFlag(p: Flag): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * insets pointer from the wrapped view image label settings
	  */
	def imageInsetsPointer = imageSettings.insetsPointer
	/**
	  * alignment pointer from the wrapped view image label settings
	  */
	def imageAlignmentPointer = imageSettings.alignmentPointer
	/**
	  * color overlay pointer from the wrapped view image label settings
	  */
	def imageColorOverlayPointer = imageSettings.colorOverlayPointer
	/**
	  * image scaling pointer from the wrapped view image label settings
	  */
	def imageScalingPointer = imageSettings.imageScalingPointer
	
	@deprecated("Renamed to .hintFlag", "v1.6")
	def isHintPointer = hintFlag
	
	
	// IMPLEMENTED  ----------------
	
	override def isHint: Boolean = hintFlag.value
	override def withIsHint(isHint: Boolean): Repr = withHintFlag(Fixed(isHint))
	
	override def mapImageAlignment(f: Alignment => Alignment) = mapImageAlignmentPointer { _.map(f) }
	override def mapImageScaling(f: Double => Double) = mapImageScalingPointer { _.map(f) }
	override def mapImageInsets(f: StackInsets => StackInsetsConvertible) =
		mapImageInsetsPointer { _.map { f(_).toInsets } }
	
	override def withImageAlignment(alignment: Alignment) = withImageAlignmentPointer(Fixed(alignment))
	override def withImageScaling(scaling: Double) = withImageScalingPointer(Fixed(scaling))
	override def withImageColorOverlay(color: Option[Color]) = withImageColorOverlayPointer(color.map(Fixed.apply))
	
	// OTHER	--------------------
	
	def mapHintFlag(f: Mutate[Flag]) = withHintFlag(f(hintFlag))
	def mapImageAlignmentPointer(f: Changing[Alignment] => Changing[Alignment]) =
		withImageAlignmentPointer(f(imageAlignmentPointer))
	def mapImageColorOverlayPointer(f: Option[Changing[Color]] => Option[Changing[Color]]) =
		withImageColorOverlayPointer(f(imageColorOverlayPointer))
	def mapImageScalingPointer(f: Changing[Double] => Changing[Double]) =
		withImageScalingPointer(f(imageScalingPointer))
	def mapImageInsetsPointer(f: Changing[StackInsets] => Changing[StackInsets]) =
		withImageInsetsPointer(f(imageInsetsPointer))
	
	/**
	  * @param p Pointer that determines the image drawing location within this component
	  * @return Copy of this factory with the specified image alignment pointer
	  */
	def withImageAlignmentPointer(p: Changing[Alignment]) =
		withImageSettings(imageSettings.withAlignmentPointer(p))
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
	
	@deprecated("Please use .withHintFlag(Flag) instead", "v1.6")
	def withIsHintPointer(p: Changing[Boolean]) = withHintFlag(p)
	@deprecated("Please use .mapHintFlag(...) instead", "v1.6")
	def mapIsHintPointer(f: Changing[Boolean] => Changing[Boolean]) = mapHintFlag { f(_) }
}

object ViewImageAndTextLabelSettings
{
	// ATTRIBUTES	--------------------
	
	private val defaultImageSettings = ViewImageLabelSettings(alignmentPointer = Fixed(Alignment.Right))
	
	val default = apply()
}
/**
  * Combined settings used when constructing view image and text labels
  * @param forceEqualBreadth Whether the image and the text should be forced to have equal height
  *                          or width (depending on the alignment used)
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageAndTextLabelSettings(customDrawers: Seq[CustomDrawer] = Empty,
                                         imageSettings: ViewImageLabelSettings = defaultImageSettings,
                                         separatingMargin: Option[SizeCategory] = Some(Small),
                                         insets: UnresolvedStackInsets = sides.symmetric(Left(Medium)),
                                         hintFlag: Flag = AlwaysFalse,
                                         forceEqualBreadth: Boolean = false)
	extends ViewImageAndTextLabelSettingsLike[ViewImageAndTextLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withHintFlag(p: Flag): ViewImageAndTextLabelSettings = copy(hintFlag = p)
	override def withSeparatingMargin(margin: Option[SizeCategory]): ViewImageAndTextLabelSettings =
		copy(separatingMargin = margin)
	override def withForceEqualBreadth(force: Boolean) = copy(forceEqualBreadth = force)
	override def withImageSettings(settings: ViewImageLabelSettings) = copy(imageSettings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ViewImageAndTextLabelSettings =
		copy(customDrawers = drawers)
	override def withInsets(insets: UnresolvedStackInsets): ViewImageAndTextLabelSettings = copy(insets = insets)
	
	override protected def _withMargins(separatingMargin: Option[SizeCategory],
	                                    insets: UnresolvedStackInsets): ViewImageAndTextLabelSettings =
		copy(separatingMargin = separatingMargin, insets = insets)
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
	override def forceEqualBreadth = settings.forceEqualBreadth
	override def imageSettings = settings.imageSettings
	override def hintFlag = settings.hintFlag
	override def separatingMargin: Option[SizeCategory] = settings.separatingMargin
	override def insets: UnresolvedStackInsets = settings.insets
	
	override def withHintFlag(p: Flag): Repr = mapSettings { _.withHintFlag(p) }
	override def withSeparatingMargin(margin: Option[SizeCategory]): Repr =
		mapSettings { _.withSeparatingMargin(margin) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withForceEqualBreadth(force: Boolean) = mapSettings { _.withForceEqualBreadth(force) }
	override def withImageSettings(settings: ViewImageLabelSettings) =
		mapSettings { _.withImageSettings(settings) }
	override def withInsets(insets: UnresolvedStackInsets): Repr = mapSettings { _.withInsets(insets) }
	
	override protected def _withMargins(separatingMargin: Option[SizeCategory], insets: UnresolvedStackInsets): Repr =
		mapSettings { _.copy(separatingMargin = separatingMargin, insets = insets) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewImageAndTextLabelSettings => ViewImageAndTextLabelSettings) = withSettings(f(settings))
}

case class ContextualViewImageAndTextLabelFactory(hierarchy: ComponentHierarchy,
                                                  context: VariableTextContext,
                                                  settings: ViewImageAndTextLabelSettings = ViewImageAndTextLabelSettings.default,
                                                  drawBackground: Boolean = false)
	extends ViewImageAndTextLabelSettingsWrapper[ContextualViewImageAndTextLabelFactory]
		with VariableBackgroundRoleAssignableFactory[VariableTextContext, ContextualViewImageAndTextLabelFactory]
		with PartOfComponentHierarchy
{
	// COMPUTED ----------------------
	
	private def resolveInsets = resolveInsetsIn(context)
	
	
	// IMPLEMENTED  ------------------
	
	override def withContext(p: VariableTextContext): ContextualViewImageAndTextLabelFactory = copy(context = p)
	override def withSettings(settings: ViewImageAndTextLabelSettings) =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContext: VariableTextContext,
	                                                     backgroundDrawer: CustomDrawer): ContextualViewImageAndTextLabelFactory =
		copy(context = newContext, settings = settings.withCustomDrawers(backgroundDrawer +: settings.customDrawers),
			drawBackground = true)
	
	// When (text) alignment is changed, also changes the image alignment
	override def apply(alignment: Alignment) =
		copy(context = context.withTextAlignment(alignment), settings = settings.withImageAlignment(alignment.opposite))
	
	
	// OTHER    ---------------------
	
	/**
	  * @param p A pointer to the applied color overlay
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @return Copy of this factory that places a color overlay, according to the specified pointer
	  */
	def withImageColorRolePointer(p: Changing[ColorRole], preferredShade: ColorLevel = Standard) =
		withImageColorOverlayPointer(context.colorPointer.preferring(preferredShade).forRole(p))
	
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
		val label = new ViewImageAndTextLabel[A](hierarchy, context, itemPointer, imagePointer,
			settings, Fixed(resolveInsets), displayFunction)
		if (drawBackground)
			context.backgroundPointer.addContinuousAnyChangeListener { label.repaint() }
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
		with FromContextComponentFactoryFactory[VariableTextContext, ContextualViewImageAndTextLabelFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext) =
		ContextualViewImageAndTextLabelFactory(hierarchy, context, settings)
	
	override def withSettings(settings: ViewImageAndTextLabelSettings) =
		copy(settings = settings)
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
class ViewImageAndTextLabel[A](override val hierarchy: ComponentHierarchy, context: VariableTextContext,
                               val itemPointer: Changing[A],
                               imgPointer: Either[Changing[SingleColorIcon], Changing[Image]],
                               settings: ViewImageAndTextLabelSettings,
                               commonInsetsPointer: Changing[StackInsets],
                               displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	extends ReachComponentWrapper with ConstrainableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	override protected val wrapped = {
		// Calculates the actual insets for the image & text label
		val textAlignment = context.textAlignment
		val imageAlignment = textAlignment.opposite
		
		val imageInsetsPointer = settings.imageInsetsPointer
			.mergeWith(commonInsetsPointer) { (a, b) => (a max b) -- imageAlignment.directions }
		val appliedContext = context
			.mapTextInsetsPointer { _.mergeWith(commonInsetsPointer) { (textInsets, commonInsets) =>
				(textInsets max commonInsets) -- textAlignment.directions
			} }
		
		Stack.withContext(hierarchy, appliedContext)
			.withMargin(settings.separatingMargin)
			.withCustomDrawers(settings.customDrawers)
			.buildPair(Mixed, textAlignment, forceFitLayout = settings.forceEqualBreadth) { factories =>
				val imageLabel = factories(ViewImageLabel)
					.withSettings(settings.imageSettings)
					.withInsetsPointer(imageInsetsPointer)
					.iconOrImagePointer(imgPointer)
				val textLabel = factories(ViewTextLabel)
					.apply(itemPointer, displayFunction)
				Pair(imageLabel, textLabel)
			}
	}
}

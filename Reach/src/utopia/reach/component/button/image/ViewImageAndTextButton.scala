package utopia.reach.component.button.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.FromContextComponentFactoryFactory
import utopia.reach.component.factory.UnresolvedFramedFactory.UnresolvedStackInsets
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ViewImageAndTextLabel, ViewImageAndTextLabelSettings, ViewImageAndTextLabelSettingsLike, ViewImageLabelSettings}
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for view image and text button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ViewImageAndTextButtonSettingsLike[+Repr]
	extends ButtonSettingsLike[Repr] with ViewImageAndTextLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	protected def buttonSettings: ButtonSettings
	protected def labelSettings: ViewImageAndTextLabelSettings
	
	/**
	  * @param settings New button settings to use.
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	def withLabelSettings(settings: ViewImageAndTextLabelSettings): Repr
	
	
	// IMPLEMENTED  -----------------
	
	def enabledPointer = buttonSettings.enabledPointer
	def hotKeys = buttonSettings.hotKeys
	def focusListeners = buttonSettings.focusListeners
	
	override def isHintPointer: Changing[Boolean] = labelSettings.isHintPointer
	override def imageSettings: ViewImageLabelSettings = labelSettings.imageSettings
	override def separatingMargin: Option[SizeCategory] = labelSettings.separatingMargin
	override def forceEqualBreadth: Boolean = labelSettings.forceEqualBreadth
	override def customDrawers: Vector[CustomDrawer] = labelSettings.customDrawers
	override def insets: UnresolvedStackInsets = labelSettings.insets
	
	def withEnabledPointer(p: Changing[Boolean]) = withButtonSettings(buttonSettings.withEnabledPointer(p))
	def withFocusListeners(listeners: Vector[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	
	override def withIsHintPointer(p: Changing[Boolean]): Repr = mapLabelSettings { _.withIsHintPointer(p) }
	override def withSeparatingMargin(margin: Option[SizeCategory]): Repr =
		mapLabelSettings { _.withSeparatingMargin(margin) }
	override def withForceEqualBreadth(force: Boolean): Repr = mapLabelSettings { _.withForceEqualBreadth(force) }
	override def withImageSettings(settings: ViewImageLabelSettings): Repr =
		mapLabelSettings { _.withImageSettings(settings) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr =
		mapLabelSettings { _.withCustomDrawers(drawers) }
	override def withInsets(insets: UnresolvedStackInsets): Repr = mapLabelSettings { _.withInsets(insets) }
	
	override protected def _withMargins(separatingMargin: Option[SizeCategory], insets: UnresolvedStackInsets): Repr =
		mapLabelSettings { _.copy(separatingMargin = separatingMargin, insets = insets) }
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
	def mapLabelSettings(f: Mutate[ViewImageAndTextLabelSettings]) = withLabelSettings(f(labelSettings))
}

object ViewImageAndTextButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing view image and text buttons
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ViewImageAndTextButtonSettings(buttonSettings: ButtonSettings = ButtonSettings.default,
                                          labelSettings: ViewImageAndTextLabelSettings = ViewImageAndTextLabelSettings.default)
	extends ViewImageAndTextButtonSettingsLike[ViewImageAndTextButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withLabelSettings(settings: ViewImageAndTextLabelSettings): ViewImageAndTextButtonSettings =
		copy(labelSettings = settings)
}

/**
  * Common trait for factories that wrap a view image and text button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ViewImageAndTextButtonSettingsWrapper[+Repr] extends ViewImageAndTextButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ViewImageAndTextButtonSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ViewImageAndTextButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	
	override protected def labelSettings: ViewImageAndTextLabelSettings = settings.labelSettings
	override def withLabelSettings(settings: ViewImageAndTextLabelSettings): Repr =
		mapSettings { _.withLabelSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewImageAndTextButtonSettings => ViewImageAndTextButtonSettings) =
		withSettings(f(settings))
}

/**
  * Factory class used for constructing view image and text buttons using contextual component
  * creation information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ContextualViewImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy,
                                                   contextPointer: Changing[TextContext],
                                                   settings: ViewImageAndTextButtonSettings = ViewImageAndTextButtonSettings.default)
	extends ViewImageAndTextButtonSettingsWrapper[ContextualViewImageAndTextButtonFactory]
		with VariableContextualFactory[TextContext, ContextualViewImageAndTextButtonFactory]
		with FromAlignmentFactory[ContextualViewImageAndTextButtonFactory]
{
	// IMPLEMENTED  --------------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: ViewImageAndTextButtonSettings) =
		copy(settings = settings)
	
	override def apply(alignment: Alignment) = copy(
		contextPointer = contextPointer.map { _.withTextAlignment(alignment) },
		settings = settings.withImageAlignment(alignment.opposite)
	)
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new button
	  * @param contentPointer (Textual) content to display on this button
	  * @param imagesPointer Pointer to the displayed button images
	  * @param displayFunction Function which converts the button content to text. Default = use .toString
	  * @param action Action that should be performed when this button is pressed.
	  *               Accepts the current content of this button.
	  * @tparam A Type of button content
	  * @return A new button
	  */
	def apply[A](contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	            (action: A => Unit) =
		new ViewImageAndTextButton[A](parentHierarchy, contextPointer, contentPointer, imagesPointer, settings,
			displayFunction)(action)
	
	/**
	  * Creates a new button
	  * @param contentPointer  (Textual) content to display on this button
	  * @param iconPointer Icon to display on this button
	  * @param displayFunction Function which converts the button content to text. Default = use .toString
	  * @param action          Action that should be performed when this button is pressed.
	  *                        Accepts the current content of this button.
	  * @tparam A Type of button content
	  * @return A new button
	  */
	def icon[A](contentPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	            displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	           (action: A => Unit) =
		apply[A](contentPointer, iconPointer.mergeWith(contextPointer) { _.inButton.contextual(_) },
			displayFunction)(action)
	@deprecated("Please use .icon(...) instead", "v1.1")
	def withIcon[A](contentPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	               (action: A => Unit) =
		icon[A](contentPointer, iconPointer, displayFunction)(action)
	
	/**
	  * Creates a new button
	  * @param textPointer Pointer to the text to display
	  * @param imagesPointer Pointer to the button images to use
	  * @param action Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def text[U](textPointer: Changing[LocalizedString], imagesPointer: Changing[ButtonImageSet])
	           (action: => U) =
		apply[LocalizedString](textPointer, imagesPointer, DisplayFunction.identity) { _ => action }
	/**
	  * Creates a new button
	  * @param textPointer   Pointer to the text to display
	  * @param iconPointer Pointer to the icon to display on this button
	  * @param action        Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def textAndIcon[U](textPointer: Changing[LocalizedString], iconPointer: Changing[SingleColorIcon])
	                         (action: => U) =
		icon(textPointer, iconPointer, DisplayFunction.identity) { _ => action }
	
	/**
	  * Creates a new button
	  * @param text   Text to display
	  * @param images Button images to use
	  * @param action        Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def fixedText[U](text: LocalizedString, images: ButtonImageSet)(action: => U) =
		this.text(Fixed(text), Fixed(images))(action)
	/**
	  * Creates a new button
	  * @param text   Text to display
	  * @param icon Icon to display
	  * @param action Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def fixedTextAndIcon[U](text: LocalizedString, icon: SingleColorIcon)(action: => U) =
		textAndIcon(Fixed(text), Fixed(icon))(action)
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param imagesPointer Pointer to the displayed image set
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	@deprecated("Please use .fixedText or .text instead", "v1.1")
	def withStaticText(text: LocalizedString, imagesPointer: Changing[ButtonImageSet])(action: => Unit) =
		this.text(Fixed(text), imagesPointer)(action)
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param icon Icon displayed on this button
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	@deprecated("Renamed to .fixedTextAndIcon(...)", "v1.1")
	def withStaticTextAndIcon(text: LocalizedString, icon: SingleColorIcon)(action: => Unit) =
		fixedTextAndIcon(text, icon)(action)
}

/**
  * Used for defining view image and text button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ViewImageAndTextButtonSetup(settings: ViewImageAndTextButtonSettings = ViewImageAndTextButtonSettings.default)
	extends ViewImageAndTextButtonSettingsWrapper[ViewImageAndTextButtonSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualViewImageAndTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualViewImageAndTextButtonFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: ViewImageAndTextButtonSettings) =
		copy(settings = settings)
	
	
	// OTHER    ----------------------
	
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualViewImageAndTextButtonFactory(hierarchy, context, settings)
}

object ViewImageAndTextButton extends ViewImageAndTextButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ViewImageAndTextButtonSettings) = withSettings(settings)
}

/**
  * A pointer-based button that displays both an image and text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v0.1
  */
class ViewImageAndTextButton[A](parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                                contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
                                settings: ViewImageAndTextButtonSettings,
                                displayFunction: DisplayFunction[A] = DisplayFunction.raw)
                               (action: A => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new EventfulPointer(GuiElementStatus.identity)
	override val statePointer = baseStatePointer
		.mergeWithWhile(settings.enabledPointer, parentHierarchy.linkPointer) { (state, enabled) =>
			state + (Disabled -> !enabled)
		}
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
	override val focusId = hashCode()
	
	/**
	  * A pointer that refers to this button's main color
	  */
	val colorPointer = contextPointer.mapWhile(parentHierarchy.linkPointer) { _.background }
	
	override protected val wrapped = {
		// Adds (fixed) space for borders
		val initialBorderWidth = contextPointer.value.buttonBorderWidth
		val appliedLabelSettings = settings.labelSettings
			.mapInsets { _.map { _.mapBoth { _.more } { _ + initialBorderWidth } } }
		
		val imagePointer = imagesPointer.mergeWith(statePointer) { _(_) }
		
		ViewImageAndTextLabel.withContext(parentHierarchy, contextPointer).withSettings(appliedLabelSettings)
			.withCustomBackgroundDrawer(ButtonBackgroundViewDrawer(colorPointer, statePointer,
				contextPointer.mapWhile(parentHierarchy.linkPointer) { _.buttonBorderWidth }))
			.apply(contentPointer, imagePointer, displayFunction)
	}
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return This button's current background color
	  */
	def color = colorPointer.value
	
	
	// INITIAL CODE	------------------------------
	
	setup(baseStatePointer, settings.hotKeys)
	colorPointer.addContinuousAnyChangeListener { repaint() }
	
	
	// IMPLEMENTED	------------------------------
	
	override def enabledPointer: FlagLike = settings.enabledPointer
	
	override protected def trigger() = action(contentPointer.value)
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}

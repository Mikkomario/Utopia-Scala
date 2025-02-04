package utopia.reach.component.label.empty

import utopia.firmament.context.color.{ColorContext, ColorContextPropsView}
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.{ContextualFactory, ContextualVariableBackgroundAssignable}
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextFactory, VariableBackgroundAssignable}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy}

object ViewEmptyLabel extends Cff[ViewEmptyLabelFactory] with ViewEmptyLabelSettingsWrapper[ViewEmptyLabelSetup]
{
	override protected def settings: ViewEmptyLabelSettings = ViewEmptyLabelSettings.default
	
	override def apply(hierarchy: ComponentHierarchy) = ViewEmptyLabelFactory(hierarchy)
	
	override protected def withSettings(settings: ViewEmptyLabelSettings): ViewEmptyLabelSetup =
		ViewEmptyLabelSetup(settings)
}

trait ViewEmptyLabelSettingsLike[+Repr] extends VariableBackgroundAssignable[Repr]
{
	// ABSTRACT ----------------------------
	
	protected def backgroundPointer: Option[Either[Color, Changing[Color]]]
	protected def drawersPointer: Changing[Seq[CustomDrawer]]
	
	def withCustomDrawers(drawersPointer: Changing[Seq[CustomDrawer]]): Repr
	
	
	// OTHER    --------------------------
	
	def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr = withCustomDrawers(Fixed(drawers))
	def withCustomDrawer(drawer: CustomDrawer) = withCustomDrawers(drawersPointer.map { _ :+ drawer })
}

object ViewEmptyLabelSettings
{
	val default = apply()
}

case class ViewEmptyLabelSettings(backgroundPointer: Option[Either[Color, Changing[Color]]] = None,
                                  drawersPointer: Changing[Seq[CustomDrawer]] = Fixed(Empty))
	extends ViewEmptyLabelSettingsLike[ViewEmptyLabelSettings]
{
	override def withCustomDrawers(drawersPointer: Changing[Seq[CustomDrawer]]): ViewEmptyLabelSettings =
		copy(drawersPointer = drawersPointer)
	override def withBackground(background: Either[Color, Changing[Color]]): ViewEmptyLabelSettings =
		copy(backgroundPointer = Some(background))
}

trait ViewEmptyLabelSettingsWrapper[+Repr] extends ViewEmptyLabelSettingsLike[Repr]
{
	// ABSTRACT ----------------------------
	
	protected def settings: ViewEmptyLabelSettings
	protected def withSettings(settings: ViewEmptyLabelSettings): Repr
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def backgroundPointer: Option[Either[Color, Changing[Color]]] = settings.backgroundPointer
	override protected def drawersPointer: Changing[Seq[CustomDrawer]] = settings.drawersPointer
	
	override def withCustomDrawers(drawersPointer: Changing[Seq[CustomDrawer]]): Repr =
		mapSettings { _.withCustomDrawers(drawersPointer) }
	override protected def withBackground(background: Either[Color, Changing[Color]]): Repr =
		mapSettings { _.withBackground(background) }
	
	
	// OTHER    --------------------------
	
	def mapSettings(f: ViewEmptyLabelSettings => ViewEmptyLabelSettings) = withSettings(f(settings))
}

case class ViewEmptyLabelSetup(settings: ViewEmptyLabelSettings)
	extends ViewEmptyLabelSettingsWrapper[ViewEmptyLabelSetup] with ComponentFactoryFactory[ViewEmptyLabelFactory]
{
	// IMPLEMENTED  -----------------------
	
	override def apply(hierarchy: ComponentHierarchy) = ViewEmptyLabelFactory(hierarchy, settings)
	
	override protected def withSettings(settings: ViewEmptyLabelSettings): ViewEmptyLabelSetup =
		copy(settings = settings)
}

trait ViewEmptyLabelFactoryLike[+Repr] extends ViewEmptyLabelSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	// OTHER    --------------------------
	
	/**
	  * Creates a new empty label
	  * @param sizePointer Pointer to the size of this label
	  * @return A new empty label
	  */
	def apply(sizePointer: Changing[StackSize]) = {
		// If background drawing is used, alters the custom drawers and may add additional repainting events
		val (drawers, repaintPointer) = backgroundPointer match {
			// Case: Background drawing is used
			case Some(color) =>
				val (drawer, repaintPointer) = color match {
					// Case: Static background
					case Left(color) => BackgroundDrawer(color) -> None
					// Case: Variable background => Adds additional repainting events
					case Right(colorPointer) => BackgroundViewDrawer(colorPointer) -> Some(colorPointer)
				}
				drawersPointer.map { drawer +: _ } -> repaintPointer
			// Case: No background drawing is used
			case None => drawersPointer -> None
		}
		val label = new ViewEmptyLabel(hierarchy, sizePointer, drawers)
		// Applies repainting events, if needed
		repaintPointer.foreach { _.addContinuousAnyChangeListener { label.repaint() } }
		label
	}
	/**
	  * @param size Size of the created label
	  * @return A new empty label with fixed size
	  */
	def apply(size: StackSize): ViewEmptyLabel = apply(Fixed(size))
}

case class ViewEmptyLabelFactory(hierarchy: ComponentHierarchy,
                                 settings: ViewEmptyLabelSettings = ViewEmptyLabelSettings.default)
	extends ViewEmptyLabelFactoryLike[ViewEmptyLabelFactory]
		with FromContextFactory[ColorContext, ContextualViewEmptyLabelFactory]
{
	// IMPLEMENTED  ------------------------------
	
	override protected def withSettings(settings: ViewEmptyLabelSettings): ViewEmptyLabelFactory =
		copy(settings = settings)
	
	override def withContext(context: ColorContext) =
		ContextualViewEmptyLabelFactory(hierarchy, context, settings)
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Creates a new label
	  * @param stackSizePointer     Pointer to this label's stack size
	  * @param customDrawersPointer Pointer to this label's custom drawers (default = always empty)
	  * @return A new label
	  */
	@deprecated("Please use .withCustomDrawers(Changing).apply(Changing) instead", "v1.1")
	def apply(stackSizePointer: Changing[StackSize],
	          customDrawersPointer: Changing[Seq[CustomDrawer]]): ViewEmptyLabel =
		withCustomDrawers(customDrawersPointer).apply(stackSizePointer)
	/**
	 * Creates a new label with fixed stack size
	 * @param stackSize This label's stack size
	 * @param customDrawersPointer Pointer to this label's custom drawers
	 * @return A new label
	 */
	@deprecated("Please use .withCustomDrawers(Changing).apply(StackSize) instead", "v1.1")
	def withStaticSize(stackSize: StackSize, customDrawersPointer: Changing[Seq[CustomDrawer]]) =
		apply(Fixed(stackSize), customDrawersPointer)
	/**
	 * Creates a new label with fixed custom drawers
	 * @param stackSizePointer Pointer to this label's stack size
	 * @param customDrawers Custom drawers to assign to this label
	 * @return A new label
	 */
	@deprecated("Please use .withCustomDrawers(Vector).apply(Changing) instead", "v1.1")
	def withStaticDrawers(stackSizePointer: Changing[StackSize], customDrawers: Seq[CustomDrawer]) =
		withCustomDrawers(customDrawers).apply(stackSizePointer)
	/**
	 * Creates a new label with a changing background color
	 * @param backgroundPointer A pointer to this label's background color
	 * @param stackSizePointer Pointer to this label's stack size
	 * @return A new label
	 */
	@deprecated("Please use .withBackground(Changing).apply(Changing) instead", "v1.1")
	def withBackground(backgroundPointer: Changing[Color], stackSizePointer: Changing[StackSize]): ViewEmptyLabel =
		withBackground(backgroundPointer).apply(stackSizePointer)
}

case class ContextualViewEmptyLabelFactory(hierarchy: ComponentHierarchy, context: ColorContext,
                                           settings: ViewEmptyLabelSettings = ViewEmptyLabelSettings.default)
	extends ViewEmptyLabelFactoryLike[ContextualViewEmptyLabelFactory]
		with ContextualFactory[ColorContext, ContextualViewEmptyLabelFactory]
		with ContextualVariableBackgroundAssignable[ColorContextPropsView, ContextualViewEmptyLabelFactory]
{
	// COMPUTED -----------------------------------
	
	/**
	 * @return A copy of this factory without contextual information
	 */
	@deprecated("Deprecated for removal", "v1.1")
	def withoutContext = ViewEmptyLabel(hierarchy)
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def withSettings(settings: ViewEmptyLabelSettings): ContextualViewEmptyLabelFactory =
		copy(settings = settings)
	
	override def withBackground(background: Color) =
		super[ContextualVariableBackgroundAssignable].withBackground(background)
	override def withBackground(background: ColorSet, preferredShade: ColorLevel) =
		super[ContextualVariableBackgroundAssignable].withBackground(background, preferredShade)
	override def withBackground(background: ColorSet) =
		super[ContextualVariableBackgroundAssignable].withBackground(background)
	override def withBackground(background: ColorRole, preferredShade: ColorLevel) =
		super[ContextualVariableBackgroundAssignable].withBackground(background, preferredShade)
	override def withBackground(background: ColorRole) =
		super[ContextualVariableBackgroundAssignable].withBackground(background)
	
	override def withCustomDrawers(drawersPointer: Changing[Seq[CustomDrawer]]): ContextualViewEmptyLabelFactory =
		mapSettings { _.copy(drawersPointer = drawersPointer) }
	override protected def withBackground(background: Either[Color, Changing[Color]]): ContextualViewEmptyLabelFactory =
		mapSettings { _.copy(backgroundPointer = Some(background)) }
	override def withContext(newContext: ColorContext) = copy(context = newContext)
}

/**
 * A pointer-based empty label
 * @author Mikko Hilpinen
 * @since 29.1.2021, v0.1
 */
class ViewEmptyLabel(override val hierarchy: ComponentHierarchy, val stackSizePointer: Changing[StackSize],
                     val customDrawersPointer: Changing[Seq[CustomDrawer]])
	extends ConcreteCustomDrawReachComponent
{
	// INITIAL CODE -------------------------------
	
	// Reacts to pointer changes
	stackSizePointer.addContinuousAnyChangeListener { revalidate() }
	customDrawersPointer.addContinuousAnyChangeListener { repaint() }
	
	
	// IMPLEMENTED  -------------------------------
	
	override def calculatedStackSize = stackSizePointer.value
	override def customDrawers = customDrawersPointer.value
	
	override def updateLayout() = ()
}

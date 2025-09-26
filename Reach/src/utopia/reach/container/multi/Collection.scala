package utopia.reach.container.multi

import utopia.firmament.component.container.many.MultiContainer
import utopia.firmament.component.stack.{HasStackSize, StackSizeCalculating}
import utopia.firmament.component.{DelayedBoundsUpdate, HasMutableBounds}
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.controller.Stacker
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.{Axis, Axis2D}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromGenericContextComponentFactoryFactory, FromGenericContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy, ReachComponent}
import utopia.reach.component.wrapper.ContainerCreation.MultiContainerCreation
import utopia.reach.component.wrapper.Open.OpenGroup
import utopia.reach.container.multi.Collection.{StackBoundsWrapper, layoutPriorities}

import scala.collection.mutable

/**
  * Common trait for collection factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait CollectionSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The direction the components are laid first.
	  * If X, the components form vertically stacking rows. If Y, the components form horizontally
	  * stacked columns.
	  */
	def primaryAxis: Axis2D
	/**
	  * Layout within individual rows or columns (first layer).
	  */
	def insideRowLayout: StackLayout
	/**
	  * Layout between the rows or columns (second layer)
	  */
	def betweenRowsLayout: StackLayout
	/**
	  * Maximum length of individual rows / columns, at which they are automatically split.
	  * Contains none if all elements should be stacked in a single row/column.
	  */
	def splitThresholdPointer: Changing[Option[Double]]
	/**
	  * A pointer that specifies the margin placed around the items at the edges of this collection.
	  */
	def outerMarginPointer: Changing[StackLength]
	
	/**
	  * Layout between the rows or columns (second layer)
	  * @param layout New between rows layout to use.
	  *               Layout between the rows or columns (second layer)
	  * @return Copy of this factory with the specified between rows layout
	  */
	def withBetweenRowsLayout(layout: StackLayout): Repr
	/**
	  * Layout within individual rows or columns (first layer).
	  * @param layout New inside row layout to use.
	  *               Layout within individual rows or columns (first layer).
	  * @return Copy of this factory with the specified inside row layout
	  */
	def withInsideRowLayout(layout: StackLayout): Repr
	/**
	  * A pointer that specifies the margin placed around the items at the edges of this collection.
	  * @param p New outer margin pointer to use.
	  *          A pointer that specifies the margin placed around the items at the edges of this
	  *          collection.
	  * @return Copy of this factory with the specified outer margin pointer
	  */
	def withOuterMarginPointer(p: Changing[StackLength]): Repr
	/**
	  * The direction the components are laid first.
	  * If X, the components form vertically stacking rows. If Y, the components form horizontally
	  * stacked columns.
	  * @param axis New primary axis to use.
	  *             The direction the components are laid first.
	  *             If X, the components form vertically stacking rows. If Y, the components form
	  *             horizontally stacked columns.
	  * @return Copy of this factory with the specified primary axis
	  */
	def withPrimaryAxis(axis: Axis2D): Repr
	/**
	  * Maximum length of individual rows / columns, at which they are automatically split.
	  * Contains none if all elements should be stacked in a single row/column.
	  * @param thresholdPointer New split threshold pointer to use.
	  *                         Maximum length of individual rows / columns, at which they are
	  *                         automatically split.
	  *                         Contains none if all elements should be stacked in a single
	  *                         row/column.
	  * @return Copy of this factory with the specified split threshold pointer
	  */
	def withSplitThresholdPointer(thresholdPointer: Changing[Option[Double]]): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Copy of this factory that builds vertical columns and places them left to right
	  */
	def columns = withPrimaryAxis(Y)
	
	
	// OTHER	--------------------
	
	/**
	  * @param threshold Threshold after which this collection prepares to split contents to a new line
	  * @return Copy of this factory with that threshold in place
	  */
	def withSplitThreshold(threshold: Double): Repr = withSplitThresholdPointer(Fixed(Some(threshold)))
	
	/**
	  * @param margin Margin placed at the edges of this collection
	  * @return Copy of this factory with the specified margin
	  */
	def withOuterMargin(margin: StackLength): Repr = withOuterMarginPointer(Fixed(margin))
	
	def mapOuterMarginPointer(f: Mutate[Changing[StackLength]]) =
		withOuterMarginPointer(f(outerMarginPointer))
	def mapOuterMargin(f: Mutate[StackLength]) = mapOuterMarginPointer { _.map(f) }
}

object CollectionSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing collections
  * @param customDrawers         Custom drawers to assign to created components
  * @param primaryAxis           The direction the components are laid first.
  *                              If X, the components form vertically stacking rows. If Y, the
  *                              components form horizontally stacked columns.
  * @param insideRowLayout       Layout within individual rows or columns (first layer).
  * @param betweenRowsLayout     Layout between the rows or columns (second layer)
  * @param splitThresholdPointer Maximum length of individual rows / columns, at which they are
  *                              automatically split.
  *                              Contains none if all elements should be stacked in a single
  *                              row/column.
  * @param outerMarginPointer    A pointer that specifies the margin placed around the items at
  *                              the edges of this collection.
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class CollectionSettings(customDrawers: Seq[CustomDrawer] = Empty, primaryAxis: Axis2D = Axis.X,
                              insideRowLayout: StackLayout = StackLayout.Fit,
                              betweenRowsLayout: StackLayout = StackLayout.Fit,
                              splitThresholdPointer: Changing[Option[Double]] = Fixed(None),
                              outerMarginPointer: Changing[StackLength] = Fixed(StackLength.fixedZero))
	extends CollectionSettingsLike[CollectionSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withBetweenRowsLayout(layout: StackLayout) = copy(betweenRowsLayout = layout)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withInsideRowLayout(layout: StackLayout) = copy(insideRowLayout = layout)
	override def withOuterMarginPointer(p: Changing[StackLength]) = copy(outerMarginPointer = p)
	override def withPrimaryAxis(axis: Axis2D) = copy(primaryAxis = axis)
	
	override def withSplitThresholdPointer(thresholdPointer: Changing[Option[Double]]) =
		copy(splitThresholdPointer = thresholdPointer)
}

/**
  * Common trait for factories that wrap a collection settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait CollectionSettingsWrapper[+Repr] extends CollectionSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: CollectionSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: CollectionSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def betweenRowsLayout = settings.betweenRowsLayout
	override def customDrawers = settings.customDrawers
	override def insideRowLayout = settings.insideRowLayout
	override def outerMarginPointer = settings.outerMarginPointer
	override def primaryAxis = settings.primaryAxis
	override def splitThresholdPointer = settings.splitThresholdPointer
	
	override def withBetweenRowsLayout(layout: StackLayout) = mapSettings { _.withBetweenRowsLayout(layout) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapSettings { _.withCustomDrawers(drawers) }
	override def withInsideRowLayout(layout: StackLayout) = mapSettings { _.withInsideRowLayout(layout) }
	override def withOuterMarginPointer(p: Changing[StackLength]) =
		mapSettings { _.withOuterMarginPointer(p) }
	override def withPrimaryAxis(axis: Axis2D) = mapSettings { _.withPrimaryAxis(axis) }
	override def withSplitThresholdPointer(thresholdPointer: Changing[Option[Double]]) =
		mapSettings { _.withSplitThresholdPointer(thresholdPointer) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: CollectionSettings => CollectionSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing collections (whether they be static or view-based)
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait AnyCollectionFactoryLike[+Repr] extends CollectionSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	// ABSTRACT ------------------------
	
	/**
	  * @return A pointer that contains the margin placed between the items in this collection.
	  */
	def innerMarginPointer: Changing[StackLength]
	/**
	  * @param p A pointer that indicates the margin to place between items within this collection
	  * @return Copy of this factory using the specified pointer
	  */
	def withInnerMarginPointer(p: Changing[StackLength]): Repr
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Copy of this factory that doesn't allow any margins between items
	  */
	def withoutInnerMargin = withInnerMargin(StackLength.fixedZero)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param margin Margin placed between the items in this collection
	  * @return Copy of this factory with the specified margin
	  */
	def withInnerMargin(margin: StackLength): Repr = withInnerMarginPointer(Fixed(margin))
	def mapInnerMarginPointer(f: Mutate[Changing[StackLength]]) = withInnerMarginPointer(f(innerMarginPointer))
	def mapInnerMargin(f: Mutate[StackLength]) = mapInnerMarginPointer { _.map(f) }
}
/**
  * Common trait for factories that are used for constructing collections
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait CollectionFactoryLike[+Repr]
	extends AnyCollectionFactoryLike[Repr] with CombiningContainerFactory[Collection, ReachComponent]
{
	// IMPLEMENTED  -------------------
	
	override def apply[C <: ReachComponent, R](content: OpenGroup[C, R]): MultiContainerCreation[Collection, C, R] = {
		val collection: Collection = new _Collection(hierarchy, content.component,
			primaryAxis, insideRowLayout, betweenRowsLayout, innerMarginPointer, outerMarginPointer,
			splitThresholdPointer, customDrawers)
		content attachTo collection
	}
}

/**
  * Factory class used for constructing some type of collection containers using contextual component creation
  * information
  * @tparam N Type of context used and passed along by this factory
  * @tparam Repr Type of implementing (generic) factory class
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait AnyContextualCollectionFactory[+N <: BaseContextPropsView, +Repr[+_ <: BaseContextPropsView]]
	extends AnyCollectionFactoryLike[Repr[N]] with GenericContextualFactory[N, BaseContextPropsView, Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return A user-specified pointer that determines the margin placed between the items in this collection.
	  *         None if default margin should be applied instead.
	  */
	def customInnerMarginPointer: Option[Changing[StackLength]]
	/**
	  * @return Whether the items in this collection should be considered closely related to each other,
	  *         resulting in a smaller margin placed between them.
	  */
	def areRelated: Boolean
	
	/**
	  * @param p A user-specified pointer that determines the margin placed between the items in this
	  *          collection.
	  *          None if default margin should be applied instead.
	  * @return Copy of this factory with the specified custom inner margin pointer
	  */
	def withInnerMarginPointer(p: Option[Changing[StackLength]]): Repr[N]
	/**
	  * @param related Whether the items in this collection should be considered closely related to
	  *                each other,
	  *                resulting in a smaller margin placed between them.
	  * @return Copy of this factory with the specified are related
	  */
	def withAreRelated(related: Boolean): Repr[N]
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Copy of this factory that places the components closer to each other
	  */
	def related = withAreRelated(true)
	
	
	// IMPLEMENTED  ----------------------
	
	override def innerMarginPointer: Changing[StackLength] = customInnerMarginPointer
		.getOrElse { if (areRelated) context.smallStackMarginPointer else context.stackMarginPointer }
	override def withInnerMarginPointer(p: Changing[StackLength]): Repr[N] = withInnerMarginPointer(Some(p))
	
	
	// OTHER    --------------------------
	
	/**
	  * @param margin Margin to place between the components in this collection (general)
	  * @return Copy of this factory with the specified margin
	  */
	def withInnerMargin(margin: SizeCategory): Repr[N] =
		withInnerMarginPointer(context.scaledStackMarginPointer(margin))
	/**
	  * @param margin Margin to place at the edges of this collection (general)
	  * @return Copy of this factory with the specified margin
	  */
	def withOuterMargin(margin: SizeCategory) = withOuterMarginPointer(context.scaledStackMarginPointer(margin))
}
/**
  * Factory class used for constructing collections using contextual component creation
  * information
  * @param customInnerMarginPointer A user-specified pointer that determines the margin placed
  *                                 between the items in this collection.
  *                                 None if default margin should be applied instead.
  * @param areRelated               Whether the items in this collection should be considered
  *                                 closely related to each other,
  *                                 resulting in a smaller margin placed between them.
  * @tparam N Type of context used and passed along by this factory
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class ContextualCollectionFactory[+N <: BaseContextPropsView](hierarchy: ComponentHierarchy,
                                                                   context: N, settings: CollectionSettings,
                                                                   customInnerMarginPointer: Option[Changing[StackLength]] = None,
                                                                   areRelated: Boolean = false)
	extends CollectionFactoryLike[ContextualCollectionFactory[N]]
		with AnyContextualCollectionFactory[N, ContextualCollectionFactory]
		with ContextualCombiningContainerFactory[N, BaseContextPropsView, Collection, ReachComponent, ContextualCollectionFactory]
{
	// IMPLEMENTED  ----------------------
	
	override def withContext[N2 <: BaseContextPropsView](context: N2) =
		copy(context = context)
	override def withSettings(settings: CollectionSettings) = copy(settings = settings)
	
	def withAreRelated(related: Boolean) = copy(areRelated = related)
	def withInnerMarginPointer(p: Option[Changing[StackLength]]) = copy(customInnerMarginPointer = p)
}
/**
  * Factory class that is used for constructing collections without using contextual information
  * @param innerMarginPointer A pointer that specifies the margin placed between the in this
  *                           collection.
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class CollectionFactory(hierarchy: ComponentHierarchy,
                             settings: CollectionSettings = CollectionSettings.default,
                             innerMarginPointer: Changing[StackLength] = Fixed(StackLength.any))
	extends CollectionFactoryLike[CollectionFactory]
		with FromGenericContextFactory[BaseContextPropsView, ContextualCollectionFactory]
		with NonContextualCombiningContainerFactory[Collection, ReachComponent]
{
	// IMPLEMENTED  ------------------------
	
	override def withContext[N <: BaseContextPropsView](context: N) =
		ContextualCollectionFactory(hierarchy, context, settings)
	override def withSettings(settings: CollectionSettings) = copy(settings = settings)
	
	/**
	  * @param p A pointer that specifies the margin placed between the in this collection.
	  * @return Copy of this factory with the specified inner margin pointer
	  */
	def withInnerMarginPointer(p: Changing[StackLength]) = copy(innerMarginPointer = p)
}

/**
  * Used for defining collection creation settings outside the component building process
  * @tparam F Type of non-contextual factories created
  * @tparam CF Type of (generic) contextual factories created
  * @tparam Repr Type of the concrete implementing setup
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
trait AnyCollectionSetup[+F, +CF[_ <: BaseContextPropsView], +Repr]
	extends CollectionSettingsWrapper[Repr] with ComponentFactoryFactory[F]
		with FromGenericContextComponentFactoryFactory[BaseContextPropsView, CF]
/**
  * Used for defining collection creation settings outside the component building process
  * @author Mikko Hilpinen
  * @since 09.04.2025, v1.6
  */
case class CollectionSetup(settings: CollectionSettings = CollectionSettings.default)
	extends AnyCollectionSetup[CollectionFactory, ContextualCollectionFactory, CollectionSetup]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = CollectionFactory(hierarchy, settings)
	
	override def withContext[N <: BaseContextPropsView](hierarchy: ComponentHierarchy, context: N) =
		ContextualCollectionFactory(hierarchy, context, settings)
	override def withSettings(settings: CollectionSettings) = copy(settings = settings)
}

object Collection extends CollectionSetup()
{
	// ATTRIBUTES   -------------------------
	
	private def takeOptimal(s: StackLength) = s.optimal
	private def takeMin(s: StackLength) = s.min
	private def takeMinIfShrinks(s: StackLength) = if (s.priority.shrinksFirst) s.min else s.optimal
	
	/**
	  * A list of different layout options.
	  * Each entry contains 2 values:
	  *     1. Function for taking the length of an item, based on its stack length (e.g. take optimal length)
	  *     1. Function for taking the length of a margin, based on the default (stack) margin (e.g. take minimum margin)
	  *
	  * These layouts are ordered in terms of their priority, from most to least preferred.
	  * Size constraints may force smaller layouts to be used.
	  */
	private val layoutPriorities = Vector[(StackLength => Double, StackLength => Double)](
		{ takeOptimal _ } -> { takeOptimal },
		{ takeMinIfShrinks _ } -> { takeMinIfShrinks },
		{ takeOptimal _ } -> { takeMin },
		{ takeMin _ } -> { takeMin }
	)
	
	
	// OTHER    -----------------------------
	
	def apply(settings: CollectionSettings) = withSettings(settings)
	
	
	// NESTED   -----------------------------
	
	private class StackBoundsWrapper(override val stackSize: StackSize) extends HasMutableBounds with HasStackSize
	{
		// ATTRIBUTES   ---------------------
		
		override var bounds = Bounds.zero
		
		
		// IMPLEMENTED  ---------------------
		
		override def position_=(p: Point): Unit = bounds = bounds.withPosition(p)
		override def size_=(s: Size): Unit = bounds = bounds.withSize(s)
	}
}
/**
  * A view that presents items within rows and columns, forming a 2D matrix.
  * Only splits the rows/columns when necessary.
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.1
  */
// TODO: We need a view-based version as well
trait Collection extends ReachComponent with MultiContainer[ReachComponent] with StackSizeCalculating
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The direction the components are being laid first
	  */
	def primaryAxis: Axis2D
	
	/**
	  * @return Layout within individual rows or columns (first layer)
	  */
	def insideRowLayout: StackLayout
	/**
	  * @return Layout between the rows or columns (second layer)
	  */
	def betweenRowsLayout: StackLayout
	
	/**
	  * @return Margin between the items within this collection
	  */
	def innerMarginPointer: Changing[StackLength]
	/**
	  * @return Margin placed at the edges of this collection
	  */
	def outerMarginPointer: Changing[StackLength]
	
	/**
	  * @return A length threshold, after which the content is split to a new line.
	  *         Affects the stack size of this collection.
	  *         None if this collection should primarily attempt to form a single line only.
	  */
	def splitThreshold: Option[Double]
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return The direction along which the rows / columns are laid next to each other
	  */
	def secondaryAxis = primaryAxis.perpendicular
	
	
	// IMPLEMENTED  ---------------------
	
	override def children = components
	
	override def calculatedStackSize: StackSize = {
		// Case: Empty => No stack size
		if (components.isEmpty)
			StackSize.any
		else {
			val innerMargin = innerMarginPointer.value
			val outerMargin = outerMarginPointer.value
			val componentSizes = components.map { _.stackSize }
			splitThreshold match {
				// Case: A split threshold has been specified => Calculates size for X rows
				case Some(threshold) =>
					// The split threshold may be increased if there are too large components involved
					val actualThreshold = threshold max componentSizes.iterator.map { _(primaryAxis).min }.max
					// Calculates what kind of row layout would be optimal
					val rows = buildRows(componentSizes, innerMargin.optimal,
						actualThreshold) { _(primaryAxis).optimal }
					// Calculates the size of the collection
					val rowSizes = rows.map { row =>
						Stacker.calculateStackSize(row, primaryAxis, innerMargin, outerMargin,
							insideRowLayout)
					}
					Stacker.calculateStackSize(rowSizes, secondaryAxis, innerMargin, outerMargin,
						betweenRowsLayout)
						.mapDimension(primaryAxis) { _.lowPriority }
						.mapDimension(secondaryAxis) { _.expanding }
					
				// Case: No split threshold specified => Places all items on the same row, if possible
				case None =>
					val base = Stacker.calculateStackSize(componentSizes, primaryAxis, innerMargin, outerMargin,
						insideRowLayout)
					// Places the outer margins, also
					base.mapDimension(secondaryAxis) { l => (l + outerMargin * 2).noMax.expanding }
						.mapDimension(primaryAxis) { _.shrinking }
			}
		}
	}
	
	override def updateLayout(): Unit = {
		val mySize = size
		val componentsWithSizes = components.map { c => (c: ReachComponent) -> c.stackSize }
		if (componentsWithSizes.nonEmpty) {
			val maxLength = mySize(primaryAxis)
			val maxBreadth = mySize(secondaryAxis)
			val margins = Pair(innerMarginPointer, outerMarginPointer).map { _.value }
			val innerMargin = margins.first
			val outerMargin = margins.second
			// Finds which layout to use, based on how many rows may be fit into this collection's current state
			val layouts = layoutPriorities.lazyMap { case (lengthOfItem, lengthOfMargin) =>
				val layout = buildRows(componentsWithSizes, lengthOfMargin(innerMargin), maxLength) {
					case (_, componentSize) => lengthOfItem(componentSize(primaryAxis)) }
				val breadth = layout.foldLeft(0.0) { _ + _.iterator.map { _._2(secondaryAxis).min }.max } +
					innerMargin.min * layout.size + outerMargin.min * 2
				layout -> breadth
			}
			// Case: Optimal layout is achievable => Uses that
			if (layouts.head._2 <= maxBreadth)
				actualizeLayout(mySize, layouts.head._1, margins)
			// Case: Not even the minimal layout is possible => Minimizes layout
			else if (layouts.last._2 >= maxBreadth)
				actualizeLayout(mySize, layouts.last._1, margins.map { _.noMin })
			// Case: The fitting layout is somewhere between these two extremes => Finds and uses the best one that fits
			else
				layouts.find { _._2 <= maxBreadth }
					.foreach { case (layout, _) => actualizeLayout(mySize, layout, margins) }
		}
	}
	
	
	// OTHER    --------------------------
	
	/**
	  * Sets up automated component layout changes that are triggered whenever the margin pointer values change
	  */
	protected def setupMarginListeners() = {
		val pointers = Pair(innerMarginPointer, outerMarginPointer)
		if (pointers.exists { _.mayChange }) {
			lazy val revalidateListener = ChangeListener.triggerAfterEffect { revalidate() }
			linkedFlag.addListener { e =>
				if (e.newValue)
					pointers.foreach { _.addListener(revalidateListener) }
				else
					pointers.foreach { _.removeListener(revalidateListener) }
			}
		}
	}
	
	private def actualizeLayout(area: Size, layout: Seq[Seq[(ReachComponent, StackSize)]],
	                            margins: Pair[StackLength]) =
	{
		val innerMargin = margins.first
		val outerMargin = margins.second
		// Places the rows first
		val rowWrappers = layout.map { row =>
			val stackSize = Stacker.calculateStackSize(row.map { _._2 }, primaryAxis, innerMargin, outerMargin,
				insideRowLayout)
			new StackBoundsWrapper(stackSize)
		}
		Stacker.apply(rowWrappers, Bounds(Point.origin, area), secondaryAxis, innerMargin, outerMargin,
			betweenRowsLayout)
		
		// Then places the components within the rows
		// The bounds updates are delayed
		layout.iterator.zip(rowWrappers)
			.flatMap { case (row, rowWrapper) =>
				val itemWrappers = row.map { case (component, stackSize) =>
					new DelayedBoundsUpdate(component, stackSize)
				}
				Stacker(itemWrappers, rowWrapper.bounds, primaryAxis, innerMargin, outerMargin, insideRowLayout)
				
				itemWrappers
			}
			// Applies the bounds updates
			.foreach { _() }
	}
	
	// Places items in rows, respecting the specified length threshold
	private def buildRows[A](items: Iterable[A], margin: Double, threshold: Double)(lengthOf: A => Double) = {
		val rowsBuilder = OptimizedIndexedSeq.newBuilder[IndexedSeq[A]]
		val (lastRowBuilder, _) = items
			.foldLeft((OptimizedIndexedSeq.newBuilder[A], 0.0)) { case ((rowBuilder, rowLength), item) =>
				placeOnLine(rowsBuilder, rowBuilder, rowLength, item, margin, threshold)(lengthOf)
			}
		rowsBuilder += lastRowBuilder.result()
		rowsBuilder.result()
	}
	
	// Intended to be used in foldLeft
	private def placeOnLine[A](rowsBuilder: mutable.Growable[IndexedSeq[A]],
	                           rowBuilder: mutable.Builder[A, IndexedSeq[A]], rowLength: Double, item: A,
	                           margin: Double, threshold: Double)
	                          (lengthOf: A => Double) =
	{
		// Checks whether to place the item on the same line, or on a different line
		val length = lengthOf(item)
		val wouldBeRowLength = rowLength + length + margin
		// Case: Too long to place on the same line => Starts a new line
		if (rowLength > 0.0 && wouldBeRowLength > threshold) {
			rowsBuilder += rowBuilder.result()
			val newRowBuilder = OptimizedIndexedSeq.newBuilder[A]
			newRowBuilder += item
			newRowBuilder -> length
		}
		// Case: Fits to the same line => Places it
		else {
			rowBuilder += item
			rowBuilder -> wouldBeRowLength
		}
	}
}

private class _Collection(override val hierarchy: ComponentHierarchy,
                          override val components: Seq[ReachComponent], override val primaryAxis: Axis2D,
                          override val insideRowLayout: StackLayout, override val betweenRowsLayout: StackLayout,
                          override val innerMarginPointer: Changing[StackLength],
                          override val outerMarginPointer: Changing[StackLength],
                          splitThresholdPointer: Changing[Option[Double]],
                          override val customDrawers: Seq[CustomDrawer])
	extends ConcreteCustomDrawReachComponent with Collection
{
	// INITIAL CODE ----------------------------
	
	setupMarginListeners()
	splitThresholdPointer.addListenerWhile(linkedFlag)(ChangeListener.triggerAfterEffect { revalidate() })
	
	
	// IMPLEMENTED  ----------------------------
	
	override def splitThreshold: Option[Double] = splitThresholdPointer.value
}
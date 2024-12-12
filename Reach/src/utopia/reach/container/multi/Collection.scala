package utopia.reach.container.multi

import utopia.firmament.component.container.many.MultiContainer
import utopia.firmament.component.stack.{HasStackSize, StackSizeCalculating}
import utopia.firmament.component.{DelayedBoundsUpdate, HasMutableBounds}
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.controller.Stacker
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromGenericContextFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}
import utopia.reach.component.wrapper.ComponentWrapResult.ComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.BundledOpenComponents
import utopia.reach.container.multi.Collection.{StackBoundsWrapper, layoutPriorities}

import scala.collection.immutable.VectorBuilder

object Collection extends Cff[CollectionFactory]
{
	// ATTRIBUTES   -------------------------
	
	private def takeOptimal(s: StackLength) = s.optimal
	private def takeMin(s: StackLength) = s.min
	private def takeMinIfShrinks(s: StackLength) = if (s.priority.shrinksFirst) s.min else s.optimal
	
	private val layoutPriorities = Vector[(StackLength => Double, StackLength => Double)](
		{ takeOptimal _ } -> { takeOptimal },
		{ takeMinIfShrinks _ } -> { takeMinIfShrinks },
		{ takeOptimal _ } -> { takeMin },
		{ takeMin _ } -> { takeMin }
	)
	
	
	// IMPLEMENTED    -----------------------
	
	override def apply(hierarchy: ComponentHierarchy): CollectionFactory = CollectionFactory(hierarchy)
	
	
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

trait CollectionFactoryLike[+Repr]
	extends CombiningContainerFactory[Collection, ReachComponentLike] with CustomDrawableFactory[Repr]
{
	// ABSTRACT ------------------------
	
	def primaryAxis: Axis2D
	def insideRowLayout: StackLayout
	def betweenRowsLayout: StackLayout
	def innerMarginPointer: Changing[StackLength]
	def outerMarginPointer: Changing[StackLength]
	def splitThreshold: Option[Double]
	
	/**
	  * @param axis The axis along which the components are laid out first (i.e. direction of rows / lines).
	  *             X = Build left to right first
	  *             Y = Build top to bottom first
	  * @return Copy of this factory with the specified axis
	  */
	def withPrimaryAxis(axis: Axis2D): Repr
	/**
	  * @param layout Layout to use inside or rows / columns
	  * @return Copy of this factory with specified row layout
	  */
	def withInsideRowLayout(layout: StackLayout): Repr
	/**
	  * @param layout Layout to use when placing the rows / columns
	  * @return Copy of this factory with the specified layout
	  */
	def withBetweenRowsLayout(layout: StackLayout): Repr
	/**
	  * @param p A pointer that indicates the margin to place between items within this collection
	  * @return Copy of this factory using the specified pointer
	  */
	def withInnerMarginPointer(p: Changing[StackLength]): Repr
	/**
	  * @param p A pointer that indicates the margin to place around this collection
	  * @return Copy of this factory using the specified pointer
	  */
	def withOuterMarginPointer(p: Changing[StackLength]): Repr
	/**
	  * @param threshold Threshold after which this collection prepares to split contents to a new line
	  * @return Copy of this factory with that threshold in place
	  */
	def withSplitThreshold(threshold: Double): Repr
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Copy of this factory that doesn't allow any margins between items
	  */
	def withoutInnerMargin = withInnerMargin(StackLength.fixedZero)
	/**
	  * @return Copy of this factory that builds vertrical columns and places them left to right
	  */
	def columns = withPrimaryAxis(Y)
	
	
	// IMPLEMENTED  -------------------
	
	override def apply[C <: ReachComponentLike, R](content: BundledOpenComponents[C, R]): ComponentsWrapResult[Collection, C, R] = {
		val collection: Collection = new _Collection(parentHierarchy, content.component,
			primaryAxis, insideRowLayout, betweenRowsLayout, innerMarginPointer, outerMarginPointer, splitThreshold,
			customDrawers)
		content attachTo collection
	}
	
	
	// OTHER    -----------------------
	
	/**
	  * @param margin Margin placed between the items in this collection
	  * @return Copy of this factory with the specified margin
	  */
	def withInnerMargin(margin: StackLength): Repr = withInnerMarginPointer(Fixed(margin))
	/**
	  * @param margin Margin placed at the edges of this collection
	  * @return Copy of this factory with the specified margin
	  */
	def withOuterMargin(margin: StackLength): Repr = withOuterMarginPointer(Fixed(margin))
	
	def mapInnerMarginPointer(f: Mutate[Changing[StackLength]]) = withInnerMarginPointer(f(innerMarginPointer))
	def mapOuterMarginPointer(f: Mutate[Changing[StackLength]]) = withOuterMarginPointer(f(outerMarginPointer))
	
	def mapInnerMargin(f: Mutate[StackLength]) = mapInnerMarginPointer { _.map(f) }
	def mapOuterMargin(f: Mutate[StackLength]) = mapOuterMarginPointer { _.map(f) }
}

case class CollectionFactory(parentHierarchy: ComponentHierarchy, primaryAxis: Axis2D = X,
                             insideRowLayout: StackLayout = Fit, betweenRowsLayout: StackLayout = Leading,
                             innerMarginPointer: Changing[StackLength] = Fixed(StackLength.any),
                             outerMarginPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                             splitThreshold: Option[Double] = None, customDrawers: Seq[CustomDrawer] = Empty)
	extends CollectionFactoryLike[CollectionFactory]
		with NonContextualCombiningContainerFactory[Collection, ReachComponentLike]
		with FromGenericContextFactory[BaseContextPropsView, ContextualCollectionFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def withPrimaryAxis(axis: Axis2D): CollectionFactory = copy(primaryAxis = axis)
	override def withInsideRowLayout(layout: StackLayout): CollectionFactory = copy(insideRowLayout = layout)
	override def withBetweenRowsLayout(layout: StackLayout): CollectionFactory = copy(betweenRowsLayout = layout)
	override def withInnerMarginPointer(p: Changing[StackLength]): CollectionFactory = copy(innerMarginPointer = p)
	override def withOuterMarginPointer(p: Changing[StackLength]): CollectionFactory = copy(outerMarginPointer = p)
	override def withSplitThreshold(threshold: Double): CollectionFactory = copy(splitThreshold = Some(threshold))
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): CollectionFactory = copy(customDrawers = drawers)
	
	override def withContext[N <: BaseContextPropsView](context: N): ContextualCollectionFactory[N] =
		ContextualCollectionFactory[N](parentHierarchy, context, primaryAxis, insideRowLayout, betweenRowsLayout,
			outerMarginPointer, splitThreshold, customDrawers)
}

case class ContextualCollectionFactory[+N <: BaseContextPropsView](parentHierarchy: ComponentHierarchy, context: N,
                                                                   primaryAxis: Axis2D = X,
                                                                   insideRowLayout: StackLayout = Fit,
                                                                   betweenRowsLayout: StackLayout = Leading,
                                                                   outerMarginPointer: Changing[StackLength] = Fixed(StackLength.fixedZero),
                                                                   splitThreshold: Option[Double] = None,
                                                                   customDrawers: Seq[CustomDrawer] = Empty,
                                                                   customInnerMarginPointer: Option[Changing[StackLength]] = None,
                                                                   areRelated: Boolean = false)
	extends CollectionFactoryLike[ContextualCollectionFactory[N]]
		with ContextualCombiningContainerFactory[N, BaseContextPropsView, Collection, ReachComponentLike, ContextualCollectionFactory]
{
	// ATTRIBUTES   ----------------------
	
	override lazy val innerMarginPointer: Changing[StackLength] = customInnerMarginPointer
		.getOrElse { if (areRelated) context.smallStackMarginPointer else context.stackMarginPointer }
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Copy of this factory that places the components closer to each other
	  */
	def related = copy(areRelated = true)
	
	
	// IMPLEMENTED  ----------------------
	
	override def withContext[N2 <: BaseContextPropsView](newContext: N2): ContextualCollectionFactory[N2] =
		copy(context = newContext)
	
	override def withInnerMarginPointer(p: Changing[StackLength]): ContextualCollectionFactory[N] =
		copy(customInnerMarginPointer = Some(p))
	override def withOuterMarginPointer(p: Changing[StackLength]): ContextualCollectionFactory[N] =
		copy(outerMarginPointer = p)
	override def withPrimaryAxis(axis: Axis2D): ContextualCollectionFactory[N] = copy(primaryAxis = axis)
	override def withInsideRowLayout(layout: StackLayout): ContextualCollectionFactory[N] =
		copy(insideRowLayout = layout)
	override def withBetweenRowsLayout(layout: StackLayout): ContextualCollectionFactory[N] =
		copy(betweenRowsLayout = layout)
	override def withSplitThreshold(threshold: Double): ContextualCollectionFactory[N] =
		copy(splitThreshold = Some(threshold))
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualCollectionFactory[N] =
		copy(customDrawers = drawers)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param margin Margin to place between the components in this collection (general)
	  * @return Copy of this factory with the specified margin
	  */
	def withInnerMargin(margin: SizeCategory): ContextualCollectionFactory[N] =
		withInnerMarginPointer(context.scaledStackMarginPointer(margin))
	/**
	  * @param margin Margin to place at the edges of this collection (general)
	  * @return Copy of this factory with the specified margin
	  */
	def withOuterMargin(margin: SizeCategory) =
		copy(outerMarginPointer = context.scaledStackMarginPointer(margin))
}

/**
  * A view that presents items within rows and columns, forming a 2D matrix.
  * Only splits the rows/columns when necessary.
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.1
  */
trait Collection extends ReachComponentLike with MultiContainer[ReachComponentLike] with StackSizeCalculating
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
		val componentsWithSizes = components.map { c => (c: ReachComponentLike) -> c.stackSize }
		if (componentsWithSizes.nonEmpty) {
			val maxLength = mySize(primaryAxis)
			val maxBreadth = mySize(secondaryAxis)
			val margins = Pair(innerMarginPointer, outerMarginPointer).map { _.value }
			val innerMargin = margins.first
			val outerMargin = margins.second
			// Finds which layout to use, based on how many rows may be fit into this collection's current state
			val layouts = layoutPriorities.lazyMap { case (lengthOfItem, lengthOfMargin) =>
				val layout = buildRows(componentsWithSizes, lengthOfMargin(innerMargin),
					maxLength) { c => lengthOfItem(c._2(primaryAxis)) }
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
			lazy val revalidateListener = ChangeListener.onAnyChange { revalidate() }
			linkedFlag.addListener { e =>
				if (e.newValue)
					pointers.foreach { _.addListener(revalidateListener) }
				else
					pointers.foreach { _.removeListener(revalidateListener) }
			}
		}
	}
	
	private def actualizeLayout(area: Size, layout: Seq[Seq[(ReachComponentLike, StackSize)]],
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
		val simulatedStackSize = Stacker.calculateStackSize(rowWrappers.map { _.stackSize }, secondaryAxis,
			innerMargin, outerMargin, betweenRowsLayout)
		Stacker.apply(rowWrappers, Bounds(Point.origin, area), simulatedStackSize(secondaryAxis).optimal,
			secondaryAxis, innerMargin, outerMargin, betweenRowsLayout)
		
		// Then places the components within the rows
		// The bounds updates are delayed
		layout.iterator.zip(rowWrappers).flatMap { case (row, rowWrapper) =>
			val itemWrappers = row.map { case (component, stackSize) => new DelayedBoundsUpdate(component, stackSize) }
			Stacker(itemWrappers, rowWrapper.bounds, rowWrapper.stackSize(primaryAxis).optimal,
				primaryAxis, innerMargin, outerMargin, insideRowLayout)
			itemWrappers
		}.foreach { _() }
	}
	
	// Places items in rows, respecting the specified length threshold
	private def buildRows[A](items: Iterable[A], margin: Double, threshold: Double)(lengthOf: A => Double) = {
		val rowsBuilder = new VectorBuilder[Vector[A]]()
		val (lastRowBuilder, _) = items
			.foldLeft((new VectorBuilder[A](), 0.0)) { case ((rowBuilder, rowLength), item) =>
				placeOnLine(rowsBuilder, rowBuilder, rowLength, item, margin, threshold)(lengthOf)
			}
		rowsBuilder += lastRowBuilder.result()
		rowsBuilder.result()
	}
	
	// Intended to be used in foldLeft
	private def placeOnLine[A](rowsBuilder: VectorBuilder[Vector[A]], rowBuilder: VectorBuilder[A],
	                           rowLength: Double, item: A, margin: Double, threshold: Double)(lengthOf: A => Double) =
	{
		// Checks whether to place the item on the same line, or on a different line
		val length = lengthOf(item)
		val wouldBeRowLength = rowLength + length + margin
		// Case: Too long to place on the same line => Starts a new line
		if (rowLength > 0.0 && wouldBeRowLength > threshold) {
			rowsBuilder += rowBuilder.result()
			val newRowBuilder = new VectorBuilder[A]()
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

private class _Collection(override val parentHierarchy: ComponentHierarchy,
                          override val components: Seq[ReachComponentLike], override val primaryAxis: Axis2D,
                          override val insideRowLayout: StackLayout, override val betweenRowsLayout: StackLayout,
                          override val innerMarginPointer: Changing[StackLength],
                          override val outerMarginPointer: Changing[StackLength],
                          override val splitThreshold: Option[Double], override val customDrawers: Seq[CustomDrawer])
	extends CustomDrawReachComponent with Collection
{
	// INITIAL CODE ----------------------------
	
	setupMarginListeners()
}
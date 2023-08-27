package utopia.reach.container.multi

import utopia.firmament.component.container.many.MultiContainer
import utopia.firmament.component.stack.{HasStackSize, StackSizeCalculating}
import utopia.firmament.component.{DelayedBoundsUpdate, HasMutableBounds}
import utopia.firmament.context.BaseContext
import utopia.firmament.controller.Stacker
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
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
	def innerMargin: StackLength
	def outerMargin: StackLength
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
	  * @param margin Margin placed between the items in this collection
	  * @return Copy of this factory with the specified margin
	  */
	def withInnerMargin(margin: StackLength): Repr
	/**
	  * @param margin Margin placed at the edges of this collection
	  * @return Copy of this factory with the specified margin
	  */
	def withOuterMargin(margin: StackLength): Repr
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
			primaryAxis, insideRowLayout, betweenRowsLayout, innerMargin, outerMargin, splitThreshold,
			customDrawers)
		content attachTo collection
	}
}

case class CollectionFactory(parentHierarchy: ComponentHierarchy, primaryAxis: Axis2D = X,
                             insideRowLayout: StackLayout = Fit, betweenRowsLayout: StackLayout = Leading,
                             innerMargin: StackLength = StackLength.any,
                             outerMargin: StackLength = StackLength.fixedZero,
                             splitThreshold: Option[Double] = None, customDrawers: Vector[CustomDrawer] = Vector())
	extends CollectionFactoryLike[CollectionFactory]
		with NonContextualCombiningContainerFactory[Collection, ReachComponentLike]
		with FromGenericContextFactory[BaseContext, ContextualCollectionFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def withPrimaryAxis(axis: Axis2D): CollectionFactory = copy(primaryAxis = axis)
	override def withInsideRowLayout(layout: StackLayout): CollectionFactory = copy(insideRowLayout = layout)
	override def withBetweenRowsLayout(layout: StackLayout): CollectionFactory = copy(betweenRowsLayout = layout)
	override def withInnerMargin(margin: StackLength): CollectionFactory = copy(innerMargin = margin)
	override def withOuterMargin(margin: StackLength): CollectionFactory = copy(outerMargin = margin)
	override def withSplitThreshold(threshold: Double): CollectionFactory = copy(splitThreshold = Some(threshold))
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): CollectionFactory = copy(customDrawers = drawers)
	
	override def withContext[N <: BaseContext](context: N): ContextualCollectionFactory[N] =
		ContextualCollectionFactory[N](parentHierarchy, context, primaryAxis, insideRowLayout, betweenRowsLayout,
			outerMargin, splitThreshold, customDrawers)
}

case class ContextualCollectionFactory[+N <: BaseContext](parentHierarchy: ComponentHierarchy, context: N,
                                                          primaryAxis: Axis2D = X,
                                                          insideRowLayout: StackLayout = Fit,
                                                          betweenRowsLayout: StackLayout = Leading,
                                                          outerMargin: StackLength = StackLength.fixedZero,
                                                          splitThreshold: Option[Double] = None,
                                                          customDrawers: Vector[CustomDrawer] = Vector(),
                                                          customInnerMargin: Option[StackLength] = None,
                                                          areRelated: Boolean = false)
	extends CollectionFactoryLike[ContextualCollectionFactory[N]]
		with ContextualCombiningContainerFactory[N, BaseContext, Collection, ReachComponentLike, ContextualCollectionFactory]
{
	// COMPUTED --------------------------
	
	/**
	  * @return Copy of this factory that places the components closer to each other
	  */
	def related = copy(areRelated = true)
	
	
	// IMPLEMENTED  ----------------------
	
	override def withContext[N2 <: BaseContext](newContext: N2): ContextualCollectionFactory[N2] =
		copy(context = newContext)
	
	override def innerMargin: StackLength =
		customInnerMargin.getOrElse { if (areRelated) context.smallStackMargin else context.stackMargin }
	override def withPrimaryAxis(axis: Axis2D): ContextualCollectionFactory[N] = copy(primaryAxis = axis)
	override def withInsideRowLayout(layout: StackLayout): ContextualCollectionFactory[N] =
		copy(insideRowLayout = layout)
	override def withBetweenRowsLayout(layout: StackLayout): ContextualCollectionFactory[N] =
		copy(betweenRowsLayout = layout)
	override def withInnerMargin(margin: StackLength): ContextualCollectionFactory[N] =
		copy(customInnerMargin = Some(margin))
	override def withOuterMargin(margin: StackLength): ContextualCollectionFactory[N] = copy(outerMargin = margin)
	override def withSplitThreshold(threshold: Double): ContextualCollectionFactory[N] =
		copy(splitThreshold = Some(threshold))
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualCollectionFactory[N] =
		copy(customDrawers = drawers)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param margin Margin to place between the components in this collection (general)
	  * @return Copy of this factory with the specified margin
	  */
	def withInnerMargin(margin: SizeCategory): ContextualCollectionFactory[N] =
		withInnerMargin(context.margins.around(margin))
	/**
	  * @param margin Margin to place at the edges of this collection (general)
	  * @return Copy of this factory with the specified margin
	  */
	def withOuterMargin(margin: SizeCategory) =
		copy(outerMargin = context.margins.around(margin))
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
	def innerMargin: StackLength
	/**
	  * @return Margin placed at the edges of this collection
	  */
	def outerMargin: StackLength
	
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
						.mapDimension(primaryAxis) { _.withLowPriority }
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
				actualizeLayout(mySize, layouts.head._1)
			// Case: Not even the minimal layout is possible => Minimizes layout
			else if (layouts.last._2 >= maxBreadth)
				actualizeLayout(mySize, layouts.last._1, removeMargins = true)
			// Case: The fitting layout is somewhere between these two extremes => Finds and uses the best one that fits
			else
				layouts.find { _._2 <= maxBreadth }.foreach { case (layout, _) => actualizeLayout(mySize, layout) }
		}
	}
	
	
	// OTHER    --------------------------
	
	private def actualizeLayout(area: Size, layout: Vector[Vector[(ReachComponentLike, StackSize)]],
	                            removeMargins: Boolean = false) =
	{
		val actualInnerMargin = if (removeMargins) innerMargin.noMin else innerMargin
		val actualOuterMargin = if (removeMargins) outerMargin.noMin else outerMargin
		// Places the rows first
		val rowWrappers = layout.map { row =>
			val stackSize = Stacker.calculateStackSize(row.map { _._2 }, primaryAxis,
				actualInnerMargin, actualOuterMargin, insideRowLayout)
			new StackBoundsWrapper(stackSize)
		}
		val simulatedStackSize = Stacker.calculateStackSize(rowWrappers.map { _.stackSize }, secondaryAxis,
			actualInnerMargin, actualOuterMargin, betweenRowsLayout)
		Stacker.apply(rowWrappers, Bounds(Point.origin, area), simulatedStackSize(secondaryAxis).optimal,
			secondaryAxis, actualInnerMargin, actualOuterMargin, betweenRowsLayout)
		
		// Then places the components within the rows
		// The bounds updates are delayed
		layout.iterator.zip(rowWrappers).flatMap { case (row, rowWrapper) =>
			val itemWrappers = row.map { case (component, stackSize) => new DelayedBoundsUpdate(component, stackSize) }
			Stacker(itemWrappers, rowWrapper.bounds, rowWrapper.stackSize(primaryAxis).optimal,
				primaryAxis, actualInnerMargin, actualOuterMargin, insideRowLayout)
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
                          override val innerMargin: StackLength, override val outerMargin: StackLength,
                          override val splitThreshold: Option[Double], override val customDrawers: Vector[CustomDrawer])
	extends CustomDrawReachComponent with Collection
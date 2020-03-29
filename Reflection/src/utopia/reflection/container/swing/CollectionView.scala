package utopia.reflection.container.swing

import utopia.flow.datastructure.mutable.Lazy
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Bounds, Direction2D, Point, Size}
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.stack.{CachingStackable, StackLeaf}
import utopia.reflection.component.swing.{AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.StackLayout.{Fit, Leading, Trailing}
import utopia.reflection.container.stack.{MultiStackContainer, StackLayout, Stacker}
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.event.ResizeListener
import utopia.reflection.shape.{StackLength, StackSize}

import scala.collection.immutable.VectorBuilder

/**
 * This container places items in rows and columns, filling a 2D space
 * @author Mikko Hilpinen
 * @since 16.1.2020, v1
 */
class CollectionView[C <: AwtStackable](rowDirection: Direction2D, initialRowSplitThreshold: Double,
										margin: StackLength = StackLength.any, insideRowLayout: StackLayout = Fit,
										forceEqualRowLength: Boolean = false)
	extends AwtComponentWrapperWrapper with MultiStackContainer[C] with CachingStackable
		with SwingComponentRelated with AwtContainerRelated with CustomDrawableWrapper
{
	// ATTRIBUTES	-----------------------
	
	private var _rowSplitThreshold = initialRowSplitThreshold
	
	private var items = Vector[C]()
	
	private val panel = new Panel[C]
	
	private val rowsWithSizes = Lazy {
		itemsInRows.map { row => row -> Stacker.calculateStackSize(row.filter { _.isVisible }.map { _.stackSize },
			rowDirection.axis, margin, layout = insideRowLayout) }
	}
	
	
	// INITIAL CODE	-----------------------
	
	// Each time size changes, also updates content (doesn't reset stack sizes at this time)
	addResizeListener(updateLayout())
	
	
	// COMPUTED	---------------------------
	
	/**
	 * @return The length threshold at which point a new row is started
	 */
	def rowSplitThreshold = _rowSplitThreshold
	def rowSplitThreshold_=(newThreshold: Double) =
	{
		_rowSplitThreshold = newThreshold
		revalidate()
	}
	
	private def rowPlacementLayout =
	{
		if (forceEqualRowLength)
			Fit
		else if (rowDirection.isPositiveDirection)
			Leading
		else
			Trailing
	}
	
	private def rowSizes = rowsWithSizes.get.map { _._2 }
	
	private def itemsInRows =
	{
		val itemsForRows = items
		if (itemsForRows.isEmpty)
			Vector()
		else
		{
			val rowsBuilder = new VectorBuilder[StackRowBuilder]
			var currentRowBuilder = new StackRowBuilder(itemsForRows.head)
			itemsForRows.drop(1).foreach { item =>
				if (!currentRowBuilder.tryAdd(item))
				{
					rowsBuilder += currentRowBuilder
					currentRowBuilder = new StackRowBuilder(item)
				}
			}
			rowsBuilder += currentRowBuilder
			rowsBuilder.result().map { _.items }
		}
	}
	
	
	// IMPLEMENTED	-----------------------
	
	override def component = panel.component
	
	override protected def wrapped = panel
	
	override protected def updateVisibility(visible: Boolean) = super.isVisible_=(visible)
	
	override def drawable = panel
	
	override def components = items
	
	override protected def add(component: C) =
	{
		items :+= component
		panel += component
	}
	
	override protected def remove(component: C) =
	{
		items = items.filterNot { _ == component }
		panel -= component
	}
	
	override def calculatedStackSize =
	{
		// Places items in rows first and checks the length based on those
		Stacker.calculateStackSize(rowSizes, rowDirection.axis.perpendicular, margin, layout = rowPlacementLayout)
	}
	
	override def updateLayout() =
	{
		// Places each row first, then places each component in those rows
		val rows = rowsWithSizes.get.map { case (items, size) => new RowWrapper(items, size) }
		val rowPlacementAxis = rowDirection.axis.perpendicular
		Stacker(rows, Bounds(Point.origin, size), stackSize.along(rowPlacementAxis).optimal, rowPlacementAxis, margin,
			layout = rowPlacementLayout)
		rows.foreach { _.updateLayout() }
	}
	
	override def resetCachedSize() =
	{
		super.resetCachedSize()
		rowsWithSizes.reset()
	}
	
	
	// NESTED	---------------------------
	
	private class StackRowBuilder(firstItem: C)
	{
		// ATTRIBUTES	-------------------
		
		private var itemsBuilder = new VectorBuilder[C]
		private var length = if (firstItem.isVisible) firstItem.stackSize.along(rowDirection.axis).optimal else 0.0
		
		
		// INITIAL CODE	-------------------
		
		itemsBuilder += firstItem
		
		
		// COMPUTED	-----------------------
		
		// Row order may be opposite (down to up or right to left) on certain directions
		def items =
		{
			if (rowDirection.isPositiveDirection)
				itemsBuilder.result()
			else
				itemsBuilder.result().reverse
		}
		
		
		// OTHER	-----------------------
		
		def tryAdd(item: C) =
		{
			if (item.isVisible)
			{
				val itemLength = item.stackSize.along(rowDirection.axis).optimal
				if (length + itemLength > _rowSplitThreshold)
					false
				else
				{
					itemsBuilder += item
					length += itemLength
					true
				}
			}
			else
			{
				itemsBuilder += item
				true
			}
		}
	}
	
	private class RowWrapper(items: Vector[C], val stackSize: StackSize) extends StackLeaf
	{
		// ATTRIBUTES	-------------------
		
		private var _resizeListeners = Vector[ResizeListener]()
		private var _isVisible = true
		
		private var _position = Point.origin
		private var _size = Size.zero
		
		
		// IMPLEMENTED	-------------------
		
		override def updateLayout() =
		{
			// Places items within this "component"'s bounds
			// TODO: This method is getting called a few too many times
			Stacker(items, bounds, stackSize.along(rowDirection.axis).optimal, rowDirection.axis, margin,
				layout = insideRowLayout)
		}
		
		override def resetCachedSize() = Unit
		
		override def stackId = hashCode()
		
		override def resizeListeners = _resizeListeners
		
		override def resizeListeners_=(listeners: Vector[ResizeListener]) = _resizeListeners = listeners
		
		override def parent = Some(CollectionView.this)
		
		override def isVisible = _isVisible
		
		override def isVisible_=(isVisible: Boolean) = _isVisible = isVisible
		
		override def background = CollectionView.this.background
		
		override def background_=(color: Color) = CollectionView.this.background = color
		
		override def isTransparent = true
		
		override def fontMetrics = CollectionView.this.fontMetrics
		
		override def mouseButtonHandler = CollectionView.this.mouseButtonHandler
		
		override def mouseMoveHandler = CollectionView.this.mouseMoveHandler
		
		override def mouseWheelHandler = CollectionView.this.mouseWheelHandler
		
		override def keyStateHandler = CollectionView.this.keyStateHandler
		
		override def keyTypedHandler = CollectionView.this.keyTypedHandler
		
		override def position = _position
		
		override def position_=(p: Point) = _position = p
		
		override def size = _size
		
		override def size_=(s: Size) = _size = s
	}
}

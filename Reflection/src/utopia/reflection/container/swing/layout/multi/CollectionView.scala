package utopia.reflection.container.swing.layout.multi

import utopia.firmament.context.BaseContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.{Fit, Leading}
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.template.layout.ReflectionCollectionViewLike
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.shape.stack.StackLength

object CollectionView
{
	/**
	  * Creates a new collection view using contextual information
	  * @param rowAxis Axis of the rows (main increase direction) within this view
	  * @param rowSplitThreshold Pixel threshold after which a new row is started
	  * @param insideRowLayout Layout inside a row (default = Fit)
	  * @param forceEqualRowLength Whether all rows should have equal length (default = false)
	  * @param isRelated Whether items should be considered related (default = false)
	  * @param context Component creation context (implicit)
	  * @tparam C Type of components inside this view
	  * @return A new collection view
	  */
	def contextual[C <: AwtStackable](rowAxis: Axis2D, rowSplitThreshold: Double,
									  insideRowLayout: StackLayout = Fit, forceEqualRowLength: Boolean = false,
									  isRelated: Boolean = false)
	                                 (implicit context: BaseContext) =
		new CollectionView[C](rowAxis, rowSplitThreshold,
			if (isRelated) context.stackMargin else context.smallStackMargin, insideRowLayout,
			forceEqualRowLength)
}

/**
 * This container places items in rows and columns, filling a 2D space
 * @author Mikko Hilpinen
 * @since 24.4.2020, v1.2
 */
class CollectionView[C <: AwtStackable](rowAxis: Axis2D, initialRowSplitThreshold: Double,
                                        margin: StackLength = StackLength.any, insideRowLayout: StackLayout = Fit,
                                        forceEqualRowLength: Boolean = false)
	extends ReflectionCollectionViewLike[C, Stack[C], Stack[Stack[C]]] with StackableAwtComponentWrapperWrapper
		with SwingComponentRelated with AwtContainerRelated with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	-----------------------
	
	protected val container = {
		val layout = if (forceEqualRowLength) Fit else Leading
		new Stack[Stack[C]](rowAxis.perpendicular, margin, layout = layout)
	}
	
	private var _rowSplitThreshold = initialRowSplitThreshold
	
	
	// INITIAL CODE	-----------------------
	
	// Each time size changes, also updates content (doesn't reset stack sizes at this time)
	// addResizeListener(updateLayout())
	
	
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
	
	
	// IMPLEMENTED	-----------------------
	
	override def component = container.component
	
	override protected def collectionMaxCapacity = rowSplitThreshold
	
	override protected def spaceOf(component: C) = component.stackSize.along(rowAxis).optimal
	
	override protected def betweenComponentsSpace = margin.optimal
	
	override protected def newCollection() = new Stack[C](rowAxis, margin, layout = insideRowLayout)
	
	override def drawable = container
}

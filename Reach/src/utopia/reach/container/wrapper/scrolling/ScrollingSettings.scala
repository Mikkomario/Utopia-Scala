package utopia.reach.container.wrapper.scrolling

import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.Mutate
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.Dimensions

/**
  * Common trait for scrolling factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
trait ScrollingSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Margin placed between the scroll bar and the content, assuming that the bar is not drawn over
	  * the content.
	  * Wider edge margin as X and thinner edge margins as Y.
	  */
	def barMargin: Size
	/**
	  * May specify maximum optimal content width and/or height
	  */
	def maxOptimalLengths: Dimensions[Option[Double]]
	/**
	  * Whether this scroll area's size should never expand past that of the content's
	  */
	def limitsToContentSize: Boolean
	
	/**
	  * Margin placed between the scroll bar and the content, assuming that the bar is not drawn over
	  * the content.
	  * @param margin New bar margin to use.
	  *               Margin placed between the scroll bar and the content, assuming that the bar is
	  *               not drawn over the content.
	  * @return Copy of this factory with the specified bar margin
	  */
	def withBarMargin(margin: Size): Repr
	/**
	  * Whether this scroll area's size should never expand past that of the content's
	  * @param limit New limits to content size to use.
	  *              Whether this scroll area's size should never expand past that of the content's
	  * @return Copy of this factory with the specified limits to content size
	  */
	def withLimitsToContentSize(limit: Boolean): Repr
	/**
	  * May specify maximum optimal content width and/or height
	  * @param lengths New max optimal lengths to use.
	  *                May specify maximum optimal content width and/or height
	  * @return Copy of this factory with the specified max optimal lengths
	  */
	def withMaxOptimalLengths(lengths: Dimensions[Option[Double]]): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Copy of this factory that will limit container size to the content size
	  */
	def limitedToContentSize = withLimitsToContentSize(true)
	
	
	// OTHER	--------------------
	
	/**
	  * @param long Margin to place along the long edge of this scroll view
	  * @param ends Margin to place at each end of the scroll bar
	  * @return Copy of this factory with the specified margins
	  */
	def withBarMargin(long: Double, ends: Double): Repr = withBarMargin(Size(long, ends))
	/**
	  * @param margin Margin to place between the long (scrolling) edge of this view and the painted scroll bar
	  * @return Copy of this factory with the specified margin in place
	  */
	def withLongEdgeBarMargin(margin: Double): Repr = mapBarMargin { _.withX(margin) }
	/**
	  * @param cap Margin to place at each scroll bar end
	  * @return Copy of this factory with the specified margins
	  */
	def withBarCap(cap: Double) = mapBarMargin { _.withY(cap) }
	def mapBarMargin(f: Mutate[Size]) = withBarMargin(f(barMargin))
	
	def withMaxOptimalSize(size: Size) =
		withMaxOptimalLengths(size.dimensions.mapWithZero[Option[Double]](None) { Some(_) })
	def withMaxOptimalHeight(height: Double) = mapMaxOptimalLengths { _.withY(Some(height)) }
	def withMaxOptimalWidth(width: Double) = mapMaxOptimalLengths { _.withX(Some(width)) }
	def withMaxOptimalLengthAlong(axis: Axis2D, length: Double) =
		mapMaxOptimalLengths { _.withDimension(axis, Some(length)) }
	def mapMaxOptimalLengths(f: Mutate[Dimensions[Option[Double]]]) =
		withMaxOptimalLengths(f(maxOptimalLengths))
}

object ScrollingSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing scrolling containers
  * @param customDrawers       Custom drawers to assign to created components
  * @param barMargin           Margin placed between the scroll bar and the content, assuming
  *                            that the bar is not drawn over the content.
  * @param maxOptimalLengths   May specify maximum optimal content width and/or height
  * @param limitsToContentSize Whether this scroll area's size should never expand past that of
  *                            the content's
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
case class ScrollingSettings(customDrawers: Seq[CustomDrawer] = Empty,
                             barMargin: Size = Size.zero,
                             maxOptimalLengths: Dimensions[Option[Double]] = Dimensions.optional[Double].empty,
                             limitsToContentSize: Boolean = false)
	extends ScrollingSettingsLike[ScrollingSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withBarMargin(margin: Size) = copy(barMargin = margin)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withLimitsToContentSize(limit: Boolean) = copy(limitsToContentSize = limit)
	override def withMaxOptimalLengths(lengths: Dimensions[Option[Double]]) =
		copy(maxOptimalLengths = lengths)
}

/**
  * Common trait for factories that wrap a scrolling settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
trait ScrollingSettingsWrapper[+Repr] extends ScrollingSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ScrollingSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ScrollingSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def barMargin = settings.barMargin
	override def customDrawers = settings.customDrawers
	override def limitsToContentSize = settings.limitsToContentSize
	override def maxOptimalLengths = settings.maxOptimalLengths
	
	override def withBarMargin(margin: Size) = mapSettings { _.withBarMargin(margin) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapSettings { _.withCustomDrawers(drawers) }
	override def withLimitsToContentSize(limit: Boolean) = mapSettings { _.withLimitsToContentSize(limit) }
	override def withMaxOptimalLengths(lengths: Dimensions[Option[Double]]) =
		mapSettings { _.withMaxOptimalLengths(lengths) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ScrollingSettings => ScrollingSettings) = withSettings(f(settings))
}

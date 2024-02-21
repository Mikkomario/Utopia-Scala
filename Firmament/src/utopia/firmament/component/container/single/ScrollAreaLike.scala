package utopia.firmament.component.container.single

import utopia.firmament.component.stack.{CachingStackable, Stackable}
import utopia.firmament.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.firmament.model.ScrollBarBounds
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.DrawLevel2.Foreground
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.action.{Actor2, ActorHandler2}
import utopia.genesis.handling.event.consume.Consumable
import utopia.genesis.handling.event.consume.ConsumeChoice.Consume
import utopia.genesis.handling.event.keyboard.KeyboardEvents
import utopia.genesis.handling.event.mouse.MouseButtonStateListener2.MouseButtonStateEventFilter
import utopia.genesis.handling.event.mouse._
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.motion.motion2d.Velocity2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import java.awt.event.KeyEvent
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.collection.immutable.HashMap
import scala.concurrent.duration.FiniteDuration

/**
  * Scroll areas are containers that allow horizontal and / or vertical content scrolling
  * @author Mikko Hilpinen
  * @since 15.5.2019, Reflection v1+
  */
trait ScrollAreaLike[+C <: Stackable] extends CachingStackable
{
	// ATTRIBUTES	----------------
	
	private var barBounds: Map[Axis2D, ScrollBarBounds] = HashMap()
	private var scroller: Option[AnimatedScroller] = None
	
	
	// ABSTRACT	--------------------
	
	/**
	  * @return The content in this scrollable
	  */
	def content: C
	
	/**
	  * @return The scrolling axis / axes of this scroll view
	  */
	def axes: Seq[Axis2D]
	/**
	  * @return Margin placed at the wide edge of the scroll bar + margin placed at the thin edge of the scroll bar
	  */
	def scrollBarMargin: Size
	/**
	  * @return Whether this scroll view's maximum length should be limited to content length
	  */
	def limitsToContentSize: Boolean
	
	/**
	  * @return Whether the scroll bar should be placed over content (true) or besides it (false)
	  */
	def scrollBarIsInsideContent: Boolean
	/**
	  * @return The width of the scroll bar
	  */
	def scrollBarWidth: Double
	
	/**
	  * @return Amount of friction applied in pixels / millisecond&#94;2
	  */
	def friction: LinearAcceleration
	
	/**
	  * Registers a function to be called whenever this component is attached or detached from the main component
	  * hierarchy. If this component is already attached to the hierarchy when this method is called, should call
	  * the specified function immediately.
	  * @param listener A function that should be called whenever this component gets attached to or detached from the
	  *                 main component hierarchy. Accepts new attachment status (whether attached).
	  */
	protected def addHierarchyListener[U](listener: Boolean => U): Unit
	
	/**
	  * Repaints this scroll view
	  */
	def repaint(): Unit
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return The size of this view's contents
	  */
	def contentSize = content.size
	/**
	  * @return The top-left corner of this area's contents
	  */
	def contentOrigin = content.position
	def contentOrigin_=(pos: Point) = {
		val newContentPosition = minContentOrigin.bottomRight(pos).topLeft(Point.origin)
		if (content.position != newContentPosition) {
			content.position = newContentPosition
			updateScrollBarBounds(repaintAfter = true)
		}
	}
	
	/**
	  * @return The smallest possible content position (= position when scrolled at bottom right corner)
	  */
	def minContentOrigin = (size - contentSize).toPoint.topLeft(Point.origin)
	
	/**
	  * @return The current scroll modifier / percentage [0, 1]
	  */
	def scrollPercents = -contentOrigin / contentSize
	def scrollPercents_=(newPercents: HasDoubleDimensions) = scrollTo(newPercents, animated = false)
	
	/**
	  * @return The currently visible area inside the content
	  */
	def visibleContentArea = Bounds(-contentOrigin, size - scrollBarContentOverlap)
	
	/**
	  * @return Whether this scroll view allows 2-dimensional scrolling
	  */
	def allows2DScrolling = Axis2D.values.forall(axes.contains)
	
	/**
	  * @return Margin placed along the outer wide edge of the scroll bar
	  */
	def wideScrollBarMargin = scrollBarMargin.width
	
	/**
	  * @return Margin placed at each thinner edge of the scroll bar
	  */
	def edgeScrollBarMargin = scrollBarMargin.height
	
	private def scrollBarContentOverlap = {
		if (scrollBarIsInsideContent)
			Size.zero
		else
			axes.map { axis => Size(0, scrollBarWidth + scrollBarMargin.width, axis) }
				.reduceOption { _ + _ }.getOrElse(Size.zero)
	}
	
	
	// IMPLEMENTED	----------------
	
	override def children = Vector(content)
	
	override def calculatedStackSize = {
		val contentSize = content.stackSize
		StackSize.fromFunction2D { axis =>
			// Handles scrollable & non-scrollable axes differently
			if (axes.contains(axis)) {
				// Uses content size but may limit it in process
				val raw = contentSize(axis)
				val limited = (if (limitsToContentSize) raw else raw.noMax).lowPriority.noMin
				// May also expand according to scroll bar width
				if (scrollBarIsInsideContent)
					limited
				else
					limited + scrollBarWidth + scrollBarMargin.width
			}
			else
				contentSize(axis)
		}
	}
	
	// Updates content size & position
	override def updateLayout() = {
		// Non-scrollable content side is dependent from this component's side while scrollable side(s) are always set to optimal
		val contentSize = content.stackSize
		val contentAreaSize = size - scrollBarContentOverlap
		
		content.size = Size.fromFunction2D { axis =>
			if (axes.contains(axis)) {
				// If this area's maximum size is tied to that of the content, will not allow the content to
				// be smaller than this area
				if (limitsToContentSize)
					contentSize(axis).optimal max contentAreaSize(axis)
				else
					contentSize(axis).optimal
			}
			else
				contentAreaSize(axis)
		}
		
		// May scroll on content size change
		if (content.width >= contentAreaSize.width) {
			if (content.x + content.width < contentAreaSize.width)
				content.x = contentAreaSize.width - content.width
		}
		if (content.height >= contentAreaSize.height) {
			if (content.y + content.height < contentAreaSize.height)
				content.y = contentAreaSize.height - content.height
		}
		
		updateScrollBarBounds()
	}
	
	
	// OTHER	----------------------
	
	/**
	  * Scrolls to a specific percentage
	  * @param abovePercents The portion of the content that should be above this view [0, 1]
	  * @param animated Whether scrolling should be animated (default = true). If false, scrolling will be completed at once.
	  */
	def scrollTo(abovePercents: HasDoubleDimensions, animated: Boolean = true) = {
		val target = -contentSize.toPoint * abovePercents
		if (animated)
			animateScrollTo(target)
		else
			contentOrigin = target
	}
	/**
	  * Scrolls to a specific percentage on a single axis
	  * @param abovePercent The portion of the content that should be above this view [0, 1]
	  * @param axis The axis on which the scrolling is applied
	  * @param animated Whether scrolling should be animated (default = true). If false, scrolling will be completed at once.
	  */
	def scrollTo(abovePercent: Double, axis: Axis2D, animated: Boolean) = {
		val target = contentOrigin.withDimension(contentSize.along(axis) * -abovePercent)
		if (animated)
			animateScrollTo(target)
		else
			contentOrigin = target
	}
	/**
	  * Scrolls to a specific percentage on a single axis
	  * @param abovePercent The portion of the content that should be above this view [0, 1]
	  * @param axis The axis on which the scrolling is applied
	  */
	def scrollTo(abovePercent: Double, axis: Axis2D): Unit = () // scrollTo(abovePercent, axis, animated = true)
	
	/**
	  * Scrolls this view a certain amount
	  * @param amounts The scroll vector
	  */
	def scroll(amounts: HasDoubleDimensions, animated: Boolean = true, preservePreviousMomentum: Boolean = true) = {
		if (animated)
			animateScrollTo(contentOrigin + amounts, preservePreviousMomentum)
		else
			contentOrigin += amounts
	}
	
	/**
	  * Scrolls to the left edge, if horizontal scrolling is supported
	  * @param animated Whether scrolling should be animated
	  */
	def scrollToLeft(animated: Boolean = true) = scrollTo(0, X, animated)
	/**
	  * Scrolls to the right edge, if horizontal scrolling is supported
	  * @param animated Whether scrolling should be animated
	  */
	def scrollToRight(animated: Boolean = true) = {
		val target = contentOrigin.withX(minContentOrigin.x)
		if (animated)
			animateScrollTo(target)
		else
			contentOrigin = target
	}
	/**
	  * Scrolls to the top, if vertical scrolling is supported
	  * @param animated Whether scrolling should be animated
	  */
	def scrollToTop(animated: Boolean = true) = scrollTo(0, Y, animated)
	/**
	  * Scrolls to the bottom, if vertical scrolling is supported
	  * @param animated Whether scrolling should be animated
	  */
	def scrollToBottom(animated: Boolean = true) = {
		val target = contentOrigin.withY(minContentOrigin.y)
		if (animated)
			animateScrollTo(target)
		else
			contentOrigin = target
	}
	
	/**
	  * Makes sure the specified area is (fully) visible in this scroll view
	  * @param area The target area (within content's relative space
	  *             (Eg. position (0, 0) is considered the top left corner of content))
	  */
	def ensureAreaIsVisible(area: Bounds, animated: Boolean = true) = {
		// Performs calculations in scroll view's relative space
		val areaInViewSpace = area + contentOrigin
		// Calculates how much scrolling is required
		val translation = areaInViewSpace.dimensions.mergeWith(size, 0.0) { (area, myLength) =>
			if (area.start < 0)
				-area.start
			else if (area.end > myLength)
				myLength - area.end
			else
				0.0
		}
		// Performs actual scrolling
		scroll(translation, animated, preservePreviousMomentum = false)
	}
	
	protected def drawWith(barDrawer: ScrollBarDrawerLike, drawer: Drawer) = {
		Axis2D.values.foreach { axis =>
			if ((!scrollBarIsInsideContent) || lengthAlong(axis) < contentSize(axis))
				barBounds.get(axis).foreach { b => barDrawer.draw(drawer, b + position, axis) }
		}
	}
	
	/**
	  * Converts a scroll bar drawer to a custom drawer, which should then be added to this view
	  * @param barDrawer A scroll bar drawer
	  * @return A custom drawer based on the scroll bar drawer
	  */
	protected def scrollBarDrawerToCustomDrawer(barDrawer: ScrollBarDrawerLike) =
		CustomDrawer(Foreground) { (d, _) => drawWith(barDrawer, d) }
	
	/**
	  * Sets up animated scrolling for this scroll area. This method doesn't need to be called if
	  * setupMouseHandling is called
	  * @param actorHandler The actor handler that will deliver action events
	  */
	private def setupAnimatedScrolling(actorHandler: ActorHandler2) = {
		if (scroller.isEmpty) {
			val newScroller = new AnimatedScroller
			scroller = Some(newScroller)
			
			// Animated scrolling is enabled only while this area is attached to the main stack hierarchy
			addHierarchyListener { isAttached =>
				if (isAttached)
					actorHandler += newScroller
				else
					actorHandler -= newScroller
			}
		}
	}
	
	/**
	  * Sets up mouse handling for this view
	  * @param actorHandler Actor handler that will allow velocity handling
	  * @param scrollPerWheelClick How many pixels should be scrolled at each wheel "click"
	  * @param dragDuration The maximum drag duration when concerning velocity tracking (default = 0.5 seconds)
	  * @param velocityMod A modifier applied to velocity (default = 1.0)
	  */
	protected def setupMouseHandling(actorHandler: ActorHandler2, scrollPerWheelClick: Double,
	                                 dragDuration: FiniteDuration = 300.millis, velocityMod: Double = 1.0) =
	{
		setupAnimatedScrolling(actorHandler)
		val listener = new MouseListener(scrollPerWheelClick, dragDuration, velocityMod, scroller.get)
		
		// Listens to mouse and keyboard events when attached to the main stack hierarchy
		addHierarchyListener { isAttached =>
			if (isAttached) {
				this += listener
				CommonMouseEvents += listener.MouseReleaseListener
			}
			else {
				this -= listener
				CommonMouseEvents -= listener.MouseReleaseListener
			}
		}
	}
	
	/**
	  * Scrolls content to certain position by affecting scrolling speed. This way the scrolling is animated.
	  * @param newContentOrigin Targeted new content origin
	  */
	protected def animateScrollTo(newContentOrigin: Point, preservePreviousMomentum: Boolean = false) =
	{
		if (scroller.isDefined) {
			// Calculates duration first (in milliseconds)
			// t = Sqrt(2D/a)
			val distanceVector = (newContentOrigin - contentOrigin).toVector
			val durationMillis = Math.sqrt(2 * distanceVector.length / friction.abs.perMilliSecond.perMilliSecond)
			// Then calculates the required velocity so that movement ends at target position after duration t
			// V = at
			val newVelocity = friction(durationMillis.millis).abs
			
			// Sets new velocity
			val newVelocityVector = newVelocity.withDirection(distanceVector.direction)
			/*
			if (preservePreviousMomentum && !currentVelocity.isZero)
			{
				if (!newVelocityVector.isZero)
				{
					// Won't continue traversing to opposite direction
					if ((newVelocityVector.direction - currentVelocity.direction).degrees.abs <= 90 ||
						newVelocity > currentVelocity.linear)
						scroller.get.velocity += newVelocityVector
					else
						scroller.get.velocity = Velocity.zero
				}
			}*/
			if (preservePreviousMomentum)
				scroller.get.velocity += newVelocityVector
			else
				scroller.get.velocity = newVelocityVector
		}
		else
			contentOrigin = newContentOrigin
	}
	
	/**
	  * Updates the scroll bar position and size etc
	  * @param repaintAfter Whether this scroll area should be repainted afterwards (default = false)
	  */
	protected def updateScrollBarBounds(repaintAfter: Boolean = false) = {
		if (contentSize.area == 0) {
			if (barBounds.nonEmpty)
				barBounds = HashMap()
		}
		else {
			barBounds = axes.map { axis =>
				// Calculates the size of the scroll area
				val barAreaSize = axis match {
					case X => Size(width - edgeScrollBarMargin * 2, scrollBarWidth)
					case Y => Size(scrollBarWidth, height - edgeScrollBarMargin * 2)
				}
				
				val length = lengthAlong(axis)
				val contentLength = content.lengthAlong(axis)
				val myBreadth = lengthAlong(axis.perpendicular)
				
				// Calculates scroll bar size
				val barLengthMod = (length / contentLength) min 1.0
				val barSize = barAreaSize.scaledAlong(axis(barLengthMod))
				
				// Calculates the positions of scroll bar area + bar itself
				val barAreaPosition = Point(myBreadth - scrollBarWidth - wideScrollBarMargin, edgeScrollBarMargin,
					axis.perpendicular)
				
				axis -> ScrollBarBounds(Bounds(
					barAreaPosition + axis(barAreaSize(axis) * scrollPercents(axis)), barSize),
					Bounds(barAreaPosition, barAreaSize), axis)
			}.toMap
			
			// Repaints scroll area and content
			if (repaintAfter)
				repaint()
			// Repaints the newly calculated bar area(s)
			// barBounds.foreach { case (_, bounds) => repaint(bounds.area) }
		}
	}
	
	
	// NESTED CLASSES	-----------------------
	
	private class AnimatedScroller extends Actor2
	{
		// ATTRIBUTES	-----------------------
		
		private val velocityPointer = EventfulPointer(Velocity2D.zero)
		override val handleCondition: FlagLike = velocityPointer.map { _.amount.nonZero }
		
		
		// COMPUTED ---------------------------
		
		def velocity = velocityPointer.value
		def velocity_=(newVelocity: Velocity2D) = velocityPointer.value = newVelocity
		
		
		// IMPLEMENTED	-----------------------
		
		override def act(duration: FiniteDuration) = {
			// Calculates the amount of scrolling and velocity after applying friction
			val (transition, newVelocity) = velocity(duration, -friction.abs, preserveDirection = true)
			
			// Applies velocity
			if (allows2DScrolling)
				scroll(transition, animated = false)
			else
				axes.foreach { axis => scroll(transition.projectedOver(axis), animated = false) }
			
			// Applies friction to velocity
			velocity = newVelocity
			
			// TODO: Should probably stop momentum once edge of scroll area is reached
		}
		
		
		// OTHER	---------------------------
		
		def stop() = velocity = Velocity2D.zero
		
		def accelerate(acceleration: Velocity2D) = velocity += acceleration
	}
	
	private class MouseListener(val scrollPerWheelClick: Double, val dragDuration: FiniteDuration,
	                            val velocityMod: Double, val scroller: AnimatedScroller)
		extends MouseButtonStateListener2 with MouseMoveListener2 with MouseWheelListener2
	{
		// ATTRIBUTES	-----------------------
		
		private var isDraggingBar = false
		private var barDragPosition = Point.origin
		private var barDragAxis: Axis2D = X
		
		private var isDraggingContent = false
		private var contentDragPosition = Point.origin
		
		private var velocities = Vector[(Instant, Velocity2D, FiniteDuration)]()
		
		// Listens to left mouse presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent2.filter.leftPressed
		// Only listens to wheel events inside component bounds
		override val mouseWheelEventFilter = Consumable.unconsumedFilter && MouseEvent2.filter.over(bounds)
		
		
		// IMPLEMENTED	-----------------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
			// Performs some calculations in this component's context
			val relativeEvent = event.relativeTo(position)
			
			if (event.pressed) {
				// If mouse was pressed inside inside scroll bar, starts dragging the bar
				val barUnderEvent = axes.findMap { axis =>
					barBounds.get(axis).filter { b => relativeEvent.isOver(b.bar) }.map { axis -> _.bar }
				}
				
				if (barUnderEvent.isDefined) {
					isDraggingContent = false
					barDragAxis = barUnderEvent.get._1
					barDragPosition = relativeEvent.position.relative - barUnderEvent.get._2.position
					isDraggingBar = true
					scroller.stop()
				}
				// Consumed pressed events are only considered in scroll bar(s)
				// if outside, starts drag scrolling
				// TODO: This not consumed -requirement should likely be customizable
				else if (event.unconsumed && event.isOver(bounds)) {
					isDraggingBar = false
					contentDragPosition = event.position
					isDraggingContent = true
					scroller.stop()
				}
			}
		}
		
		override def onMouseMove(event: MouseMoveEvent2) = {
			// If dragging scroll bar, scrolls the content
			if (isDraggingBar) {
				val newBarOrigin = event.position.relative - bounds.position - barDragPosition
				scrollTo(newBarOrigin(barDragAxis) / lengthAlong(barDragAxis), barDragAxis, animated = false)
			}
			// If dragging content, updates scrolling and remembers velocity
			else if (isDraggingContent) {
				// Drag scrolling is different when both axes are being scrolled
				if (allows2DScrolling)
					scroll(event.transition, animated = false)
				else
					axes.foreach { axis => scroll(event.transition.projectedOver(axis), animated = false) }
				
				val now = Now.toInstant
				velocities = velocities.dropWhile { _._1 < now - dragDuration } :+ (now, event.velocity, event.duration)
			}
		}
		
		// When wheel is rotated inside component bounds, scrolls
		override def onMouseWheelRotated(event: MouseWheelEvent2) = {
			// in 2D scroll views, X-scrolling is applied only if shift is being held
			val scrollAxis = {
				if (allows2DScrolling) {
					if (KeyboardEvents.state(KeyEvent.VK_SHIFT)) X else Y
				}
				else
					axes.headOption getOrElse Y
			}
			
			scroll(scrollAxis(-event.wheelTurn * scrollPerWheelClick), animated = false)
			Consume(s"Scroll area scrolling along axis $scrollAxis")
		}
		
		
		// NESTED	--------------------------------
		
		/**
		  * Listens to global mouse release events
		  */
		object MouseReleaseListener extends MouseButtonStateListener2
		{
			// ATTRIBUTES   ------------------------
			
			override val mouseButtonStateEventFilter = MouseButtonStateEventFilter.leftReleased
			
			
			// IMPLEMENTED  ------------------------
			
			override def handleCondition: FlagLike = AlwaysTrue
			
			override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
				// When mouse is released, stops dragging. May apply scrolling velocity
				isDraggingBar = false
				if (isDraggingContent) {
					isDraggingContent = false
					
					// Calculates the scrolling velocity
					val now = Instant.now
					val velocityData = velocities.dropWhile { _._1 < now - dragDuration }
					velocities = Vector()
					
					if (velocityData.nonEmpty) {
						val actualDragDuration = now - velocityData.head._1
						val averageTranslationPerMilli = velocityData.map {
							case (_, v, d) => v.perMilliSecond * d.toPreciseMillis }
							.reduce { _ + _ } / actualDragDuration.toPreciseMillis
						scroller.accelerate(Velocity2D(averageTranslationPerMilli)(TimeUnit.MILLISECONDS) * velocityMod)
					}
				}
			}
		}
	}
}

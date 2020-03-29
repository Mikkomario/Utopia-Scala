package utopia.reflection.container.stack

import java.awt.event.KeyEvent
import java.time.Instant
import java.util.concurrent.TimeUnit

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.event._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{Actor, KeyStateListener, MouseButtonStateListener, MouseMoveListener, MouseWheelListener}
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.{Axis2D, LinearAcceleration, Vector3D, VectorLike, Velocity}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.drawing.template.DrawLevel.Foreground
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.stack.{CachingStackable, Stackable}
import utopia.reflection.shape.{ScrollBarBounds, StackLengthLimit, StackSize}

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{Duration, FiniteDuration}

object ScrollAreaLike
{
	/**
	  * The scrolling friction that should be used by default (= 2000 pixels/s&#94;2)
	  */
	val defaultFriction = LinearAcceleration(2000)(TimeUnit.SECONDS)
}

/**
  * Scroll areas are containers that allow horizontal and / or vertical content scrolling
  * @author Mikko Hilpinen
  * @since 15.5.2019, v1+
  */
trait ScrollAreaLike[C <: Stackable] extends CachingStackable with StackContainerLike[C]
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
	  * @return Limits applied to this area's stack lengths
	  */
	def lengthLimits: Map[Axis2D, StackLengthLimit]
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
	def scrollBarWidth: Int
	
	/**
	  * @return Amount of friction applied in pixels / millisecond&#94;2
	  */
	def friction: LinearAcceleration
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return The size of this view's contents
	  */
	def contentSize = content.size
	/**
	  * @return The top-left corner of this area's contents
	  */
	def contentOrigin = content.position
	def contentOrigin_=(pos: Point) =
	{
		content.position = minContentOrigin.bottomRight(pos).topLeft(Point.origin)
		updateScrollBarBounds()
	}
	
	/**
	  * @return The smallest possible content position (= position when scrolled at bottom right corner)
	  */
	def minContentOrigin = (size - contentSize).toPoint.topLeft(Point.origin)
	
	/**
	  * @return The current scroll modifier / percentage [0, 1]
	  */
	def scrollPercents = -contentOrigin / contentSize
	def scrollPercents_=(newPercents: VectorLike[_]) = scrollTo(newPercents, animated = false)
	
	/**
	  * @return The currently visible area inside the content
	  */
	def visibleContentArea = Bounds(-contentOrigin, size - scrollBarContentOverlap)
	
	/**
	  * @return Whether this scroll view allows 2-dimensional scrolling
	  */
	def allows2DScrolling = Axis2D.values.forall(axes.contains)
	
	private def scrollBarContentOverlap =
	{
		if (scrollBarIsInsideContent)
			Size.zero
		else
			axes.map { Size(0, scrollBarWidth, _) } reduceOption { _ + _ } getOrElse Size.zero
	}
	
	/**
	  * Repaints this scroll view
	  * @param bounds The area that needs repainting
	  */
	def repaint(bounds: Bounds): Unit
	
	
	// IMPLEMENTED	----------------
	
	override def components = Vector(content)
	
	override def calculatedStackSize =
	{
		val contentSize = content.stackSize
		val lengths = Axis2D.values.map
		{
			axis =>
				// Handles scrollable & non-scrollable axes differently
				if (axes.contains(axis))
				{
					// Uses content size but may limit it in process
					val raw = contentSize.along(axis)
					val limit = lengthLimits.get(axis)
					val limited = limit.map(raw.within) getOrElse raw
					axis -> (if (limitsToContentSize) limited else if (limit.exists { _.max.isDefined }) limited else
						limited.noMax).withLowPriority.noMin
				}
				else
					axis -> contentSize.along(axis)
		}.toMap
		
		StackSize(lengths(X), lengths(Y))
	}
	
	// Updates content size & position
	override def updateLayout() =
	{
		// Non-scrollable content side is dependent from this component's side while scrollable side(s) are always set to optimal
		val contentSize = content.stackSize
		val contentAreaSize = size - scrollBarContentOverlap
		
		val lengths: Map[Axis2D, Double] = Axis2D.values.map
		{
			axis =>
				if (axes.contains(axis))
				{
					// If this area's maximum size is tied to that of the content, will not allow the content to
					// be smaller than this area
					if (limitsToContentSize)
						axis -> (contentSize.along(axis).optimal.toDouble max contentAreaSize.along(axis))
					else
						axis -> contentSize.along(axis).optimal.toDouble
				}
				else
					axis -> contentAreaSize.along(axis)
		}.toMap
		
		content.size = Size(lengths(X), lengths(Y))
		
		// May scroll on content size change
		if (content.width >= contentAreaSize.width)
		{
			if (content.x + content.width < contentAreaSize.width)
				content.x = contentAreaSize.width - content.width
		}
		if (content.height >= contentAreaSize.height)
		{
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
	def scrollTo(abovePercents: VectorLike[_], animated: Boolean = true) =
	{
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
	def scrollTo(abovePercent: Double, axis: Axis2D, animated: Boolean) =
	{
		val target = contentOrigin.withCoordinate(-contentSize.along(axis) * abovePercent, axis)
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
	def scrollTo(abovePercent: Double, axis: Axis2D): Unit = scrollTo(abovePercent, axis, animated = true)
	
	/**
	  * Scrolls this view a certain amount
	  * @param amounts The scroll vector
	  */
	def scroll(amounts: VectorLike[_], animated: Boolean = true, preservePreviousMomentum: Boolean = true) =
	{
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
	def scrollToRight(animated: Boolean = true) =
	{
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
	def scrollToBottom(animated: Boolean = true) =
	{
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
	def ensureAreaIsVisible(area: Bounds, animated: Boolean = true) =
	{
		// Performs calculations in scroll view's relative space
		val areaInViewSpace = area + contentOrigin
		
		// Calculates how much scrolling is required
		val xTransition =
		{
			if (areaInViewSpace.x < 0)
				areaInViewSpace.x
			else if (areaInViewSpace.rightX > width)
				areaInViewSpace.rightX - width
			else
				0
		}
		val yTransition =
		{
			if (areaInViewSpace.y < 0)
				areaInViewSpace.y
			else if (areaInViewSpace.bottomY > height)
				areaInViewSpace.bottomY - height
			else
				0
		}
		
		// Performs actual scrolling
		scroll(Vector3D(-xTransition, -yTransition), animated, preservePreviousMomentum = false)
	}
	
	protected def drawWith(barDrawer: ScrollBarDrawer, drawer: Drawer) = Axis2D.values.foreach
	{
		axis =>
			if ((!scrollBarIsInsideContent) || lengthAlong(axis) < contentSize.along(axis))
				barBounds.get(axis).foreach { barDrawer.draw(drawer, _, axis) }
	}
	
	/**
	  * Converts a scroll bar drawer to a custom drawer, which should then be added to this view
	  * @param barDrawer A scroll bar drawer
	  * @return A custom drawer based on the scroll bar drawer
	  */
	protected def scrollBarDrawerToCustomDrawer(barDrawer: ScrollBarDrawer) = CustomDrawer(Foreground) {
		(d, _) => drawWith(barDrawer, d) }
	
	/**
	  * Sets up animated scrolling for this scroll area. This method doesn't need to be called if
	  * setupMouseHandling is called
	  * @param actorHandler The actor handler that will deliver action events
	  */
	private def setupAnimatedScrolling(actorHandler: ActorHandler) =
	{
		if (scroller.isEmpty)
		{
			val newScroller = new AnimatedScroller
			scroller = Some(newScroller)
			actorHandler += newScroller
		}
	}
	
	/**
	  * Sets up mouse handling for this view
	  * @param actorHandler Actor handler that will allow velocity handling
	  * @param scrollPerWheelClick How many pixels should be scrolled at each wheel "click"
	  * @param dragDuration The maximum drag duration when concerning velocity tracking (default = 0.5 seconds)
	  * @param friction Friction applied to velocity (pixels / millisecond, default = 0.1)
	  * @param velocityMod A modifier applied to velocity (default = 1.0)
	  */
	protected def setupMouseHandling(actorHandler: ActorHandler, scrollPerWheelClick: Double,
									 dragDuration: Duration = 300.millis, friction: Double = 0.1,
									 velocityMod: Double = 1.0) =
	{
		setupAnimatedScrolling(actorHandler)
		val listener = new MouseListener(scrollPerWheelClick, dragDuration, velocityMod, scroller.get)
		
		addMouseButtonListener(listener)
		addMouseMoveListener(listener)
		addMouseWheelListener(listener)
		addKeyStateListener(listener)
	}
	
	/**
	  * Scrolls content to certain position by affecting scrolling speed. This way the scrolling is animated.
	  * @param newContentOrigin Targeted new content origin
	  */
	protected def animateScrollTo(newContentOrigin: Point, preservePreviousMomentum: Boolean = false) =
	{
		if (scroller.isDefined)
		{
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
	
	private def updateScrollBarBounds() =
	{
		if (contentSize.area == 0)
		{
			if (barBounds.nonEmpty)
				barBounds = HashMap()
		}
		else
		{
			barBounds = axes.map
			{
				axis =>
					// Calculates the size of the scroll area
					val barAreaSize = axis match
					{
						case X => Size(width, scrollBarWidth)
						case Y => Size(scrollBarWidth, height)
					}
					
					val length = lengthAlong(axis)
					val contentLength = content.lengthAlong(axis)
					val myBreadth = lengthAlong(axis.perpendicular)
					
					// Calculates scroll bar size
					val barLengthMod = (length / contentLength) min 1.0
					val barSize = barAreaSize * (barLengthMod, axis)
					
					// Calculates the positions of scroll bar area + bar itself
					val barAreaPosition = Point(myBreadth - scrollBarWidth, 0, axis.perpendicular)
					
					axis -> ScrollBarBounds(Bounds(barAreaPosition + (barAreaSize.along(axis) * scrollPercents.along(axis),
						axis), barSize), Bounds(barAreaPosition, barAreaSize), axis)
			}.toMap
			
			val repaintBounds = Bounds.around(barBounds.values.map { _.area })
			repaint(repaintBounds)
		}
	}
	
	
	// NESTED CLASSES	-----------------------
	
	private class AnimatedScroller extends Actor with Handleable
	{
		// ATTRIBUTES	-----------------------
		
		var velocity = Velocity.zero
		
		
		// IMPLEMENTED	-----------------------
		
		override def act(duration: FiniteDuration) =
		{
			if (velocity.amount != Vector3D.zero)
			{
				// Calculates the amount of scrolling and velocity after applying friction
				val (transition, newVelocity) = velocity(duration, -friction.abs, preserveDirection = true)
				
				// Applies velocity
				if (allows2DScrolling)
					scroll(transition, animated = false)
				else
					axes.foreach { axis => scroll(transition.projectedOver(axis), animated = false) }
				
				// Applies friction to velocity
				velocity = newVelocity
			}
			
			// TODO: Should probably stop momentum once edge of scroll area is reached
		}
		
		
		// OTHER	---------------------------
		
		def stop() = velocity = Velocity.zero
		
		def accelerate(acceleration: Velocity) = velocity += acceleration
	}
	
	private class MouseListener(val scrollPerWheelClick: Double, val dragDuration: Duration, val velocityMod: Double,
								val scroller: AnimatedScroller) extends MouseButtonStateListener with MouseMoveListener
		with MouseWheelListener with Handleable with KeyStateListener
	{
		// ATTRIBUTES	-----------------------
		
		private var isDraggingBar = false
		private var barDragPosition = Point.origin
		private var barDragAxis: Axis2D = X
		
		private var isDraggingContent = false
		private var contentDragPosition = Point.origin
		
		private var velocities = Vector[(Instant, Velocity, Duration)]()
		
		private var keyState = KeyStatus.empty
		
		
		// IMPLEMENTED	-----------------------
		
		// Listens to left mouse presses & releases
		override def mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
		
		// Only listens to wheel events inside component bounds
		override def mouseWheelEventFilter = Consumable.notConsumedFilter && MouseEvent.isOverAreaFilter(bounds)
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			// Performs some calculations in this component's context
			val relativeEvent = event.relativeTo(position)
			
			if (event.wasPressed)
			{
				// If mouse was pressed inside inside scroll bar, starts dragging the bar
				val barUnderEvent = axes.findMap { axis =>
					barBounds.get(axis).filter { b => relativeEvent.isOverArea(b.bar) }.map { axis -> _.bar }
				}
				
				if (barUnderEvent.isDefined)
				{
					isDraggingContent = false
					barDragAxis = barUnderEvent.get._1
					barDragPosition = relativeEvent.positionOverArea(barUnderEvent.get._2)
					isDraggingBar = true
					scroller.stop()
				}
				// Consumed pressed events are only considered in scroll bar(s)
				// if outside, starts drag scrolling
				else if (event.isOverArea(bounds) && !event.isConsumed)
				{
					isDraggingBar = false
					contentDragPosition = event.mousePosition
					isDraggingContent = true
					scroller.stop()
				}
			}
			else
			{
				// TODO: Handle mouse releases on a global scale (use root mouse event handler)
				// When mouse is released, stops dragging. May apply scrolling velocity
				isDraggingBar = false
				if (isDraggingContent)
				{
					isDraggingContent = false
					
					// Calculates the scrolling velocity
					val now = Instant.now
					val velocityData = velocities.dropWhile { _._1 < now - dragDuration }
					velocities = Vector()
					
					if (velocityData.nonEmpty)
					{
						val actualDragDuration = now - velocityData.head._1
						val averageTranslationPerMilli = velocityData.map {
							case (_, v, d) => v.perMilliSecond * d.toPreciseMillis }
							.reduce { _ + _ } / actualDragDuration.toPreciseMillis
						
						scroller.accelerate(Velocity(averageTranslationPerMilli)(TimeUnit.MILLISECONDS) * velocityMod)
					}
				}
			}
			None
		}
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			// If dragging scroll bar, scrolls the content
			if (isDraggingBar)
			{
				val newBarOrigin = event.positionOverArea(bounds) - barDragPosition
				scrollTo(newBarOrigin.along(barDragAxis) / lengthAlong(barDragAxis), barDragAxis, animated = false)
			}
			// If dragging content, updates scrolling and remembers velocity
			else if (isDraggingContent)
			{
				// Drag scrolling is different when both axes are being scrolled
				if (allows2DScrolling)
					scroll(event.transition, animated = false)
				else
					axes.foreach { axis => scroll(event.transition.projectedOver(axis), animated = false) }
				
				val now = Instant.now
				velocities = velocities.dropWhile { _._1 < now - dragDuration } :+ (now, event.velocity, event.duration)
			}
		}
		
		// When wheel is rotated inside component bounds, scrolls
		override def onMouseWheelRotated(event: MouseWheelEvent) =
		{
			// in 2D scroll views, X-scrolling is applied only if shift is being held
			val scrollAxis =
			{
				if (allows2DScrolling)
				{
					if (keyState(KeyEvent.VK_SHIFT))
						X
					else
						Y
				}
				else
					axes.headOption getOrElse Y
			}
			
			scroll(scrollAxis(-event.wheelTurn * scrollPerWheelClick), animated = false)
			Some(ConsumeEvent(s"Scroll area scrolling along axis $scrollAxis"))
		}
		
		override def onKeyState(event: KeyStateEvent) = keyState = event.keyStatus
	}
}

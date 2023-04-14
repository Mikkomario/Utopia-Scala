package utopia.reach.test

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.color.Color
import utopia.genesis.event.KeyStateEvent
import utopia.paradigm.generic.ParadigmDataType
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.image.Image
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.shape.shape2d.Size
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.image.{ImageLabel, ViewImageLabel}
import utopia.reach.container.ReachCanvas2
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize
import utopia.reach.container.multi.ViewStack
import utopia.reflection.util.SingleFrameSetup

import java.awt.event.KeyEvent

/**
  * Tests combination of image labels and view stack
  * @author Mikko Hilpinen
  * @since 26.1.2021, v0.1
  */
object ImageViewStackTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	ParadigmDataType.setup()
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val icon1 = Image.readFrom("Reach/test-images/cursor-arrow.png").get
	val icon2 = Image.readFrom("Reach/test-images/cursor-hand.png").get.withCenterOrigin * 2
	val icon3 = Image.readFrom("Reach/test-images/cursor-text.png").get
	
	val pointer1 = new PointerWithEvents(false)
	val pointer2 = new PointerWithEvents(false)
	val pointer3 = new PointerWithEvents(false)
	/*
	val canvas = ReachCanvas2(cursors) { hierarchy =>
		ViewStack(hierarchy).builder(Mixed)
			.withFixedStyle(X, Center, margins.verySmall.downscaling.withLowPriority,
				customDrawers = Vector(BackgroundDrawer(Color.magenta))) { factories =>
				Vector(
					factories.next()(ImageLabel).apply(icon1, customDrawers = Vector(BackgroundDrawer(Color.green))) -> pointer1,
					factories.next()(ViewImageLabel).apply(pointer3.map { if (_) icon2 else icon3 },
						customDrawers = Vector(BackgroundDrawer(Color.cyan))) -> pointer2
				)
			}
	}
	
	pointer2.addListener { e => println(s"label 2 visibility: ${e.newValue}") }
	
	GlobalKeyboardEventHandler += KeyStateListener(KeyStateEvent.wasPressedFilter) { event =>
		event.index match
		{
			case KeyEvent.VK_1 => pointer1.update { !_ }
			case KeyEvent.VK_2 => pointer2.update { !_ }
			case KeyEvent.VK_3 => pointer3.update { !_ }
		}
	}
	
	val testSize = Size(128, 64)
	
	canvas.child.size = testSize
	canvas.child.addConstraint { _ => StackSize.fixed(testSize) }
	canvas.size = testSize
	
	val frame = Frame.windowed(canvas.parent, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	 */
}

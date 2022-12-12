package utopia.reach.test

import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.color.Hsl
import utopia.paradigm.angular.Angle
import utopia.paradigm.shape.shape2d.Size
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reach.component.wrapper.Open
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.ScrollArea
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.SingleFrameSetup

import java.awt.event.KeyEvent

/**
  * A test app for scroll areas
  * @author Mikko Hilpinen
  * @since 9.12.2020, v0.1
  */
object ReachScrollAreaTest extends App
{
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	val blockSize = StackSize.any(Size(96, 96))
	val altBlockSize = blockSize * 0.75
	val colorIterator = Iterator.continually { Hsl(Angle.ofDegrees(math.random() * 360), 1, 0.5) }
	val bg = colorScheme.gray.light
	
	val isAltSizeFlag = ResettableFlag()
	val activeSizePointer = isAltSizeFlag.map { if (_) altBlockSize else blockSize }
	
	val canvas: ReachCanvas = ReachCanvas(cursors) { canvasHierarchy =>
		implicit val c: ReachCanvas = canvasHierarchy.top
		Framing(canvasHierarchy).withContext(baseContext).build(ScrollArea)
			.apply(margins.large.any, Vector(BackgroundDrawer(bg))) { scrollF =>
				scrollF.mapContext { _.inContextWithBackground(bg) }.build(Stack)
					.apply(maxOptimalSize = Some(Size(320, 320))) { rowF =>
						rowF.build(Stack).row() { colF =>
							val blocks = Vector.fill(5) {
								val content = Open { hierarchy =>
									Vector.fill(5) {
										CustomDrawReachComponent(hierarchy,
											Vector(BackgroundDrawer(colorIterator.next()))) { activeSizePointer.value }
									}
								}
								colF.column(content)
							}
							activeSizePointer.addContinuousAnyChangeListener {
								blocks.foreach { _.child.foreach { _.resetCachedSize() } }
								blocks.foreach { c => c.parent.revalidateAndThen { c.parent.repaintParent() } }
							}
							blocks
						}
					}
			}
	}
	
	GlobalKeyboardEventHandler.registerKeyStateListener { KeyStateListener.onKeyPressed(KeyEvent.VK_SPACE) { _ => isAltSizeFlag.update { !_ } } }
	GlobalKeyboardEventHandler.registerKeyStateListener { KeyStateListener.onKeyPressed(KeyEvent.VK_F5) { _ => canvas.repaint() } }
	
	val frame = Frame.windowed(canvas, "Reach Test")
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}

package utopia.reach.test

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.Hsl
import utopia.paradigm.shape.shape2d.Size
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas2
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.ScrollArea
import utopia.reach.window.ReachWindow

import java.awt.event.KeyEvent

/**
  * A test app for scroll areas
  * @author Mikko Hilpinen
  * @since 9.12.2020, v0.1
  */
object ReachScrollAreaTest extends App
{
	import ReachTestContext._
	
	AwtEventThread.debugMode = true
	
	// Settings and data
	val blockSize = StackSize.any(Size(96, 96))
	val altBlockSize = blockSize * 0.75
	val colorIterator = Iterator.continually { Hsl(Angle.ofDegrees(math.random() * 360), 1, 0.5) }
	val bg = colors.gray.light
	
	val isAltSizeFlag = ResettableFlag()
	val activeSizePointer = isAltSizeFlag.map { if (_) altBlockSize else blockSize }
	
	// Creates the components
	val window = ReachWindow.popupContextual.withWindowBackground(bg).using(Framing) { (canvas, framingF) =>
		implicit val c: ReachCanvas2 = canvas
		// Framing
		val framing = framingF.build(ScrollArea).apply(margins.aroundMedium) { scrollF =>
			// Scroll area
			scrollF.build(Stack).apply(maxOptimalSize = Some(Size(320, 320))) { rowF =>
				// Stack (X)
				rowF.build(Stack).row() { colF =>
					// Each row contains 5 columns, which each contain 5 blocks,
					// resulting in a 5x5 matrix
					val blocks = Vector.fill(5) {
						// Each block is created in open form
						val content = Open { hierarchy =>
							Vector.fill(5) {
								CustomDrawReachComponent(hierarchy,
									Vector(BackgroundDrawer(colorIterator.next()))) { activeSizePointer.value }
							}
						}
						colF.column(content)
					}
					// Implements revalidation for the blocks
					activeSizePointer.addContinuousAnyChangeListener {
						blocks.foreach { _.child.foreach { _.resetCachedSize() } }
						blocks.foreach { c => c.parent.revalidateAndThen { c.parent.repaintParent() } }
					}
					blocks
				}
			}
		}
		framing.withResult(framing.child)
	}
	
	// Displays the window
	window.display(centerOnParent = true)
	start()
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	
	// Adds user-interaction
	GlobalKeyboardEventHandler.registerKeyStateListener {
		KeyStateListener.onKeyPressed(KeyEvent.VK_SPACE) { _ => isAltSizeFlag.update { !_ } } }
	GlobalKeyboardEventHandler.registerKeyStateListener(
		KeyStateListener.onKeyPressed(KeyEvent.VK_F5) { _ => window.result.repaint() })
	
	Loop.regularly(3.seconds) {
		println()
		println(AwtEventThread.debugString)
	}
	
	println("Test is now running")
}

package utopia.reach.test

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.model.stack.StackSize
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.genesis.handling.event.keyboard.Key.{FunctionKey, Space}
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.Hsl
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.ScrollArea
import utopia.reach.window.ReachWindow

/**
  * A test app for scroll areas
  * @author Mikko Hilpinen
  * @since 9.12.2020, v0.1
  */
object ReachScrollAreaTest extends App
{
	import ReachTestContext._
	
	// Settings and data
	val blockSize = StackSize.any(Size(96, 96))
	val altBlockSize = blockSize * 0.75
	val colorIterator = Iterator.continually { Hsl(Angle.degrees(math.random() * 360)) }
	val bg = colors.gray.light
	
	val isAltSizeFlag = ResettableFlag()
	val activeSizePointer = isAltSizeFlag.map { if (_) altBlockSize else blockSize }
	
	// Creates the components
	val window = ReachWindow.contentContextual.withWindowBackground(bg).using(Framing) { (canvas, framingF) =>
		implicit val c: ReachCanvas = canvas
		// Framing
		val framing = framingF.build(ScrollArea) { scrollF =>
			// Scroll area
			scrollF.withMaxOptimalSize(Size.square(320)).build(Stack) { rowF =>
				// Stack (X)
				rowF.row.build(Stack) { colF =>
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
	KeyboardEvents += KeyStateListener.pressed(Space) { _ => isAltSizeFlag.update { !_ } }
	KeyboardEvents += KeyStateListener.pressed(FunctionKey(5)) { _ => window.result.repaint() }
	
	Loop.regularly(3.seconds) {
		println()
		println(AwtEventThread.debugString)
	}
	
	println("Test is now running")
}

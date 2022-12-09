package utopia.reach.test

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
	val colorIterator = Iterator.continually { Hsl(Angle.ofDegrees(math.random() * 360), 1, 0.5) }
	val bg = colorScheme.gray.light
	
	val canvas: ReachCanvas = ReachCanvas(cursors) { canvasHierarchy =>
		implicit val c: ReachCanvas = canvasHierarchy.top
		Framing(canvasHierarchy).withContext(baseContext).build(ScrollArea)
			.apply(margins.large.any, Vector(BackgroundDrawer(bg))) { scrollF =>
				scrollF.mapContext { _.inContextWithBackground(bg) }.build(Stack)
					.apply(maxOptimalSize = Some(Size(320, 320))) { rowF =>
						rowF.build(Stack).row() { colF =>
							Vector.fill(5) {
								val content = Open { hierarchy =>
									Vector.fill(5) {
										CustomDrawReachComponent(hierarchy,
											Vector(BackgroundDrawer(colorIterator.next())))(blockSize)
									}
								}
								colF.column(content)
							}
						}
					}
			}
	}
	
	val frame = Frame.windowed(canvas, "Reach Test")
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}

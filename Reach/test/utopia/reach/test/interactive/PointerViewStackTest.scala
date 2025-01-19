package utopia.reach.test.interactive

import utopia.flow.collection.immutable.Single
import utopia.flow.view.mutable.Pointer
import utopia.genesis.handling.event.keyboard.Key.ArrowKey
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.multi.ViewStack
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Tests a pointer-based view stack
  * @author Mikko Hilpinen
  * @since 13.01.2025, v1.5
  */
object PointerViewStackTest extends App
{
	// Sets up the managed pointer
	private val p = Pointer.eventful[Seq[Int]](Vector(1, 2, 3))
	
	// Sets up the GUI
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(ViewStack) { stackF =>
			stackF.centered.mapPointer(p, ViewTextLabel) { (labelF, p) =>
				println("Constructing a new label")
				labelF.mapContext { _.larger }(p)
			}
		}
	}
	
	// Sets up keyboard events (arrow keys)
	KeyboardEvents += KeyStateListener.pressed(ArrowKey.values) { e =>
		import utopia.paradigm.enumeration.Direction2D._
		e.arrow.foreach {
			case Up => p.update { _.map { _ * 2 } }
			case Down => p.update { _.map { _ / 2 } }
			case Left => p.update { _.drop(1) }
			case Right => p.update { v =>
				if (v.isEmpty)
					Single(1)
				else
					v :+ (v.last + 1)
			}
		}
	}
	
	// Displays the app
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
	
	/*
	Delay(2.seconds) { AwtEventThread.async {
		println(s"${ window.component.getInsets }")
		println(window.bounds)
		println(window.canvas.bounds)
		println(window.component.getSize)
		println(window.canvas.component.getSize)
		println(window.component.getWidth)
		println(window.component.getHeight)
		
		println(Toolkit.getDefaultToolkit.getScreenInsets(window.component.getGraphicsConfiguration))
	} }*/
	/*
	window.positionPointer.addListener { e => println(s"P = ${ e.newValue }") }
	KeyboardEvents += KeyStateListener.pressed.arrow(Down) { _ =>
		println(s"\n${ window.component.getLocation } / ${ window.position }")
		val newPos = window.position + Vector2D(20, -37)
		println(s"Setting position to $newPos")
		AwtEventThread.later { window.component.setLocation(newPos.toAwtPoint) }
		Delay(0.5.seconds) {
			println(s"${ window.component.getLocation } / ${ window.position }")
		}
	}*/
}

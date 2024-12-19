package utopia.reach.test.interactive

import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.{Bottom, BottomLeft, Top, TopRight}
import utopia.paradigm.enumeration.Axis.Y
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.layered.LayerPositioning.AlignedToSide
import utopia.reach.container.layered.Layers
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

/**
  * Tests the Layers view.
  *
  * When you run this test, you should see the following:
  * - A resizable window with gray background
  * - A red label spanning the top of the window, reading "Top"
  * - A blue label spanning the right side of the window, reading "Right"
  * - A green label at the bottom, reading "Bottom"
  * - A magenta label at the left, reading "Left"
  * - Two orange labels, one at the bottom left and one at the top right corner (each with text)
  *
  * @author Mikko Hilpinen
  * @since 08/01/2024, v1.2
  */
object LayersTest extends App
{
	import ReachTestContext._
	
	val window = ReachWindow.contentContextual.using(Layers, title = "Layers Test") { (_, layersF) =>
		layersF.build(EmptyLabel, TextLabel) { (mainF, labelF) =>
			val mainLabel = mainF((300.any x 300.any).lowPriority)
			val layers = Vector(
				labelF.withBackground(Color.red).withHorizontallyExpandingText("Top") -> AlignedToSide(Top),
				labelF.withBackground(Color.blue).withTextExpandingAlong(Y)("Right") -> AlignedToSide(Alignment.Right),
				labelF.withBackground(Color.green)("Bottom") -> AlignedToSide(Bottom, expandIfPossible = false),
				labelF.withBackground(Color.magenta)("Left") -> AlignedToSide(Alignment.Left, expandIfPossible = false),
				labelF.withBackground(Secondary)("TopRight") -> AlignedToSide(TopRight),
				labelF.withBackground(Secondary)("BottomLeft") -> AlignedToSide(BottomLeft, expandIfPossible = false)
			)
			ComponentCreationResult.layers(mainLabel, layers)
		}
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
}

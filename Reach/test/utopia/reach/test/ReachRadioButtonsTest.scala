package utopia.reach.test

import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.reach.component.input.selection.RadioButtonGroup
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.LocalizedString
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests radio button creation
  * @author Mikko Hilpinen
  * @since 20.3.2023, v0.5.1
  */
object ReachRadioButtonsTest extends App
{
	ParadigmDataType.setup()
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import TestCursors._
	import utopia.reflection.test.TestContext._
	
	val mainBg = colorScheme.gray.default
	
	val canvas = ReachCanvas(cursors) { hierarchy =>
		Framing(hierarchy).withContext(baseContext.inContextWithBackground(mainBg).forTextComponents)
			.build(RadioButtonGroup)
			.apply(margins.medium.any, customDrawers = Vector(BackgroundDrawer(mainBg))) { buttonsF =>
				buttonsF(Vector[(Int, LocalizedString)](1 -> "First", 2 -> "Second", 3 -> "Third Option"),
					customDrawers = Vector(BackgroundDrawer(Color.cyan)))
			}
	}
	
	val frame = Frame.windowed(canvas.parent, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}

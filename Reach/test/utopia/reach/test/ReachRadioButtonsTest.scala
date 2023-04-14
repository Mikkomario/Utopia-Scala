package utopia.reach.test

import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.reach.component.input.selection.RadioButtonGroup
import utopia.reach.container.ReachCanvas2
import utopia.reach.container.wrapper.Framing
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.localization.LocalizedString
import utopia.reflection.util.SingleFrameSetup
import utopia.firmament.model.stack.LengthExtensions._

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
	/*
	val canvas = ReachCanvas2(cursors) { hierarchy =>
		Framing(hierarchy).withContext(baseContext.against(mainBg).forTextComponents)
			.build(RadioButtonGroup)
			.apply(margins.medium.any, customDrawers = Vector(BackgroundDrawer(mainBg))) { buttonsF =>
				buttonsF(Vector[(Int, LocalizedString)](1 -> "First", 2 -> "Second", 3 -> "Third Option"),
					customDrawers = Vector(BackgroundDrawer(Color.cyan)))
			}
	}
	
	val frame = Frame.windowed(canvas.parent, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	 */
}

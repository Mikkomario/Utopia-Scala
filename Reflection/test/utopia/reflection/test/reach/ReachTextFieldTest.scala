package utopia.reflection.test.reach

import utopia.flow.event.Changing
import utopia.flow.generic.DataType
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.reach.factory.Mixed
import utopia.reflection.component.reach.input.TextField
import utopia.reflection.component.reach.label.ViewTextLabel
import utopia.reflection.container.reach.{Framing, Stack}
import utopia.reflection.container.stack.StackLayout.Trailing
import utopia.reflection.container.swing.ReachCanvas
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._

/**
  * A simple test for text fields
  * @author Mikko Hilpinen
  * @since 18.11.2020, v2
  */
object ReachTextFieldTest extends App
{
	DataType.setup()
	
	import TestContext._
	
	// Creates text fields (+ result views)
	val canvas = ReachCanvas(cursors) { hierarchy =>
		Framing(hierarchy).buildWithContext(Stack, baseContext).withBackground(colorScheme.gray, margins.medium.any.square) { stack =>
			stack.build(Stack).column() { r =>
				val rows = r.mapContext { _.forTextComponents }
				Vector(
					// TODO: WET WET
					rows.build(Mixed).row(layout = Trailing, areRelated = true) { row1 =>
						val field = row1(TextField).forString(320.any, Some(Changing.wrap("Text")),
							maxLength = Some(32), showCharacterCount = true)
						val summary = row1(ViewTextLabel)(field.valuePointer)
						Vector(field, summary)
					},
					rows.build(Mixed).row(layout = Trailing, customDrawers = Vector(BackgroundDrawer(Color.magenta)), areRelated = true) { row2 =>
						val field = row2(TextField).forInt(Some(Changing.wrap("Int")), fillBackground = false)
						val summary = row2(ViewTextLabel)(field.valuePointer, DisplayFunction.rawOption)
						Vector(field, summary)
					},
					rows.build(Mixed).row(layout = Trailing, areRelated = true) { row3 =>
						val field = row3(TextField).forInt(Some(Changing.wrap("Int+")),
							minValue = 0, maxValue = 10, fillBackground = false)
						val summary = row3(ViewTextLabel)(field.valuePointer, DisplayFunction.rawOption)
						Vector(field, summary)
					},
					rows.build(Mixed).row(layout = Trailing, areRelated = true) { row4 =>
						val field = row4(TextField).forDouble(0.0, 1.0,
							Some(Changing.wrap("Double")), Some(Changing.wrap("0.".noLanguageLocalizationSkipped)))
						val summary = row4(ViewTextLabel)(field.valuePointer, DisplayFunction.rawOption)
						Vector(field, summary)
					}
				).map { _.parent }
			}
		}
	}.parent
	
	val frame = Frame.windowed(canvas, "Reach Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}

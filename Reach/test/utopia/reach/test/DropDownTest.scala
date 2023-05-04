package utopia.reach.test

import utopia.firmament.context.ColorContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reach.component.input.selection.DropDown
import utopia.reach.component.label.text.TextLabel
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.context.ReachContentWindowContext.apply
import utopia.reach.window.ReachWindow

/**
  * A test application with drop down fields
  * @author Mikko Hilpinen
  * @since 23.12.2020, v0.1
  */
object DropDownTest extends App
{
	import ReachTestContext._
	
	val arrowImage = Image.readFrom("Reflection/test-images/arrow-back-48dp.png")
	arrowImage.logFailure
	val expandIcon = arrowImage.map { i => SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationCounterClockwise)) }
		.getOrElse(SingleColorIcon.empty)
	val shrinkIcon = arrowImage.map { i => SingleColorIcon(i.transformedWith(Matrix2D.quarterRotationClockwise)) }
		.getOrElse(SingleColorIcon.empty)
	
	val items = Map("Fruits" -> Vector("Apple", "Banana", "Kiwi"), "Minerals" -> Vector("Diamond", "Ruby", "Sapphire"))
	
	val window = ReachWindow.contextual.apply(title = "Drop-Down Test") { hierarchy =>
		implicit val canvas: ReachCanvas = hierarchy.top
		Framing(hierarchy).buildFilledWithContext(baseContext, colors.gray.light, Stack.static[ColorContext])
			.apply(margins.medium.any.square) { stackF =>
				stackF.mapContext { _.forTextComponents.borderless.nonResizable }.build(DropDown)
					.column(areRelated = true) { ddF =>
						val selectedCategoryPointer = new PointerWithEvents[Option[String]](None)
						val selectedItemPointer = new PointerWithEvents[Option[String]](None)
						
						selectedItemPointer.addListener { e => println(e) }
						
						Vector(
							ddF.simple(Fixed(items.keys.toVector.sorted), selectedCategoryPointer, expandIcon,
								shrinkIcon, fieldNamePointer = Fixed("Category"),
								promptPointer = Fixed("Select One")),
							ddF.simple(
								selectedCategoryPointer.map {
									case Some(category) => items(category)
									case None => Vector()
								},
								selectedItemPointer, expandIcon, shrinkIcon,
								fieldNamePointer = selectedCategoryPointer.map {
									case Some(category) => category
									case None => "Item"
								},
								hintPointer = selectedCategoryPointer.map {
									case Some(_) => LocalizedString.empty
									case None => "Select category first"
								},
								promptPointer = Fixed("Select One"),
								makeNoOptionsView = Some({ (hierarchy, context, _) =>
									TextLabel(hierarchy).withContext(context)
										.apply("Please select a category first", isHint = true)
								})
							)
						)
					}
			}
	}
	
	// AwtEventThread.debugMode = true
	// Loop.regularly(2.seconds) { println(AwtEventThread.debugString) }
	
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}

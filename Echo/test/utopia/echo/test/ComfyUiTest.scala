package utopia.echo.test

import utopia.echo.controller.client.ComfyUIClient
import utopia.echo.controller.sd.GenerateImages
import utopia.echo.model.comfyui.Seed.RandomSeed
import utopia.echo.model.comfyui.settings.SamplerSettings
import utopia.echo.model.comfyui.{CheckpointModel, ComfyUiDir, Seed}
import utopia.echo.test.EchoTestContext._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.console.ConsoleExtensions._

import java.nio.file.Paths
import scala.io.StdIn

/**
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
object ComfyUiTest extends App
{
	// APP CODE --------------------
	
	// Requests the required information
	StdIn.readNonEmptyLine("Specify where ComfyUI directory is located").map { Paths.get(_) }.filter { _.exists } match {
		case Some(comfyUiDir) =>
			implicit val dir: ComfyUiDir = comfyUiDir
			val modelPaths = CheckpointModel.list.get
			StdIn.selectFrom(modelPaths.map { p => p -> p.name }).foreach { implicit model =>
				StdIn.readNonEmptyLine("Write the positive prompt").foreach { prompt =>
					println("Write the negative prompt, if applicable")
					generate(prompt, StdIn.readLine())
				}
			}
		case None => println("Not a valid directory")
	}
	
	
	// OTHER    ------------------
	
	private def generate(positive: String, negative: String)
	                    (implicit comfyDir: ComfyUiDir, model: CheckpointModel): Unit =
	{
		implicit val seed: Seed = RandomSeed
		implicit val samplerSettings: SamplerSettings = SamplerSettings.default
		implicit val client: ComfyUIClient = new ComfyUIClient()
		
		// Request the work
		println("Requesting the work...")
		val imagePaths = GenerateImages.apply(positive, negative).waitForResult().get
		println("Waiting for the generation to finish...")
		
		if (imagePaths.isEmpty)
			println("No images generated")
		else if (imagePaths.hasSize > 1)
			imagePaths.head.openDirectory()
		else
			imagePaths.head.openInDesktop()
	}
}

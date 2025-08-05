package utopia.echo.test

import utopia.echo.controller.client.ComfyUIClient
import utopia.echo.model.request.comfyui.Seed.RandomSeed
import utopia.echo.model.request.comfyui.workflow.SamplerSettings
import utopia.echo.model.request.comfyui.workflow.node._
import utopia.echo.model.request.comfyui.{ComfyUiDir, GetWorkResult, Seed}
import utopia.echo.test.EchoTestContext._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.Today
import utopia.flow.util.StringExtensions._
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
			val modelPaths = (comfyUiDir/"models/checkpoints")
				.iterateChildren { _.filter { _.fileType.nonEmpty }.map { _.fileName }.toOptimizedSeq.sorted }.get
			StdIn.selectFrom(modelPaths.map { p => p -> p.untilLast(".") }).foreach { model =>
				StdIn.readNonEmptyLine("Write the positive prompt").foreach { prompt =>
					println("Write the negative prompt, if applicable")
					generate(model, prompt, StdIn.readLine())
				}
			}
		case None => println("Not a valid directory")
	}
	
	
	// OTHER    ------------------
	
	private def generate(modelName: String, positive: String, negative: String)
	                    (implicit comfyDir: ComfyUiDir): Unit =
	{
		implicit val seed: Seed = RandomSeed
		implicit val samplerSettings: SamplerSettings = SamplerSettings.default
		
		// Creates the workflow
		val model = SimpleCheckpointLoaderNode("model", modelName)
		val posPrompt = EncodeTextPromptNode("positive_prompt", positive, model.clip)
		val negPrompt = EncodeTextPromptNode("negative_prompt", negative, model.clip)
		val latent = EmptyLatentImageNode("latent")
		val sampler = KSamplerNode("sampler", model.model, posPrompt, negPrompt, latent)
		val decode = DecodeImageNode("decode", sampler.latentOutput, model.vae)
		val save = SaveImageNode("save", decode, s"$Today-Utopia-Echo-test")
		
		// Request the work
		val client = new ComfyUIClient()
		println("Requesting the work...")
		val imagePaths = client(Vector(model, posPrompt, negPrompt, latent, sampler, decode, save),
			GetWorkResult.imagesExtractor(save.name)).waitForResult().get
		
		if (imagePaths.isEmpty)
			println("No images generated")
		else if (imagePaths.hasSize > 1)
			imagePaths.head.openDirectory()
		else
			imagePaths.head.openInDesktop()
	}
}

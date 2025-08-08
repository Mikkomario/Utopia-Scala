package utopia.echo.controller.sd

import utopia.echo.controller.client.ComfyUIClient
import utopia.echo.model.comfyui.request.GetWorkResult
import utopia.echo.model.comfyui.settings.SamplerSettings
import utopia.echo.model.comfyui.workflow.node._
import utopia.echo.model.comfyui.workflow.OutputRef
import utopia.echo.model.comfyui.{CheckpointModel, ComfyUiDir, Seed}
import utopia.flow.collection.immutable.Pair

import scala.annotation.unused
import scala.language.implicitConversions

/**
 * An interface for generating images using stable diffusion and ComfyUI.
 *
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
object GenerateImages
{
	// ATTRIBUTES   ---------------------
	
	private val _pos = "prompt_pos"
	private val _neg = "prompt_neg"
	
	
	// IMPLICIT -------------------------
	
	// Implicitly converts into a generator
	implicit def objectToFactory(@unused o: GenerateImages.type)
	                            (implicit comfyDir: ComfyUiDir, client: ComfyUIClient, model: CheckpointModel,
	                             seed: Seed = Seed.random, settings: SamplerSettings = SamplerSettings.default): ImageGenerator =
		generator()
	
	
	// OTHER    -------------------------
	
	/**
	 * Prepares an image generator
	 * @param size Size of the generated image. Both width and height should be factors of 64.
	 *             Default = 512 x 512.
	 * @param batchSize Number of images to generate (default = 1)
	 * @param fileNamePrefix Prefix to assign for the generated image file names. Default = "Echo".
	 * @param comfyDir Implicit ComfyUI directory location
	 * @param model Implicit checkpoint model to use
	 * @param seed Implicit seed to use (default = always random)
	 * @param settings Implicit sampler settings to apply
	 * @return A future that yields the results once they're acquired.
	 *         May take a while to complete, and may yield a failure.
	 */
	def generator(size: Pair[Int] = EmptyLatentImageNode.defaultSize, batchSize: Int = 1,
	              fileNamePrefix: String = "Echo")
	             (implicit comfyDir: ComfyUiDir, client: ComfyUIClient, model: CheckpointModel,
	              seed: Seed = Seed.random, settings: SamplerSettings = SamplerSettings.default) =
		new ImageGenerator(size, batchSize, fileNamePrefix)
	
	
	// NESTED   -------------------------
	
	class ImageGenerator(size: Pair[Int], batchSize: Int, fileNamePrefix: String)
	                    (implicit comfyDir: ComfyUiDir, client: ComfyUIClient, model: CheckpointModel, seed: Seed,
	                     settings: SamplerSettings)
	{
		// ATTRIBUTES   -----------------
		
		private lazy val loadModel = SimpleCheckpointLoaderNode("model")
		private lazy val latent = EmptyLatentImageNode("latent", size, batchSize)
		private lazy val sampler = KSamplerNode("sampler", loadModel.model, OutputRef(_pos), OutputRef(_neg), latent)
		private lazy val decode = DecodeImageNode("decode", sampler.latentOutput, loadModel.vae)
		private lazy val save = SaveImageNode("save", decode, fileNamePrefix)
		
		private lazy val parser = GetWorkResult.imagesExtractor(save.name)
		
		
		// OTHER    ---------------------
		
		def apply(positivePrompt: String, negativePrompt: String = "") = client(Vector(
			loadModel, EncodeTextPromptNode(_pos, positivePrompt, loadModel.clip),
			EncodeTextPromptNode(_neg, negativePrompt, loadModel.clip), latent, sampler, decode, save),
			parser)
	}
}

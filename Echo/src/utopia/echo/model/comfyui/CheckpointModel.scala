package utopia.echo.model.comfyui

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._

import scala.language.implicitConversions

object CheckpointModel
{
	// COMPUTED -------------------------
	
	/**
	 * Lists the locally available checkpoint models
	 * @param comfyDir Implicit ComfyUI directory
	 * @return List of locally available models. Failure if file-accessing failed.
	 */
	def list(implicit comfyDir: ComfyUiDir) =
		(comfyDir/"models/checkpoints").iterateChildren {
			_.filter { _.fileType.nonEmpty }.map { p => apply(p.fileName) }.toOptimizedSeq.sortBy { _.fileName } }
	
	
	// IMPLICIT -------------------------
	
	/**
	 * @param fileName Name of this model's file in the ComfyUI checkpoints directory
	 * @return A checkpoint model reference
	 */
	implicit def apply(fileName: String): CheckpointModel = _CheckPointModel(fileName)
	
	
	// NESTED   -------------------------
	
	private case class _CheckPointModel(fileName: String) extends CheckpointModel
	{
		override lazy val name: String = fileName.untilLast(".")
	}
}

/**
 * Used for selecting the checkpoint model to use in stable diffusion
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
trait CheckpointModel
{
	/**
	 * @return Name of this model
	 */
	def name: String
	/**
	 * @return Name of this model's file in the ComfyUI checkpoints directory
	 */
	def fileName: String
}

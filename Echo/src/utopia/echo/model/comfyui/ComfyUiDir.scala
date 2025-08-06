package utopia.echo.model.comfyui

import utopia.flow.parse.file.FileExtensions.RichPath

import java.nio.file.Path
import scala.language.implicitConversions

object ComfyUiDir
{
	implicit def fromPath(path: Path): ComfyUiDir = apply(path)
	implicit def toPath(dir: ComfyUiDir): Path = dir.path
	implicit def toRichPath(dir: ComfyUiDir): RichPath = dir.path
}

/**
 * Used for determining where the ComfyUI directory resides on the disk
 *
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class ComfyUiDir(path: Path)

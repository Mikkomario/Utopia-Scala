package utopia.echo.model.response.llm

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.{Changing, Flag}

object StreamedDownloadStatus
{
	// OTHER    ---------------------------
	
	/**
	  * @param digest SHA digest of the file that was downloaded
	  * @param size Size of the downloaded file in bytes
	  * @return A completed download status
	  */
	def completed(digest: String, size: Long) = apply(digest, size, Fixed(size))
}

/**
  * Used for representing the download status of a single file / digest in real time.
  * @param digest SHA digest of the file being downloaded
  * @param totalSize The total size of this download, in bytes
  * @param downloadedPointer A pointer containing the amount of data downloaded so far in bytes
  * @author Mikko Hilpinen
  * @since 03.09.2024, v1.1
  */
case class StreamedDownloadStatus(digest: String, totalSize: Long, downloadedPointer: Changing[Long])
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A pointer that contains the current download progress [0,1]
	  */
	lazy val progressPointer = downloadedPointer.map { d => d.toDouble / totalSize }
	/**
	  * A pointer that contains true once this download has completed
	  */
	lazy val completionFlag: Flag = downloadedPointer.map { _ >= totalSize }
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return The amount of bytes downloaded so far
	  */
	def downloaded = downloadedPointer.value
	
	/**
	  * @return The current download progress [0,1]
	  */
	def progress = progressPointer.value
	/**
	  * @return Whether this download has completed
	  */
	def completed = completionFlag.value
	/**
	  * @return Whether this download has not yet completed
	  */
	def incomplete = !completed
}

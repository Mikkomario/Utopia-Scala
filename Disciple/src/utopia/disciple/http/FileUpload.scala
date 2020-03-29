package utopia.disciple.http

import java.io.File
import utopia.access.http.ContentType
import java.nio.charset.Charset

object FileUpload
{
    /**
     * Wraps a file into a file upload
     * @param file the target file
     * @param contentType the type of the file's contents. Optional, guessed if left empty.
     * @param charset The character set of the file (optional)
     */
    def from(file: File, contentType: Option[ContentType] = None, charset: Option[Charset]) = 
    {
        if (file.exists())
            contentType.orElse(ContentType.guessFrom(file.getName)).map(FileUpload(file, _, charset))
        else
            None
    }
}

/**
* FileUpload is a simple struct for the data necessary for a file upload
* @author Mikko Hilpinen
* @since 19.2.2018
**/
case class FileUpload(val file: File, val contentType: ContentType, val charset: Option[Charset] = None)
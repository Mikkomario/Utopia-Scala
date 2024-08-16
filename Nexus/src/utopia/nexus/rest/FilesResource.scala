package utopia.nexus.rest

import utopia.access.http.Method._
import utopia.access.http.Status._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.nexus.http.{Path, Response, StreamedBody}
import utopia.nexus.rest.ResourceSearchResult.Ready

import java.io.File
import java.nio.file.Files
import scala.util.{Failure, Success, Try}

/**
 * This resource is used for uploading and retrieving file data.<br>
 * GET retrieves a file / describes a directory (model)<br>
 * POST targets a directory and uploads the file(s) to that directory. Returns CREATED along with 
 * a set of links to uploaded files.<br>
 * DELETE targets a file or a directory and deletes that file + all files under it. Returns 
 * OK if deletion was successful and Internal Server Error (500) if it was not. Returns Not Found (404) 
 * if no valid file or directory was targeted
 * @author Mikko Hilpinen
 * @since 17.9.2017
 */
class FilesResource(override val name: String, uploadPath: java.nio.file.Path) extends Resource[Context]
{
    // TODO: Convert to use result instead of response
    
    // IMPLEMENTED METHODS & PROPERTIES    ---------------------
    
    override def allowedMethods = Vector(Get, Post, Delete)
    
    override def follow(path: Path)(implicit context: Context) = Ready(this, Some(path))
         
    override def toResponse(remainingPath: Option[Path])(implicit context: Context) = 
    {
        context.request.method match 
        {
            case Get => handleGet(remainingPath)
            case Post => handlePost(remainingPath)
            case Delete => handleDelete(remainingPath)
            case _ => Response.empty(MethodNotAllowed)
        }
    }
    
    
    // OTHER METHODS    ---------------------------------------
    
    private def handleGet(remainingPath: Option[Path])(implicit context: Context) = 
    {
        val targetFilePath = targetFilePathFrom(remainingPath)
        
        if (Files.isDirectory(targetFilePath))
            Response.fromModel(makeDirectoryModel(targetFilePath.toFile, context.request.targetUrl))
        else if (Files.isRegularFile(targetFilePath))
            Response.fromFile(targetFilePath)
        else
            Response.empty(NotFound)
    }
    
    private def handlePost(remainingPath: Option[Path])(implicit context: Context) = 
    {
        val request = context.request
        
        if (request.body.isEmpty)
            Response.plainText("No files were provided", BadRequest, 
                    request.headers.preferredCharsetOrUTF8)
        else
        {
            val counter = Iterator.iterate(1) { _ + 1 }
            val nameFromParam = request.parameters("filename").string.orElse(request.parameters("name").string)
            val partNames = request.body.map { p =>
                p.name.getOrElse {
                    s"upload_${Now.toLocalDateTime}${ if (request.body.size > 1) s"_${ counter.next() }" else "" }"
                }
            }
            
            val uploadResults = request.body.zip(partNames).map { case (b, partName) => upload(b, partName, remainingPath) }
            val successes = partNames.zip(uploadResults).filter(_._2.isSuccess).toMap.view.mapValues(_.get)
            
            if (successes.isEmpty) {
                // TODO: For some reason, the error message only tells the directory which couldn't be created
                val errorMessage = Option(uploadResults.head.failed.get.getMessage)
                errorMessage.map(Response.plainText(_, Forbidden)).getOrElse(Response.empty(Forbidden))
            }
            else {
                // TODO: Add better handling for cases where request path is empty for some reason
                val myPath = myLocationFrom(request.path.getOrElse(Path(name)), remainingPath)
                val resultUrls = successes.mapValues(p => (myPath/p).toServerUrl(context.settings))
                
                val location = if (resultUrls.size == 1) resultUrls.head._2 else myPath.toServerUrl(context.settings)
                
                Response.fromModel(Model.fromMap(resultUrls.toMap)).mapHeaders(_.withLocation(location))
            }
        }
    }
    
    private def handleDelete(remainingPath: Option[Path])(implicit context: Context) = 
    {
        if (remainingPath.isEmpty)
            Response.plainText("May not delete the root upload folder", Forbidden)
        else
            Response.empty(delete(remainingPath.get))
    }
    
    /**
     * @param directory the directory whose data is returned
     * @param directoryAddress the request url targeting the directory
     */
    private def makeDirectoryModel(directory: File, directoryAddress: String) = 
    {
        val allFiles = directory.listFiles().toSeq.groupBy { _.isDirectory() }
        val files = allFiles.getOrElse(false, Empty).map { f => s"$directoryAddress/${f.getName}" }
        val directories = allFiles.getOrElse(true, Empty).map { f => s"$directoryAddress/${f.getName}" }
        
        Model(Pair("files" -> files.toVector, "directories" -> directories.toVector))
    }
    
    private def upload(part: StreamedBody, partName: String, remainingPath: Option[Path]) = {
        val makeDirectoryResult = remainingPath.map(_.toString()).map(
                uploadPath.resolve).map(p => Try(Files.createDirectories(p))).getOrElse(Success(uploadPath))
        
        if (makeDirectoryResult.isSuccess) {
            // Generates the proper file name
            val fileName = if (partName.contains(".")) partName else s"$partName.${ part.contentType.subType }"
            val filePath = makeDirectoryResult.get.resolve(fileName)
            
            // Writes the file, returns the server path for the targeted resource
            part.writeTo(filePath).map(_ => remainingPath.map(_/fileName) getOrElse Path(fileName))
        }
        else
            Failure(makeDirectoryResult.failed.get)
    }
    
    /*
    private def upload(fileUpload: FileUpload, remainingPath: Option[Path])(implicit settings: ServerSettings) = 
    {
        val makeDirectoryResult = remainingPath.map { remaining => 
                Try(Files.createDirectories(settings.uploadPath.resolve(remaining.toString()))) }
        
        if (makeDirectoryResult.isEmpty || makeDirectoryResult.get.isSuccess)
        {
            val fileName = remainingPath.map { _ / fileUpload.submittedFileName }.getOrElse(
                    Path(fileUpload.submittedFileName));
            fileUpload.write(fileName)
        }
        else
        {
            Failure(makeDirectoryResult.get.failed.get)
        }
    }*/
    
    private def delete(remainingPath: Path)(implicit context: Context) = 
    {
        val targetFilePath = targetFilePathFrom(Some(remainingPath))
        if (Files.exists(targetFilePath))
        {
            if (recursiveDelete(targetFilePath.toFile)) OK else InternalServerError
        }
        else
        {
            NotFound
        }
    }
    
    private def targetFilePathFrom(remainingPath: Option[Path])(implicit context: Context) = 
            remainingPath.map { remaining => uploadPath.resolve(remaining.toString) }.getOrElse(uploadPath)
    
    private def myLocationFrom(targetPath: Path, remainingPath: Option[Path]) = 
            remainingPath.flatMap(targetPath.before).getOrElse(targetPath)
    
    // private def parseLocation(targetPath: Path, remainingPath: Option[Path], generatedPath: Path) =
    //        myLocationFrom(targetPath, remainingPath)/generatedPath
    
    private def recursiveDelete(file: File): Boolean = 
    {
        if (file.isDirectory)
        {
            // If a directory is targeted, removes all files from the said directory
            file.listFiles().foreach(recursiveDelete)
        }
        // removes the file itself as well
        file.delete()
    }
}
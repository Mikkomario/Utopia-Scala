package utopia.nexus.rest

import utopia.nexus.http.ServerSettings
import utopia.nexus.result.ResultParser
import utopia.nexus.result.UseRawJson
import utopia.nexus.http.Request

/**
 * A base context is a very simple context that only contains server settings
* @author Mikko Hilpinen
* @since 22.5.2018
**/
class BaseContext(override val request: Request, override val resultParser: ResultParser = UseRawJson)
        (implicit override val settings: ServerSettings) extends Context
{
    // Doesn't need to close anything
    def close() = ()
}
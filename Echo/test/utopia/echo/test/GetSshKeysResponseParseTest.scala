package utopia.echo.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.echo.model.request.vastai.GetSshKeys.GetSshKeysResponseParser

/**
 *
 * @author Mikko Hilpinen
 * @since 02.03.2026, v
 */
object GetSshKeysResponseParseTest extends App
{
	private val json = "{\n  \"success\": true,\n  \"ssh_keys\": \"[{\\\"id\\\": 1, \\\"name\\\": \\\"my-key\\\", \\\"public_key\\\": \\\"ssh-rsa AAAA...\\\"}]\"\n}"
	
	println(GetSshKeysResponseParser(JsonBunny(json).get.tryModel.get).get)
}

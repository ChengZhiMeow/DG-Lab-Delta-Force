package cn.chengzhiya.dglabdeltaforce.client

import cn.chengzhiya.dglabdeltaforce.Main
import cn.chengzhiya.dglabdeltaforce.capture.BufferedImageAddon.subImage
import cn.chengzhiya.mhdfhttpframework.client.HttpClient
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class OcrClient {
    val httpClient = HttpClient()

    fun ocrImage(
        image: BufferedImage,
        zone: Main.ConfigData.Zone?
    ): String? {
        val out = ByteArrayOutputStream()
        ImageIO.write(if (zone != null) image.subImage(zone) else image, "png", out)

        httpClient.headerHashMap["Content-Type"] = "application/octet-stream"
        return httpClient.post(Main.configData.ocr.url, out.toByteArray())
    }
}
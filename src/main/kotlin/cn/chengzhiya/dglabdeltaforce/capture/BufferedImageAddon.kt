package cn.chengzhiya.dglabdeltaforce.capture

import cn.chengzhiya.dglabdeltaforce.Main
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

object BufferedImageAddon {
    fun BufferedImage.save(file: File) {
        val formatName = file.extension.ifBlank { "png" }
        ImageIO.write(this, formatName, file)
    }

    fun BufferedImage.subImage(zone: Main.ConfigData.Zone): BufferedImage {
        var x1 = zone.lt.x
        var y1 = zone.lt.y
        var x2 = zone.rb.x
        var y2 = zone.rb.y
        if (zone.right) {
            x1 = this.width - x1
            x2 = this.width - x2
        }
        if (!zone.top) {
            y1 = this.height - y1
            y2 = this.height - y2
        }

        val minX = min(x1, x2)
        val maxX = max(x1, x2)
        val minY = min(y1, y2)
        val maxY = max(y1, y2)

        return this.getSubimage(
            minX, minY,
            maxX - minX,
            maxY - minY
        )
    }
}
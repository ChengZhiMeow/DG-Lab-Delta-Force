package cn.chengzhiya.dglabdeltaforce

import cn.chengzhiya.dglabdeltaforce.Main.ConfigData.*
import cn.chengzhiya.dglabdeltaforce.capture.BufferedImageAddon.subImage
import cn.chengzhiya.dglabdeltaforce.capture.CaptureUtil
import cn.chengzhiya.dglabdeltaforce.client.GameClient
import cn.chengzhiya.dglabdeltaforce.client.OcrClient
import cn.chengzhiya.dglabdeltaforce.file.FileManager
import cn.chengzhiya.dglabdeltaforce.jna.JnaWindowUtil
import cn.chengzhiya.dglabdeltaforce.jna.ext.Capture
import cn.chengzhiya.dglabdeltaforce.yaml.YamlConfiguration
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.absoluteValue

object Main {
    val configFile = File("config.yml")
    val captureDll = File("CaptureLibrary.dll")

    val configData = ConfigData()

    val gameClient = GameClient()
    val ocrClient = OcrClient()

    fun reloadConfig() {
        val config = YamlConfiguration.loadConfiguration(configFile)

        run {
            configData.capture.delay = config.getInt("capture.delay")!!.toLong()
        }

        run {
            configData.process.name = config.getString("process.name")!!
        }

        run {
            configData.image.sharpening.enable = config.getBoolean("image.sharpening.enable")

            var width = 0
            var height = 0
            val kernelData = mutableListOf<Float>()
            config.getList("image.sharpening.kernel")
                .apply { height = this.size }
                .map { it as List<Double> }
                .forEach {
                    width = it.size
                    it.forEach { d -> kernelData.add(d.toFloat()) }
                }
            configData.image.sharpening.kernel = Kernel(width, height, kernelData.toFloatArray())
        }

        run {
            configData.strength.min = config.getInt("strength.min")!!
            configData.strength.max = config.getInt("strength.max")!!
            configData.strength.random = config.getInt("strength.random")!!
            configData.strength.multiplier = config.getDouble("strength.multiplier")!!
        }

        run {
            configData.server.url = config.getString("server.url")!!
            configData.server.clientId = config.getString("server.client_id")!!
            configData.server.api.strength = config.getString("server.api.strength")!!
                .replace("{client}", configData.server.clientId)
        }

        run {
            configData.ocr.url = config.getString("ocr.url")!!
        }

        run {
            configData.colors.clear()

            val colors = config.getConfigurationSection("colors")
            colors.getKeys().forEach {
                val list = mutableListOf<Color>()

                colors.getConfigurationSection(it!!).getKeys().forEach { c ->
                    val color = colors.getConfigurationSection("$it.$c")

                    list.add(
                        Color(
                            java.awt.Color(
                                color.getInt("color.r")!!,
                                color.getInt("color.g")!!,
                                color.getInt("color.b")!!
                            ),
                            color.getInt("tolerance")!!
                        )
                    )
                }

                configData.colors[it] = list
            }
        }

        run {
            configData.zones.clear()

            val zones = config.getConfigurationSection("zones")
            zones.getKeys().forEach {
                val map = mutableMapOf<String?, Zone>()

                zones.getConfigurationSection(it!!).getKeys().forEach { z ->
                    val zone = zones.getConfigurationSection("$it.$z")

                    map[z] = Zone(
                        zone.getBoolean("right"),
                        zone.getBoolean("top"),
                        Zone.Pos(
                            zone.getInt("lt.x")!!,
                            zone.getInt("lt.y")!!
                        ),
                        Zone.Pos(
                            zone.getInt("rb.x")!!,
                            zone.getInt("rb.y")!!
                        )
                    )
                }

                configData.zones[it] = map
            }
        }

        run {
            configData.game.health.type = Game.Health.Type.valueOf(config.getString("game.health.type")!!)
            configData.game.health.tolerance = config.getInt("game.health.tolerance")!!

            configData.game.start.type = Game.GameCheckType.valueOf(config.getString("game.start.type")!!)

            configData.game.end.type = Game.GameCheckType.valueOf(config.getString("game.end.type")!!)
            configData.game.end.reset = config.getBoolean("game.end.reset")
            configData.game.end.vl = config.getInt("game.end.vl")!!
        }
    }

    data class ConfigData(
        val capture: Capture = Capture(),
        val process: Process = Process(),
        val image: Image = Image(),
        val strength: Strength = Strength(),
        val server: Server = Server(),
        val ocr: Ocr = Ocr(),
        val colors: MutableMap<String?, List<Color>> = mutableMapOf(),
        val zones: MutableMap<String?, Map<String?, Zone>> = mutableMapOf(),
        val game: Game = Game()
    ) {
        data class Capture(
            var delay: Long = 1000L
        )

        data class Process(
            var name: String = "DeltaForceClient-Win64-Shipping.exe"
        )

        data class Image(
            val sharpening: Sharpening = Sharpening()
        ) {
            data class Sharpening(
                var enable: Boolean = true,
                var kernel: Kernel = Kernel(0, 0, floatArrayOf())
            )
        }

        data class Strength(
            var min: Int = 3,
            var max: Int = 100,
            var random: Int = 1,
            var multiplier: Double = 1.0
        )

        data class Server(
            var url: String = "http://127.0.0.1:8920",
            var clientId: String = "65489a26-09f8-4638-bafe-f4f8fadd371c",
            val api: Api = Api()
        ) {
            data class Api(
                var strength: String = "/api/v2/game/{client}/strength",
            )
        }

        data class Ocr(
            var url: String = "http://127.0.0.1:15666/ocr",
        )

        data class Color(
            val color: java.awt.Color,
            val tolerance: Int = 0
        )

        data class Zone(
            val right: Boolean = false,
            val top: Boolean = false,
            val lt: Pos = Pos(),
            val rb: Pos = Pos(),
        ) {
            data class Pos(
                val x: Int = 0,
                val y: Int = 0,
            )
        }

        data class Game(
            val health: Health = Health(),
            val start: Start = Start(),
            val end: End = End()
        ) {
            enum class GameCheckType {
                ONLY_GAME_TIME,
                ONLY_HEALTH,
                BOTH
            }

            data class Health(
                var type: Type = Type.BOTH,
                var tolerance: Int = 0
            ) {
                enum class Type {
                    ONLY_OCR,
                    ONLY_COLOR,
                    BOTH
                }
            }

            data class Start(
                var type: GameCheckType = GameCheckType.BOTH
            )

            data class End(
                var type: GameCheckType = GameCheckType.BOTH,
                var reset: Boolean = true,
                var vl: Int = 3
            )
        }
    }
}

class PreImage(
    var image: BufferedImage
) {
    var hasData: Boolean = false
}

fun initFiles() {
    val fileManager = FileManager()

    fileManager.saveResource("CaptureLibrary.dll", "CaptureLibrary.dll", false)
    fileManager.saveResource("config.yml", "config.yml", false)
}

fun getGameImage(): BufferedImage? {
    val window = JnaWindowUtil.getWindowByName(Main.configData.process.name) ?: return null
    return CaptureUtil.captureWindow(window)
}

fun preprocessImage(image: BufferedImage): BufferedImage {
    val preImg = PreImage(
        BufferedImage(image.width, image.height, image.type)
    )

    // 锐化
    if (Main.configData.image.sharpening.enable) {
        val convolveOp = ConvolveOp(Main.configData.image.sharpening.kernel, ConvolveOp.EDGE_NO_OP, null)
        convolveOp.filter(image, preImg.image)
        preImg.hasData = true
    }

    return if (preImg.hasData) preImg.image else image
}

fun getZones(image: BufferedImage) =
    Main.configData.zones["${image.width}x${image.height}"]

fun calculateColorPercentage(image: BufferedImage, color: Color, zone: Zone): Double {
    val subImage = image.subImage(zone)
    val total = subImage.width.toLong() * subImage.height.toLong()
    var has = 0

    val tolerance = color.tolerance
    val color = color.color
    (0..<subImage.width).forEach { x ->
        (0..<subImage.height).forEach { y ->
            val pixelColor = java.awt.Color(subImage.getRGB(x, y))

            if ((color.red - pixelColor.red).absoluteValue >= tolerance) return@forEach
            if ((color.green - pixelColor.green).absoluteValue >= tolerance) return@forEach
            if ((color.blue - pixelColor.blue).absoluteValue >= tolerance) return@forEach

            has++
        }
    }

    return if (total >= 0) has * 100.0 / total else 0.0
}

fun main() {
    initFiles()
    Main.reloadConfig()

    Capture.INSTANCE.InitializeCapture()
    Runtime.getRuntime().addShutdownHook(Thread {
        Capture.INSTANCE.DeinitializeCapture()
    })

    println(
        "============郊狼 X 三角洲============\n" +
                "初始化完成!\n" +
                "DXGI截图库: 存在\n" +
                "============郊狼 X 三角洲============"
    )

    val test = ImageIO.read(File("1e3a0b3d-14ab-42c2-8345-bcbf9a8029e2.png"))
    println(Main.ocrClient.ocrImage(test, null))

    var i = 0
    var endVl = 0
    var startGame = false
    while (true) {
        i++

        println("\n============第 $i 次循环============")

        run Main@{
            val image: BufferedImage?
            run {
                val start = System.currentTimeMillis()
                image = getGameImage()
                if (image == null) {
                    println(" - [错误] 找不到进程${Main.configData.process.name}!")
                    return@Main
                }
                println(" - [耗时] 截图完成, 耗时: ${System.currentTimeMillis() - start}ms")
            }
            image!!

            val preImage: BufferedImage
            run {
                val start = System.currentTimeMillis()
                preImage = preprocessImage(image)
                println(" - [耗时] 预处理完成, 耗时: ${System.currentTimeMillis() - start}ms")
            }

            val zones: Map<String?, Zone>?
            run {
                val start = System.currentTimeMillis()
                zones = getZones(preImage)
                if (zones == null) {
                    println(" - [错误] 不兼容的分辨率 ${preImage.width}x${preImage.height}!")
                    return@Main
                }
                println(" - [耗时] 区域选择完成, 耗时: ${System.currentTimeMillis() - start}ms")
            }
            zones!!

            val health: Int?
            val gameTimeText: String?
            run {
                val start = System.currentTimeMillis()

                val healthText = Main.ocrClient.ocrImage(preImage, zones["health_text"]!!)
                health = if (!healthText.isNullOrEmpty()) healthText.substringBefore("/").toInt() else null
                println(" - [OCR-提示] 识别血量文字: $healthText, 血量判定为: ")

                gameTimeText = Main.ocrClient.ocrImage(preImage, zones["game_time_text"]!!)
                println(" - [OCR-提示] 识别游戏时间文字: $gameTimeText")

                println(" - [耗时] OCR识别完成, 耗时: ${System.currentTimeMillis() - start}ms")
            }

            val healthPercentage: Int
            run {
                val start = System.currentTimeMillis()

                val zone = zones["health_bar"]!!
                var maxHealth = 0
                Main.configData.colors["health_bar"]!!.forEach { color ->
                    val percentage = calculateColorPercentage(preImage, color, zone).toInt()
                    if (percentage > maxHealth) maxHealth = percentage
                }
                healthPercentage = maxHealth

                println(" - [提示] 识别游戏血条颜色比例: $healthPercentage")
                println(" - [耗时] 血条颜色比例识别完成, 耗时: ${System.currentTimeMillis() - start}ms")
            }

            if (startGame) {
                if (endVl >= Main.configData.game.end.vl) {
                    startGame = false
                    endVl = 0
                    println(" - [状态] 判定游戏结束, vl: ${Main.configData.game.end.vl}")

                    if (!Main.configData.game.end.reset) Main.gameClient.setStrength(Main.configData.strength.min)
                    return@Main
                }

                if (health == null && gameTimeText == null) endVl++
                else endVl = 0

                val health = when (Main.configData.game.health.type) {
                    Game.Health.Type.ONLY_OCR -> health ?: return@Main
                    Game.Health.Type.ONLY_COLOR -> healthPercentage
                    Game.Health.Type.BOTH -> if (health == null || (health - healthPercentage).absoluteValue >= Main.configData.game.health.tolerance) healthPercentage else health
                }

                Main.gameClient.setStrength((health * Main.configData.strength.multiplier).toInt())
            } else {
                val result = when (Main.configData.game.start.type) {
                    Game.GameCheckType.ONLY_GAME_TIME -> !gameTimeText.isNullOrEmpty()
                    Game.GameCheckType.ONLY_HEALTH -> health != null
                    Game.GameCheckType.BOTH -> !gameTimeText.isNullOrEmpty() && health != null
                }

                if (!result) return@Main

                startGame = true
                println(" - [状态] 判定游戏开始, 判定模式: ${Main.configData.game.start.type}")
            }
        }

        Thread.sleep(Main.configData.capture.delay)
    }
}
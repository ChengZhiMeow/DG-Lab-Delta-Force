package cn.chengzhiya.dglabdeltaforce.client

import cn.chengzhiya.dglabdeltaforce.Main
import cn.chengzhiya.mhdfhttpframework.client.HttpClient

class GameClient {
    val httpClient = HttpClient()
    var lastStrength = 0

    /**
     * 修改郊狼强度
     */
    fun setStrength(value: Int) {
        val finalValue =
            if (Main.configData.strength.min > value) {
                println(" - [API-警告] 强度低于最小设定值 ${Main.configData.strength.min}, 已使用最小值!")
                Main.configData.strength.min
            } else if (Main.configData.strength.max < value) {
                println(" - [API-警告] 强度超过最大设定值 ${Main.configData.strength.max}, 已使用最大值!")
                Main.configData.strength.max
            } else value

        if (lastStrength != finalValue) println(" - [API-提示] 强度值修改为 $finalValue 成功！")
        else println(" - [API-提示] 强度值 $finalValue 无变化。!")

        try {
            httpClient.post(
                Main.configData.server.api.strength,
                "{\"strength\":{\"set\":$finalValue},\"randomStrength\":{\"set\":${Main.configData.strength.random}}"
            )
        } catch (_: Exception) {
            if (lastStrength != finalValue) println(" - [API-错误] 无法连接服务器！")
        }

        lastStrength = finalValue
    }
}
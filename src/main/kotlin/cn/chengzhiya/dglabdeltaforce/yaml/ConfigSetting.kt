package cn.chengzhiya.dglabdeltaforce.yaml

import cn.chengzhimeow.ccyaml.manager.AbstractYamlManager

class ConfigSetting private constructor() : AbstractYamlManager(
    ConfigManager.instance
) {
    override fun originFilePath() = "config.yml"
    override fun filePath() = "config.yml"

    companion object {
        val instance by lazy { ConfigSetting() }
    }
}
package cn.chengzhiya.dglabdeltaforce.yaml

import cn.chengzhimeow.ccyaml.CCYaml
import java.io.File

class ConfigManager private constructor() {
    companion object {
        val instance by lazy { CCYaml(this::class.java.classLoader, File("./"), "1.0.0") }
    }
}
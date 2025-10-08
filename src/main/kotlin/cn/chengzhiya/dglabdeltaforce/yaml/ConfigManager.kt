package cn.chengzhiya.dglabdeltaforce.yaml

import cn.chengzhimeow.ccyaml.CCYaml

class ConfigManager private constructor() {
    companion object {
        val instance by lazy { CCYaml("1.0.0") }
    }
}
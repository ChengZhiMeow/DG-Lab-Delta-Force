package cn.chengzhiya.dglabdeltaforce.yaml

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.nio.file.Files

@Suppress("unused")
class YamlConfiguration {
    private var data: MutableMap<String?, Any?>? = null

    companion object {
        fun loadConfiguration(file: File) = loadConfiguration(Files.newInputStream(file.toPath()))

        fun loadConfiguration(`in`: InputStream?): YamlConfiguration {
            `in`.use {
                val config = YamlConfiguration()
                val yaml = Yaml()
                config.data = yaml.load(`in`)
                if (config.data == null) {
                    config.data = mutableMapOf()
                }
                return config
            }
        }
    }

    fun set(path: String, value: Any?) {
        val keys = path
            .split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()

        keys.forEachIndexed { i, it ->
            if (i == keys.lastIndex) return@forEachIndexed
            this.data = this.data!![it] as MutableMap<String?, Any?>?
        }

        this.data!![keys.last()] = value
    }


    fun get(path: String): Any? {
        val keys: Array<String?> = path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var current = this.data
        keys.forEach {
            val value = current!![it]
            if (value !is MutableMap<*, *>) {
                return value
            }
            current = value as MutableMap<String?, Any?>?
        }

        return null
    }

    fun save(file: File) {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        val yaml = Yaml(options)

        val writer = BufferedWriter(FileWriter(file))
        yaml.dump(this.data, writer)
    }

    fun getConfigurationSection(path: String): YamlConfiguration {
        val keys = path
            .split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()

        var current = this.data
        for (key in keys) {
            current = current!![key] as MutableMap<String?, Any?>
        }

        val section = YamlConfiguration()
        section.data = current
        return section
    }

    fun getKeys() = this.data!!.keys

    fun getString(path: String) = this.get(path) as String?

    fun getInt(path: String) = this.get(path) as Int?

    fun getBoolean(path: String) = this.get(path) as Boolean? ?: false

    fun getDouble(path: String) = this.get(path) as Double?

    fun getLong(path: String) = this.get(path) as Long?

    fun getList(path: String) = this.get(path) as MutableList<*>? ?: emptyList()

    fun getStringList(path: String) = this.get(path) as MutableList<String>? ?: emptyList()

    fun getIntList(path: String) = this.get(path) as MutableList<Int>? ?: emptyList()

    fun getBooleanList(path: String) = this.get(path) as MutableList<Boolean>? ?: emptyList()

    fun getDoubleList(path: String) = this.get(path) as MutableList<Double>? ?: emptyList()

    fun getLongList(path: String) = this.get(path) as MutableList<Long>? ?: emptyList()

    fun isString(path: String) = this.get(path) is String

    fun isInt(path: String) = this.get(path) is Int

    fun isBoolean(path: String) = this.get(path) is Boolean

    fun isDouble(path: String) = this.get(path) is Double

    fun isLong(path: String) = this.get(path) is Long

    fun isList(path: String) = this.get(path) is MutableList<*>
}

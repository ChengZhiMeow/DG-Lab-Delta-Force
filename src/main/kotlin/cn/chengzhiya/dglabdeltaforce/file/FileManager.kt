package cn.chengzhiya.dglabdeltaforce.file

import java.io.File
import java.io.InputStream
import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ResourceException : Exception {
    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)

    constructor(
        message: String?,
        cause: Throwable?
    ) : super(message, cause)
}

class FileManager {
    fun formatPath(path: String) = path.replace("\\", "/")

    private fun copyFile(
        `in`: InputStream,
        filePath: Path,
        replace: Boolean
    ) {
        if (Files.exists(filePath) && !replace) return

        filePath.parent?.let { Files.createDirectories(it) }
        Files.copy(`in`, filePath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun saveFolderResource(
        resourceFolderPath: String,
        fileFolderPath: String,
        replace: Boolean
    ) {
        var resourceFolderPath = resourceFolderPath
        var fileFolderPath = fileFolderPath
        resourceFolderPath = this.formatPath(resourceFolderPath)
        resourceFolderPath = if (resourceFolderPath.endsWith("/")) resourceFolderPath else "$resourceFolderPath/"
        fileFolderPath = this.formatPath(fileFolderPath)
        fileFolderPath = if (fileFolderPath.endsWith("/")) fileFolderPath else "$fileFolderPath/"

        val loader = this.javaClass.getClassLoader()
        loader.getResource(resourceFolderPath) ?: throw ResourceException("找不到资源文件夹: $resourceFolderPath")

        val resources = loader.getResources(resourceFolderPath)
        while (resources.hasMoreElements()) {
            val resourceUrl = resources.nextElement()
            (resourceUrl.openConnection() as JarURLConnection).jarFile.use { jar ->
                val finalResourceFolderPath = resourceFolderPath
                val finalFileFolderPath = fileFolderPath
                jar.stream()
                    .filter {
                        this.formatPath(it!!.getName()).startsWith(finalResourceFolderPath) && !it.isDirectory
                    }
                    .forEach {
                        val target = File(
                            it.toString().replaceFirst(finalResourceFolderPath.toRegex(), finalFileFolderPath)
                        )
                        this.copyFile(jar.getInputStream(it), target.toPath(), replace)
                    }
            }
        }
    }

    fun saveResource(
        resourcePath: String,
        filePath: String,
        replace: Boolean
    ) {
        var resourcePath = resourcePath
        var filePath = filePath
        resourcePath = this.formatPath(resourcePath)
        filePath = this.formatPath(filePath)

        val target = File(filePath)

        val loader = this.javaClass.getClassLoader()
        loader.getResource(resourcePath) ?: throw ResourceException("找不到资源: $resourcePath")

        loader.getResourceAsStream(resourcePath)?.let {
            this.copyFile(it, target.toPath(), replace)
        }
    }

    fun listFiles(directory: File?): List<File?> {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return emptyList()
        }

        val fileArray = directory.listFiles() ?: return emptyList()

        val files = mutableListOf<File?>()
        for (file in fileArray) {
            if (file.isFile()) files.add(file)
            else if (file.isDirectory()) files.addAll(listFiles(file))
        }
        return files
    }
}

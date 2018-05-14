package xyz.teufelsgraben.recrypt

import java.io.IOException
import java.io.InputStreamReader
import java.lang.System.err
import java.lang.System.exit
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

fun main(args: Array<String>) {
    if (args.size != 3) {
        err.println("Usage: src-path target-path crypt-key")
        exit(1)
    }

    val srcPath = Paths.get(args[0])
    if (!srcPath.isAbsolute) {
        err.println("Source directory $srcPath isn't absolute")
        exit(1)
    }

    val targetPath = Paths.get(args[1])
    if (!targetPath.isAbsolute || Files.exists(targetPath)) {
        err.println("Target directory ${targetPath} isn't absolute or already exists")
        exit(1)
    }

    Files.createDirectories(targetPath)
    Files.walkFileTree(srcPath, ReCryptVisitor(targetPath, args[2]))
}

internal class ReCryptVisitor(private val target: Path, private val cryptKey: String) : FileVisitor<Path> {

    private val dirStack: MutableList<String> = mutableListOf()

    override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        println("push Dir $dir")
        if (dir!! != target) { // ignore initial directory
            dirStack.add(dir.fileName.toString())
            Files.createDirectories(dirStack.fold(target, { acc, i -> acc.resolve(i) }))
        }
        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
        println("pop Dir $dir")
        if (!dir!!.equals(target)) { // ignore initial directory
            (dirStack.size - 1).also { last ->
                assert(dirStack[last] == dir.fileName.toString())
                dirStack.removeAt(last)
            }
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult =
            throw IllegalStateException("VisitFileFailed $file", exc)

    override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
        if (file!!.fileName.toString().endsWith(".gpg")) {
            reCrypt(file)
        } else {
            copy(file)
        }
        return FileVisitResult.CONTINUE
    }

    private fun reCrypt(file: Path) {
        val destination = dirStack.fold(target, { acc, i -> acc.resolve(i) }).resolve(file.fileName)
        val dest = destination.toString()
        Paths.get(destination.toString() + ".tmp").also { temp ->
            exec(listOf("gpg", "--output", temp.toString(), "--decrypt", file.toString()))
            val md = sum(temp)
            exec(listOf("gpg", "--output", dest, "--encrypt", "--recipient", cryptKey, temp.toString()))
            Files.delete(temp)
            exec(listOf("gpg", "--output", temp.toString(), "--decrypt", dest))
            val md2 = sum(temp)
            Files.delete(temp)
            if (md != md2) {
                throw IllegalStateException("Message digest mismatch $md != $md2 of: $file")
            } else {
                println("Sum OK")
            }
        }
    }


    private fun copy(file: Path) {
        val target = dirStack.fold(target, { acc, i -> acc.resolve(i) }).resolve(file.fileName)
        println("Copy $file -> $target")
        Files.copy(file, target)
    }
}

fun sum(file: Path): String =
        Files.readAllBytes(file).let { bytes ->
            MessageDigest.getInstance("SHA-512").let { md ->
                md.digest(bytes).fold("", { acc, i -> acc + Integer.toHexString(i.toInt()) })
            }
        }

fun exec(args: List<String>) {
    println("Cmd $args")

    val builder = ProcessBuilder(args)
    builder.redirectErrorStream(true)

    val proc = builder.start()
    proc.outputStream.close()
    val rc = proc.waitFor()

    proc.inputStream.use {
        println("Out ${InputStreamReader(it, StandardCharsets.UTF_8).readText()}")
    }

    if (rc != 0) {
        throw IllegalStateException("exec failed")
    }
}
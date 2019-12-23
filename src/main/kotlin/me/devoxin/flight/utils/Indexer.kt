package me.devoxin.flight.utils

import me.devoxin.flight.annotations.Command
import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.api.Context
import me.devoxin.flight.arguments.Argument
import me.devoxin.flight.arguments.Greedy
import me.devoxin.flight.arguments.Name
import me.devoxin.flight.internal.Jar
import me.devoxin.flight.models.Cog
import org.reflections.Reflections
import org.reflections.scanners.MethodParameterNamesScanner
import org.reflections.scanners.SubTypesScanner
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure

class Indexer {

    private val jar: Jar?
    private val packageName: String
    private val reflections: Reflections
    private val classLoader: URLClassLoader?

    constructor(packageName: String) {
        this.packageName = packageName
        this.classLoader = null
        this.jar = null
        reflections = Reflections(packageName, MethodParameterNamesScanner(), SubTypesScanner())
    }

    constructor(packageName: String, jarPath: String) {
        this.packageName = packageName

        val commandJar = File(jarPath)
        check(commandJar.exists()) { "jarPath points to a non-existent file." }
        check(commandJar.extension == "jar") { "jarPath leads to a file which is not a jar." }

        val path = URL("jar:file:${commandJar.absolutePath}!/")
        this.classLoader = URLClassLoader.newInstance(arrayOf(path))
        this.jar = Jar(commandJar.nameWithoutExtension, commandJar.absolutePath, packageName, classLoader)
        reflections = Reflections(packageName, this.classLoader, MethodParameterNamesScanner(), SubTypesScanner())
    }

    fun getCogs(): List<Cog> {
        val cogs = reflections.getSubTypesOf(Cog::class.java)
        logger.debug("Discovered ${cogs.size} cogs in $packageName")

        return cogs
            .filter { !Modifier.isAbstract(it.modifiers) && !it.isInterface && Cog::class.java.isAssignableFrom(it) }
            .map { it.getDeclaredConstructor().newInstance() }
    }

    @ExperimentalStdlibApi
    fun getCommands(cog: Cog): List<KFunction<*>> {
        val cogClass = cog::class
        logger.debug("Scanning ${cog.name()} for commands...")
        val commands = cogClass.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.hasAnnotation<Command>() }

        logger.debug("Found ${commands.size} commands in cog ${cog.name()}")
        return commands.toList()
    }

    @ExperimentalStdlibApi
    fun loadCommand(meth: KFunction<*>, cog: Cog): CommandWrapper {
        require(meth.javaMethod!!.declaringClass == cog::class.java) { "${meth.name} is not from ${cog.name()}" }
        require(meth.hasAnnotation<Command>()) { "${meth.name} is not annotated with Command!" }

        val category = cog.name()
        val name = meth.name
        val properties = meth.findAnnotation<Command>()!!
        val async = meth.isSuspend
        val ctxParam = meth.valueParameters.firstOrNull { it.type.classifier?.equals(Context::class) == true }

        require(ctxParam != null) { "${meth.name} is missing the Context parameter!" }

        val parameters = meth.valueParameters
            .filterNot { it.type.classifier?.equals(Context::class) == true }

        val arguments = mutableListOf<Argument>()

        for (p in parameters) {
            val pName = p.findAnnotation<Name>()?.name ?: p.name ?: p.index.toString()
            val type = p.type.jvmErasure.javaObjectType
            val greedy = p.hasAnnotation<Greedy>()
            val optional = p.isOptional
            val isNullable = p.type.isMarkedNullable

            arguments.add(Argument(pName, type, greedy, optional, isNullable, p))
        }

        return CommandWrapper(name, arguments, category, properties, async, meth, cog, jar, ctxParam)
    }

//    fun getParamNames(meth: Method): List<String> {
//        return reflections.getMethodParamNames(meth)
//    }

    companion object {
        private val logger = LoggerFactory.getLogger(Indexer::class.java)
    }

}

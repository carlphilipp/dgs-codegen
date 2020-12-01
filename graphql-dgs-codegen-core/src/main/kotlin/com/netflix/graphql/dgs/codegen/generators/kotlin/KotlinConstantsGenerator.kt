package com.netflix.graphql.dgs.codegen.generators.kotlin

import com.netflix.graphql.dgs.codegen.CodeGenConfig
import com.netflix.graphql.dgs.codegen.KotlinCodeGenResult
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.*

class KotlinConstantsGenerator(private val config: CodeGenConfig, private val document: Document) {
    fun generate(): KotlinCodeGenResult {
        val baseConstantsType = TypeSpec.objectBuilder("DgsConstants")

        document.definitions.filterIsInstance<ObjectTypeDefinition>().filter { it !is ObjectTypeExtensionDefinition }.map {
            val constantsType = TypeSpec.objectBuilder((it.name.toUpperCase()))

            val extensions = findExtensions(it.name, document.definitions)
            val fields = it.fieldDefinitions.plus(extensions.flatMap { it.fieldDefinitions }).distinctBy { it.name }

            constantsType.addProperty(PropertySpec.builder("TYPE_NAME", String::class).addModifiers(KModifier.CONST).initializer(""""${it.name}"""").build())

            fields.forEach { field ->
                addFieldName(constantsType, field.name)
            }

            baseConstantsType.addType(constantsType.build())
        }

        document.definitions.filterIsInstance<InputObjectTypeDefinition>().filter { it !is InputObjectTypeExtensionDefinition }.map {
            val constantsType = TypeSpec.objectBuilder((it.name.toUpperCase()))

            val extensions = findInputExtensions(it.name, document.definitions)
            val fields = it.inputValueDefinitions.plus(extensions.flatMap { it.inputValueDefinitions })
            constantsType.addProperty(PropertySpec.builder("TYPE_NAME", String::class).addModifiers(KModifier.CONST).initializer(""""${it.name}"""").build())
            fields.forEach { field ->
                addFieldName(constantsType, field.name)
            }

            baseConstantsType.addType(constantsType.build())
        }

        document.definitions.filterIsInstance<InterfaceTypeDefinition>().map {
            val constantsType = TypeSpec.objectBuilder((it.name.toUpperCase()))
            constantsType.addProperty(PropertySpec.builder("TYPE_NAME", String::class).addModifiers(KModifier.CONST).initializer(""""${it.name}"""").build())

            it.fieldDefinitions.forEach { field ->
                addFieldName(constantsType, field.name)
            }

            baseConstantsType.addType(constantsType.build())
        }

        document.definitions.filterIsInstance<UnionTypeDefinition>().map {
            val constantsType = TypeSpec.objectBuilder((it.name.toUpperCase()))
            constantsType.addProperty(PropertySpec.builder("TYPE_NAME", String::class).addModifiers(KModifier.CONST).initializer(""""${it.name}"""").build())
            baseConstantsType.addType(constantsType.build())
        }

        if(document.definitions.firstOrNull { it is ObjectTypeDefinition && it.name == "Query" } != null) {
            baseConstantsType.addProperty(PropertySpec.builder("QUERY_TYPE", String::class).addModifiers(KModifier.CONST).initializer(""""Query"""").build())
        }

        if(document.definitions.firstOrNull { it is ObjectTypeDefinition && it.name == "Mutation" } != null) {
            baseConstantsType.addProperty(PropertySpec.builder("Mutation_TYPE", String::class).addModifiers(KModifier.CONST).initializer(""""Mutation"""").build())
        }
        if(document.definitions.firstOrNull { it is ObjectTypeDefinition && it.name == "Subscription" } != null) {
            baseConstantsType.addProperty(PropertySpec.builder("Subscription_TYPE", String::class).addModifiers(KModifier.CONST).initializer(""""Subscription"""").build())
        }

        val fileSpec = FileSpec.builder(config.packageName, "DgsConstants").addType(baseConstantsType.build()).build()
        return KotlinCodeGenResult(constants = listOf(fileSpec))
    }

    private fun addFieldName(constantsType: TypeSpec.Builder, name: String) {
        constantsType.addProperty(PropertySpec.builder(name.capitalize(), String::class).addModifiers(KModifier.CONST).initializer(""""$name"""").build())
    }

    private fun findExtensions(name: String, definitions: List<Definition<Definition<*>>>) =
            definitions.filterIsInstance<ObjectTypeExtensionDefinition>().filter { name == it.name }

    private fun findInputExtensions(name: String, definitions: List<Definition<Definition<*>>>) =
            definitions.filterIsInstance<InputObjectTypeExtensionDefinition>().filter { name == it.name }

}
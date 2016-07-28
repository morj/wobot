buildscript {

    // need to redeclare `extra` because buildscript is compiled without the imports
    val extra = project.extensions.extraProperties
    extra["kotlinVersion"] = "1.1.0-dev-998"
    extra["kumoVersion"] = "1.8"
    extra["repo"] = "https://repo.gradle.org/gradle/repo"

    repositories {
        jcenter()
        maven { setUrl(extra["repo"]) }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply {
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
    plugin<ApplicationPlugin>()
}

configure<ApplicationPluginConvention> {
    mainClassName = "com.github.morj.wobot.MainKt"
}

repositories {
    maven { setUrl(extra["repo"]) }
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib:${extra["kotlinVersion"]}")
    compile("com.kennycason:kumo:${extra["kumoVersion"]}")
    // compile("com.ullink.slack:simpleslackapi:0.5.1")
    compile("com.github.morj:simple-slack-api:c97584f453")
    compile("org.apache.httpcomponents:httpmime:4.4")
    compile("org.languagetool:language-ru:2.5")
}

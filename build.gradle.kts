plugins {
    id("faststats.java-conventions") apply false
}

subprojects {
    pluginManager.apply("faststats.java-conventions")
}

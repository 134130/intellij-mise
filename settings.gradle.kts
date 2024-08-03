include(
    "modules/core",
    "modules/products/idea",
    "modules/products/gradle",
    "modules/products/goland",
    "modules/products/nodejs",
    "modules/products/pythonid",
    "modules/products/pythoncore",
)

rootProject.name = "mise"

rootProject.children.forEach {
    it.name = (it.name.replaceFirst("modules/", "mise/").replace("/", "-"))
}

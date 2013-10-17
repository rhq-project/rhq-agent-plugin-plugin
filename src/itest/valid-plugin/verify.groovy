import java.util.jar.JarFile

JarFile pluginJarFile = new JarFile(new File(basedir, "target/itest-valid-plugin-1.0-SNAPSHOT.jar"))
assert pluginJarFile.getJarEntry("lib/commons-codec-1.3.jar") != null: "commons-codec library not found"
assert pluginJarFile.getJarEntry("lib/commons-httpclient-3.1.jar") != null: "commons-httpclient not found"

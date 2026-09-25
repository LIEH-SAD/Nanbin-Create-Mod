import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Strips dead duplicate members from a mod jar so that the jar can be remapped by Loom.
 *
 * MTR 1.20.x ships {@code mtr/screen/WidgetShorterSlider.class} with two public methods that mean
 * the same thing but carry both the "named" and the production mapping name, so they collapse onto
 * the same name once remapped:
 *
 * <ul>
 *   <li>Fabric jar (intermediary): {@code method_48579(DrawContext,int,int,float)} and
 *       {@code renderButton(DrawContext,int,int,float)}.</li>
 *   <li>Forge jar (srg): {@code m_87963_(GuiGraphics,int,int,float)} and
 *       {@code renderButton(GuiGraphics,int,int,float)} → conflict against
 *       {@code AbstractWidget.m_87963_} during the srg → official pass.</li>
 * </ul>
 *
 * Loom/tiny-remapper then reports "Mapping target name conflicts detected" and aborts with
 * "There were unfixable conflicts." because the remapped class would hold two identical methods.
 *
 * The {@code renderButton} copies are dead code (nothing in the jar invokes them; Minecraft calls the
 * intermediary/srg override instead), so removing them is behaviour preserving.
 *
 * Both jars consumed by the build are produced from the official YMTR release
 * (Modrinth project {@code U75MCH6y}, versions {@code tW2FT3tx} = Fabric and {@code MhSlBOh2} = Forge,
 * i.e. {@code MTR-fabric-1.20.1-3.6.3.jar} / {@code MTR-forge-1.20.1-3.6.3.jar}); the patched results
 * live in {@code libs/}. Each jar is ~63 MB, so only regenerate them when the YMTR version used by
 * the build scripts changes.
 *
 * <pre>
 * javac -cp asm.jar -d out MtrJarPatcher.java
 * java -cp "asm.jar;out" MtrJarPatcher MTR-fabric-1.20.1-3.6.3.jar libs/MTR-fabric-1.20.1-3.6.3-patched.jar
 * java -cp "asm.jar;out" MtrJarPatcher MTR-forge-1.20.1-3.6.3.jar  libs/MTR-forge-1.20.1-3.6.3-patched.jar
 * </pre>
 *
 * (<i>asm.jar</i> is available from any Gradle cache, e.g.
 * {@code ~/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.9.1/&lt;hash&gt;/asm-9.9.1.jar}.)
 *
 * Usage: java -cp asm.jar:. MtrJarPatcher &lt;input.jar&gt; &lt;output.jar&gt;
 */
public final class MtrJarPatcher {

	private static final Map<String, String[][]> REMOVALS = new LinkedHashMap<>();

	static {
		REMOVALS.put("mtr/screen/WidgetShorterSlider.class", new String[][]{
			// name, descriptor
			// Fabric distribution (intermediary names)
			{"renderButton", "(Lnet/minecraft/class_332;IIF)V"},
			{"renderButton", "(Lnet/minecraft/class_1071;IIF)V"},
			// Forge distribution (Mojang names)
			{"renderButton", "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}
		});
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: MtrJarPatcher <input.jar> <output.jar>");
			System.exit(2);
		}

		final Path input = Paths.get(args[0]);
		final Path output = Paths.get(args[1]);
		int patchedClasses = 0;
		int removedMethods = 0;

		try (ZipFile zip = new ZipFile(input.toFile());
			 OutputStream fileOut = Files.newOutputStream(output);
			 ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(fileOut))) {
			final Enumeration<? extends ZipEntry> entries = zip.entries();

			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				byte[] data;
				try (InputStream in = zip.getInputStream(entry)) {
					data = in.readAllBytes();
				}

				final String[][] removals = REMOVALS.get(entry.getName());
				if (removals != null) {
					final ClassReader reader = new ClassReader(data);
					final ClassWriter writer = new ClassWriter(0);
					final int[] counter = {0};
					reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
						@Override
						public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
							for (String[] removal : removals) {
								if (removal[0].equals(name) && removal[1].equals(descriptor)) {
									counter[0]++;
									System.out.println("  removed " + entry.getName() + " " + name + descriptor);
									return null;
								}
							}
							return super.visitMethod(access, name, descriptor, signature, exceptions);
						}
					}, 0);
					data = writer.toByteArray();
					patchedClasses++;
					removedMethods += counter[0];
				}

				final ZipEntry newEntry = new ZipEntry(entry.getName());
				newEntry.setTime(entry.getTime());
				out.putNextEntry(newEntry);
				out.write(data);
				out.closeEntry();
			}
		}

		System.out.println("Patched " + patchedClasses + " class(es), removed " + removedMethods + " method(s).");
		System.out.println("Wrote " + output.toAbsolutePath());
	}
}

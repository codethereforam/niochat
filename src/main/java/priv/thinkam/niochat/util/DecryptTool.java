package priv.thinkam.niochat.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @author thinkam
 * @date 2018/11/11
 */
public class DecryptTool {
	public static void main(String[] args) {
		if (args.length <= 0) {
			System.out.println("please enter the path of file in args[0]");
			return;
		}
		Path path = Paths.get(args[0].trim());
		try (Stream<String> lines = Files.lines(path)) {
			lines.forEach(line -> {
				String decryptedLine = line;
				if (StringUtils.isNotBlank(line)) {
					try {
						decryptedLine = AESUtils.decrypt(line, "123456789123456789");
					} catch (Exception e) {
						// line is not encrypted
					}
				}
				System.out.println(decryptedLine);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

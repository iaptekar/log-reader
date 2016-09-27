package igor.logreader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

public class ApplicationTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		String word = "test";
		Pattern regex = Pattern.compile("(" + word + ")");
		String line = "this is a test testing one two three";
		Matcher matcher = regex.matcher(line);
		int start = 0;
		while (matcher.find()) {
			System.out.println(matcher.start(1));
			System.out.println(matcher.group(1));
			System.out.println(matcher.groupCount());
			System.out.println(matcher.end(1));
		}
	}

}

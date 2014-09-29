
/**
 * Log Utils
 * @author caisenchuan
 */
public class KLog {
	private static final boolean DEBUG = true;
	
	public static void d(String tag, String format, Object... args) {
		if (DEBUG) {
			String str = String.format(format, args);
			String ret = String.format("%s(D) : %s", tag, str);
			System.out.println(ret);
		}
	}
	
	public static void e(String tag, String format, Object... args) {
		String str = String.format(format, args);
		String ret = String.format("%s(E) : %s", tag, str);
		System.out.println(ret);
	}
	
	public static void e(String tag, Throwable tr, String format, Object... args) {
		e(tag, format, args);
		tr.printStackTrace();
	}
}
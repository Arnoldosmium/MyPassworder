/** Utils
  * @author Arnold Lin 
  * @date first commit 2016/12/31, update at 2017/1/3
  */
import java.util.*;
import java.util.regex.*;

public class Utils {
	private static <T extends Comparable<T>> T findExtreme(Iterable<T> iter, int sign){
		T rtn = null;
		for(T elem: iter)
			rtn = (rtn == null || (elem.compareTo(rtn) * sign > 0)) ? elem : rtn;
		return rtn;
	}
	
	public static <T extends Comparable<T>> T max(Iterable<T> iter){
		return findExtreme(iter, 1);
	}
	
	public static <T extends Comparable<T>> T min(Iterable<T> iter){
		return findExtreme(iter, -1);
	}
	
	public static <T extends Number> Double sum(T... ts){
		double sum = 0;
		for (T t : ts) {
			sum += t.doubleValue();
		}
		return sum;
	}
	
	private static String basicCleanse(String original){
		return original.replaceAll("[\"\':;]", "");
	}
	
	public static String titleSafeCleanse(String original){
		return basicCleanse(original.replaceAll("\n", ""));
	}
	
	public static String contentSafeCleanse(String original){
		return basicCleanse(original);
	}
	
	private static Pattern newLine = Pattern.compile("\n");
	public static String blockContentCleanseAndPack(String content){
		String[] everything = newLine.split(content);
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < everything.length; i++){
			String cleaned = contentSafeCleanse(everything[i]);
			if(cleaned.length() == 0)	continue;
			sb.append(cleaned);
			sb.append(':');
		}
		return sb.deleteCharAt(sb.length()-1).toString();
	}
	
	public static String unpackBlockContent(String packed){
		return packed == null ? "" : packed.replaceAll(":", "\n");
	}
}

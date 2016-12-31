import java.util.*;
import java.util.List;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.*;

public class GBCParser {
	static Pattern comma = Pattern.compile(",");
	static Pattern at = Pattern.compile("@"); 
	static Pattern tuple = Pattern.compile("\\((.+?)\\)");
	
	public static List<GridBagConstraints> parseFile(String file){
		Scanner in;
		try{
			in = new Scanner(new File(file));
		}catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
		in.useDelimiter("@");
		List<GridBagConstraints> rtn = new ArrayList<>();
		while(in.hasNext())
			rtn.add(parseString(in.next()));
		in.close();
		return rtn;
		
	}
	
	public static GridBagConstraints parseString(String s){
		Scanner in = new Scanner(s);
		Matcher xy = null;
		if(!in.hasNextLine() || !(xy = tuple.matcher(in.nextLine())).matches()){
			System.err.println("Invalid GBC");
			return null;
		}
		String[] xytuple = comma.split(xy.group(1));
		GridBagConstraints rtn = new GridBagConstraints();
		rtn.gridx = Integer.parseInt(xytuple[0]);
		rtn.gridy = Integer.parseInt(xytuple[1]);
		
		Pattern field = Pattern.compile("^([a-z]+) ?= ?(.+?);?$");
		while(in.hasNextLine()){
			String line = in.nextLine();
			Matcher lm;
			if(!(lm = field.matcher(line)).matches()){
				System.err.println("Invalid field line: " + line);
				continue;
			}
			String fname = lm.group(1);
			if(fname.equals("insets")){
				if(!(xy = tuple.matcher(lm.group(2))).matches() || (xytuple = comma.split(xy.group(1))).length != 4){
					System.err.println("Invalid insets");
				}
				int[] insetsArg = new int[4];
				for(int i = 0; i < 4; insetsArg[i] = Integer.parseInt(xytuple[i++]));
				rtn.insets = new Insets(insetsArg[0], insetsArg[1], insetsArg[2], insetsArg[3]);
				continue;
			}
			Object val;
			if(Pattern.matches("[0-9]{1,8}", lm.group(2))){	// integer
				val = Integer.parseInt(lm.group(2));
			}else if(Pattern.matches("[0-9]+\\.[0-9]*", lm.group(2))){ // Double
				val = Double.parseDouble(lm.group(2));
			}else{
				try {
					val = GridBagConstraints.class.getField(lm.group(2).toUpperCase()).get(null);	//static var
				} catch (NoSuchFieldException e) {
					System.err.printf("Invalid constant \"%s\"\n", lm.group(2));
					continue;
				} catch (Exception e) {
					System.err.printf("Error fetching constant \"%s\" - %s\n", lm.group(2), e.getMessage());
					continue;
				}
			}
			
			try {
				GridBagConstraints.class.getField(fname).set(rtn, val);
			} catch (Exception e) {
				System.err.println("Failed to assign " + line);
				System.err.println(e.getMessage());
			}
		}
		in.close();
		return rtn;
	}
	
	public static void main(String[] args) {
		List<GridBagConstraints> l = parseFile("FC_layout.txt");
		for(GridBagConstraints gbc:l){
			if(gbc == null){
				System.out.println("null\n");
				continue;
			}
			for(Field f: GridBagConstraints.class.getFields()){
				if(Pattern.matches("[A-Z_]+",f.getName()))
					continue;
				try {
					System.out.println(f.getName() + ": ("+f.get(gbc).getClass()+") "+f.get(gbc));
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
			System.out.println();
		}
			
	}
}


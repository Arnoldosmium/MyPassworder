/* Arnold Lin 10/27/2016
 * Multi-language Toolbox Java section
 * Comparable Tuple
 * 
 */

import java.util.*;

public class Tuple<T extends Comparable<T>> implements Comparable<Tuple<T>>, Iterable<T>{

	T[] elements;
	
	public Tuple(T... args){
		elements = Arrays.copyOf(args, args.length);
	}
	
	public Tuple(Tuple<T> t){
		elements = Arrays.copyOf(t.elements, t.length());
	}
	
	public boolean isEmpty(){
		return elements.length <= 0;
	}
	
	public int length(){
		return elements.length;
	}
	
	public T get(int index){
		return elements[index];
	}
	
	public T getAnyway(int index) {
		if(isEmpty()) throw	new NoSuchElementException();
		index = index % length();
		if(index < 0) index += length();
		return elements[index];
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <U extends Comparable<U>> Tuple concat(Iterable<U>... its){
		List<Comparable> arr = new ArrayList<>(elements.length);
		for(T c : elements)
			arr.add(c);
		for(Iterable<U> it : its)
			for(U c: it)
				arr.add(c);
		return new Tuple(arr.toArray(new Comparable[0]));
	}
	
	@Override
	public String toString(){
		if(isEmpty())
			return "()";
		StringBuffer sb = new StringBuffer("(");
		for(T c: elements){
			sb.append(c.toString());
			sb.append(", ");
		}
		sb.delete(sb.length()-2, sb.length());
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public int compareTo(Tuple<T> o) {
		for(int i = 0; i < length() && i < o.length(); i++){
			int result = get(i).compareTo(o.get(i));
			if(result != 0) return result;
		}
		return length() - o.length();
	}
	
	public boolean equals(Tuple<T> o) {
		return compareTo(o) == 0;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int pt = 0;
			
			@Override
			public boolean hasNext() {
				return pt < elements.length;
			}

			@Override
			public T next() {
				return elements[pt++];
			}
			
		};
	}
}

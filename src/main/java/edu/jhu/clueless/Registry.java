package edu.jhu.clueless;

public interface Registry<T> {

	public T get(String id);
	
	public void add(T obj);

	public T remove(String id);
	
	public int getCount();

}

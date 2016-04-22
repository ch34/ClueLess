package edu.jhu.clueless;

public interface Registry<T> {

	public T get(String id);
	
	public T create(T obj);
	
	public T remove(T obj);
	
	public int getCount();

}

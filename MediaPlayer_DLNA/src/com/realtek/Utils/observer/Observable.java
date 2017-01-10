
package com.realtek.Utils.observer;

public interface Observable {
	public void addObserver(Observer obs);
	public void deleteObserver(Observer obs);
}

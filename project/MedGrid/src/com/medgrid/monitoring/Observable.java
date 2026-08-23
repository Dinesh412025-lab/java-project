package com.medgrid.monitoring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Observable {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyObservers(String eventType, String message) {
        for (Observer observer : observers) {
            observer.logEvent(eventType, message);
        }
    }
}

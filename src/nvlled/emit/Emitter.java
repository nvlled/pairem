package nvlled.emit;

import java.util.concurrent.*;
import java.util.*;

public class Emitter<T> {
    private List<T> frontEvents;
    private List<T> backEvents;

    private Set<Receiver<T>> anyListeners;

    public Emitter() {
        frontEvents = new LinkedList<T>();
        backEvents = new LinkedList<T>();
        anyListeners = Collections.synchronizedSet(new HashSet<Receiver<T>>());
    }

    public void emit(T e) {
        backEvents.add(e);
    }

    public void listen(Receiver<T> l) {
        anyListeners.add(l);
    }

    public void unlisten(Receiver<T> l) {
        anyListeners.remove(l);
    }

    public T waitEvent(final Predicate<T> pred) throws InterruptedException {
        final Semaphore lock = new Semaphore(0);
        final Option<T> event = new Option<T>();
        final Option<Receiver<T>> opt = new Option<Receiver<T>>();

        final Receiver<T> l = new Receiver<T>() {
            public void receive(T e) {
                if (pred.test(e)) {
                    event.set(e);
                    unlisten(opt.get());
                    lock.release();
                }
            }
        };
        opt.set(l);

        listen(l);
        lock.acquire();
        return event.get();
    }

    public T waitEvent() throws InterruptedException {
        return waitEvent(new Predicate<T>() {
            public boolean test(T e) {
                return true;
            }
        });
    }

    public void dispatchEvents() {
        List<T> events = backEvents;
        backEvents = frontEvents;
        frontEvents = events;
        backEvents.clear();

        for (T e: frontEvents) {
            System.out.println(e);

            for (Receiver<T> l: anyListeners)
                l.receive(e);
        }
    }
}

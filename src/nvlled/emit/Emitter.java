package nvlled.emit;

import java.util.*;
import java.util.concurrent.*;

public class Emitter<T> {
    private List<T> backEvents;
    private List<T> frontEvents;

    private Set<Receiver<T>> anyListeners;

    public Emitter() {
        frontEvents = new LinkedList<T>();
        backEvents = new LinkedList<T>();
        // TODO: use a set from concurrent one instread
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
        return waitEvent(
                new Predicate<T>() {
                    public boolean test(T _) { return true; }
                });
    }

    public Iterator<T> iterator(Predicate<T> pred) {
        return new Iterator<T>(this, pred);
    }

    public Iterator<T> iterator() {
        return new Iterator<T>(this, new Predicate<T>() {
            public boolean test(T _) { return true; }
        });
    }

    public void dispatchEvents() {
        List<T> events = backEvents;
        backEvents = frontEvents;
        frontEvents = events;
        backEvents.clear();

        for (T e: frontEvents) {
            for (Receiver<T> l: anyListeners)
                l.receive(e);
        }
    }

    public static class Iterator<T> implements Receiver<T>, java.util.Iterator<T> {
        private Predicate<T> pred;
        private Emitter<T> emitter;
        private BlockingQueue<T> items;

        private boolean closed = false;

        private Iterator(Emitter<T> emitter, Predicate<T> pred) {
            this.emitter = emitter;
            this.pred = pred;
            items = new LinkedBlockingQueue<T>();
            emitter.listen(this);
        }

        @Override
        public void receive(T e) {
            items.add(e);
        }

        @Override
        public boolean hasNext() { return !closed; }

        @Override
        public T next() {
            try {
                return items.take();
            } catch (InterruptedException e) {
            }
            close();
            return null;
        }

        @Override
        public void remove() { /* noped */ }

        public void close() {
            emitter.unlisten(this);
            closed = true;
        }
    }

}

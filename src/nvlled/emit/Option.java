package nvlled.emit;

class Option<T> {
    T item;

    public Option() {  }
    public Option(T t) { set(t); }

    public void set(T t) { item = t; }

    public T get() { return item; }
}

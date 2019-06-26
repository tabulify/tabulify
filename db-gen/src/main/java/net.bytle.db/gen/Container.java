package net.bytle.db.gen;

public class Container<T> {

    private final T value;

    public Container(T value) {

        this.value = value;

    }


    public T getValue() {
        return value;
    }

}

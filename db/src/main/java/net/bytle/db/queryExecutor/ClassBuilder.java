package net.bytle.db.queryExecutor;

public class ClassBuilder<T> {
    @SuppressWarnings("unchecked")
    public T build(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return (T) Class.forName(className).newInstance();
    }
}

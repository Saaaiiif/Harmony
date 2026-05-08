package interfaces;

import java.util.List;

public interface RessourceServices<T> {
    void add(T t);
    void update(T t);
    void delete(int id);
    List<T> getAll();
}

package interfaces;
import java.util.List;
public interface services<T> {

    void ajouter (T t);
    List<T> afficherTout();
    void modifier (T t);
    void supprimer (T t);
}

package interfaces;
import java.util.List;

public interface ActiviteServices<T> {

        void ajouter (T t);
        List<T> afficherTout();
        void modifier (T t);
        void supprimer (T t);

}
